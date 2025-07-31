package com.mafia.game.webSocket.server;

import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.service.RoomHintService;
import com.mafia.game.game.model.vo.Hint;
import com.mafia.game.game.model.vo.RoomHint;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class AbilityManager {

	private final GameRoomManager roomManager;
	private final GameEventManager eventManager;
	private final GameRoomService gameRoomService;
	private final RoomHintService roomHintService;
	private final MemberService memberService;
	private final ObjectMapper mapper = new ObjectMapper();

	// 밤 동안의 능력 사용 결과를 저장하는 임시 저장소
	private final Map<Integer, NightlyAction> nightlyActions = new ConcurrentHashMap<>();

	@Autowired
	public AbilityManager(@Lazy GameRoomManager roomManager, GameEventManager eventManager,
			GameRoomService gameRoomService, RoomHintService roomHintService, MemberService memberService) {
		this.roomManager = roomManager;
		this.eventManager = eventManager;
		this.gameRoomService = gameRoomService;
		this.roomHintService = roomHintService;
		this.memberService = memberService;
	}

	/**
	 * 새로운 게임이 시작될 때 호출되어 이전 기록을 초기화합니다.
	 * 
	 * @param roomNo 방 번호
	 */
	public void onGameStart(int roomNo) {
		nightlyActions.put(roomNo, new NightlyAction());
	}

	/**
	 * 클라이언트로부터 직업 능력 사용 요청을 처리합니다.
	 * 
	 * @param roomNo         방 번호
	 * @param userName       능력 사용자
	 * @param targetUserName 능력 대상
	 */
	public void useAbility(int roomNo, String userName, String targetUserName) {
		Job userJob = roomManager.getJobForUser(roomNo, userName);
		if (userJob == null)
			return;

		NightlyAction actions = nightlyActions.computeIfAbsent(roomNo, k -> new NightlyAction());
		String jobName = userJob.getJobName();

		switch (jobName) {
		case "mafia":
			actions.addMafiaKill(userName, targetUserName);
			break;
		case "doctor":
			actions.setDoctorHeal(userName, targetUserName);
			break;
		case "police":
			actions.setPoliceCheck(userName, targetUserName);
			Job targetJob = roomManager.getJobForUser(roomNo, targetUserName);
			boolean isMafia = (targetJob != null && targetJob.getJobClass() == 1);
			notifyAbilityResult(roomNo, userName,
					String.format("[%s]님은 %s.", targetUserName, isMafia ? "마피아입니다" : "마피아가 아닙니다"));
			break;
		case "robber":
			Job deadPersonJob = roomManager.getJobForUser(roomNo, targetUserName);
			if (deadPersonJob != null
					&& (deadPersonJob.getJobNo() == 0 || deadPersonJob.getJobName().contains("Ghost"))) {
				actions.setRobberTarget(userName, targetUserName);
			}
			break;
		case "necromancer":
			if (userJob.getJobNo() == 10) {
				actions.setNecromancerRevive(userName, targetUserName);
			}
			break;
		case "spy":
			actions.addSpyCheck(userName, targetUserName);
			break;
		case "hacker":
			Map<String, Job> allUserJobs = roomManager.getUserJobs(roomNo);

			// 3. 모든 유저를 순회하며 새로운 힌트를 생성하고 DB에 INSERT
			for (Map.Entry<String, Job> entry : allUserJobs.entrySet()) {
				String currentUserName = entry.getKey();
				Job currentUserJob = entry.getValue();

				// 닉네임 조회를 위해 Member 정보 가져오기
				Member member = memberService.getMemberByUserName(currentUserName);
				if (member == null)
					continue;
				String currentUserNick = member.getNickName();

				// 4. "기존" 메서드를 사용하여 새로운 힌트 조회
				// 사망자의 경우 jobNo가 0 또는 *Ghost 계열이므로, 해당 직업 번호에 맞는 힌트가 조회됨
				Hint newHint = roomHintService.selectRandomHintByJob(currentUserJob.getJobNo(), currentUserNick);

				if (newHint != null) {
					// 5. "기존" 메서드를 사용하여 조회된 힌트를 RoomHint 객체로 만들어 삽입
					RoomHint roomHint = new RoomHint(roomNo, currentUserName, // 힌트의 주체
							newHint.getHint(), currentUserNick);

					roomHintService.insertRoomHint(roomHint);
				}
			}

			eventManager.broadcastSystemEvent(roomNo, "해커가 SNS 힌트를 교란시켰습니다!");
			break;
		}

		// 투표 (투표 페이즈에만 동작하도록 PhaseBroadcaster에서 관리)
		if (targetUserName != null && !targetUserName.isBlank()) {
			actions.addVote(userName, targetUserName);
		}
	}

	/**
	 * 밤이 끝날 때 PhaseBroadcaster에 의해 호출되어 밤 동안의 모든 행동을 처리합니다.
	 * 
	 * @param roomNo 방 번호
	 */
	public void processNightActions(int roomNo) {
		NightlyAction actions = nightlyActions.get(roomNo);
		if (actions == null)
			return;

		Set<String> killedByMafia = new HashSet<>();
		Set<String> healedByDoctor = new HashSet<>(actions.getHealTargets());
		Set<String> diedTonight = new HashSet<>();

		// 1. 마피아 킬 대상 결정
		Optional<Map.Entry<String, Integer>> mostVotedEntry = actions.getKillVotes().entrySet().stream()
			    .max(Map.Entry.comparingByValue());

		if (mostVotedEntry.isPresent()) {
			killedByMafia.add(mostVotedEntry.get().getKey());
		}

		// 2. 킬/힐/특수직 상호작용 처리
		for (String target : killedByMafia) {
			Job targetJob = roomManager.getJobForUser(roomNo, target);
			if (targetJob == null)
				continue;
			if (healedByDoctor.contains(target))
				continue;

			if (targetJob.getJobNo() == 9) { // 군인
				roomManager.updateJob(roomNo, target, 1009);
				continue;
			}

			if (targetJob.getJobNo() == 5 || targetJob.getJobNo() == 6) {
				int partnerJobNo = (targetJob.getJobNo() == 5) ? 6 : 5;
				Optional<String> partner = roomManager.getUserJobs(roomNo).entrySet().stream()
						.filter(e -> e.getValue().getJobNo() == partnerJobNo).map(Map.Entry::getKey).findFirst();
				if (partner.isPresent()) {
					diedTonight.add(partner.get());
				} else {
					diedTonight.add(target);
				}
			} else {
				diedTonight.add(target);
			}
		}

		// 3. 네크로맨서 부활 처리
		String revivedUser = actions.getRevivedTarget();
		if (revivedUser != null && diedTonight.contains(revivedUser)) {
			diedTonight.remove(revivedUser);
			roomManager.updateJob(roomNo, actions.getReviver(), 1010);
			Job revivedUserStartJob = roomManager.getStartJobForUser(roomNo, revivedUser);
			if (revivedUserStartJob != null) {
				roomManager.updateJob(roomNo, revivedUser, revivedUserStartJob.getJobNo());
			}
		}

		// 4. 스파이 마피아 접선 처리
		actions.getSpyChecks().forEach((spy, targets) -> {
			for (String target : targets) {
				Job targetJob = roomManager.getJobForUser(roomNo, target);
				if (targetJob != null && targetJob.getJobClass() == 1) {
					roomManager.updateJob(roomNo, spy, 1);
					notifyAbilityResult(roomNo, spy, "당신은 마피아와 접선하여 마피아가 되었습니다!");
					break;
				}
			}
		});

		// 5. 도굴꾼 직업 변경 처리
		String robber = actions.getRobber();
		if (robber != null) {
			Job stolenStartJob = roomManager.getStartJobForUser(roomNo, actions.getRobbedTarget());
			if (stolenStartJob != null) {
				roomManager.updateJob(roomNo, robber, stolenStartJob.getJobNo());
				notifyAbilityResult(roomNo, robber, "당신은 [" + stolenStartJob.getJobVisible() + "]의 직업을 훔쳤습니다!");
			}
		}

		// 6. 최종 사망자 처리
		diedTonight.forEach(deadUser -> {
			roomManager.updateJobToGhost(roomNo, deadUser);
			eventManager.broadcastMafiaKillEvent(roomNo, deadUser);
		});

		actions.clearNightActions();
	}

	/**
	 * 투표 결과를 처리합니다.
	 * 
	 * @param roomNo 방 번호
	 */
	public void processVote(int roomNo) {
		NightlyAction actions = nightlyActions.get(roomNo);
		if (actions == null)
			return;

		Optional<Map.Entry<String, Long>> mostVotedEntry = actions.getVoteCounts().entrySet().stream()
				.max(Map.Entry.comparingByValue());

		// Optional에 값이 있을 때 (즉, 한 명이라도 투표를 받았을 때)만 아래 로직을 실행합니다.
		if (mostVotedEntry.isPresent()) {
			String mostVotedUser = mostVotedEntry.get().getKey();
			Job targetJob = roomManager.getJobForUser(roomNo, mostVotedUser);
			if (targetJob == null) {
				actions.clearVotes();
				return;
			}

			boolean isPolitician = (targetJob.getJobNo() == 4);
			if (!isPolitician) {
				roomManager.updateJobToGhost(roomNo, mostVotedUser);
			}
			eventManager.broadcastVoteResultEvent(roomNo, mostVotedUser, isPolitician);
		} else {
			eventManager.broadcastSystemEvent(roomNo, "아무도 지목되지 않았습니다. 밤이 찾아옵니다.");
		}
		actions.clearVotes();
	}

	private void notifyAbilityResult(int roomNo, String userName, String message) {
		WebSocketSession session = roomManager.getGameSessionByUser(roomNo, userName);
		if (session != null && session.isOpen()) {
			try {
				String payload = mapper.writeValueAsString(Map.of("type", "abilityResult", "msg", message));
				session.sendMessage(new TextMessage(payload));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// =================================================================
	// NightlyAction 내부 클래스: 밤 동안의 행동을 기록
	// =================================================================
	private static class NightlyAction {
		private final Map<String, Integer> killVotes = new ConcurrentHashMap<>();
		private final Map<String, String> doctorHeals = new ConcurrentHashMap<>();
		private final Map<String, String> policeChecks = new ConcurrentHashMap<>();
		private final Map<String, List<String>> spyChecks = new ConcurrentHashMap<>();
		private final Map<String, String> necromancerRevives = new ConcurrentHashMap<>();
		private final Map<String, String> robberTargets = new ConcurrentHashMap<>();
		private final Map<String, String> votes = new ConcurrentHashMap<>();

		public void addMafiaKill(String mafia, String target) {
			killVotes.merge(target, 1, Integer::sum);
		}

		public Map<String, Integer> getKillVotes() {
			return killVotes;
		}

		public void setDoctorHeal(String doctor, String target) {
			doctorHeals.put(doctor, target);
		}

		public Set<String> getHealTargets() {
			return new HashSet<>(doctorHeals.values());
		}

		public void setPoliceCheck(String police, String target) {
			policeChecks.put(police, target);
		}

		public void addSpyCheck(String spy, String target) {
			spyChecks.computeIfAbsent(spy, k -> new CopyOnWriteArrayList<>()).add(target);
		}

		public Map<String, List<String>> getSpyChecks() {
			return spyChecks;
		}

		public void setNecromancerRevive(String necro, String target) {
			necromancerRevives.put(necro, target);
		}

		public String getRevivedTarget() {
			return necromancerRevives.values().stream().findFirst().orElse(null);
		}

		public String getReviver() {
			return necromancerRevives.keySet().stream().findFirst().orElse(null);
		}

		public void setRobberTarget(String robber, String target) {
			robberTargets.put(robber, target);
		}

		public String getRobbedTarget() {
			return robberTargets.values().stream().findFirst().orElse(null);
		}

		public String getRobber() {
			return robberTargets.keySet().stream().findFirst().orElse(null);
		}

		public void addVote(String voter, String target) {
			votes.put(voter, target);
		}

		public Map<String, Long> getVoteCounts() {
			return votes.values().stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()));
		}

		public void clearNightActions() {
			killVotes.clear();
			doctorHeals.clear();
			policeChecks.clear();
			spyChecks.clear();
			necromancerRevives.clear();
			robberTargets.clear();
		}

		public void clearVotes() {
			votes.clear();
		}
	}
}