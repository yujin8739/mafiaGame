package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.service.RoomHintService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Hint;
import com.mafia.game.game.model.vo.Kill;
import com.mafia.game.game.model.vo.RoomHint;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.webSocket.timer.PhaseBroadcaster;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AbilityManager {

	private final GameRoomManager roomManager;
	private final GameEventManager eventManager;
	private final GameRoomService gameRoomService;
	private final RoomHintService roomHintService;
	private final MemberService memberService;
	private final ObjectMapper mapper = new ObjectMapper();

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

	public void onGameStart(int roomNo) {
		nightlyActions.put(roomNo, new NightlyAction());
	}

	public void useAbility(int roomNo, String userName, String targetUserName) {
		PhaseBroadcaster broadcaster = roomManager.getPhaseBroadcaster(roomNo);
		if (broadcaster == null || !"NIGHT".equals(broadcaster.getCurrentPhase())) {
			notifyAbilityResult(roomNo, userName, "밤이 아닐 때는 능력을 사용할 수 없습니다.");
			return;
		}

		Job userJob = roomManager.getJobForUser(roomNo, userName);
		if (userJob == null)
			return;

		NightlyAction actions = nightlyActions.computeIfAbsent(roomNo, k -> new NightlyAction());
		String jobName = userJob.getJobName();
		Job targetJob = roomManager.getJobForUser(roomNo, targetUserName);

		switch (jobName) {
		case "mafia":
			actions.setMafiaKill(userName, targetUserName);
			break;
		case "doctor":
			actions.setDoctorHeal(userName, targetUserName);
			break;
		case "police":
			if (targetJob != null && targetJob.getJobName().toLowerCase().contains("ghost")) {
				notifyAbilityResult(roomNo, userName, "죽은 플레이어는 조사할 수 없습니다.");
				return;
			}
			actions.setPoliceCheck(userName, targetUserName);
			boolean isMafia = (targetJob != null && targetJob.getJobClass() == 1);
			notifyAbilityResult(roomNo, userName,
					String.format("[%s]님은 %s.", targetUserName, isMafia ? "마피아입니다" : "마피아가 아닙니다"));
			break;
		case "robber":
			if (targetJob != null && targetJob.getJobName().toLowerCase().contains("ghost")) {
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
			for (Map.Entry<String, Job> entry : allUserJobs.entrySet()) {
				String currentUserName = entry.getKey();
				Job currentUserJob = entry.getValue();
				Member member = memberService.getMemberByUserName(currentUserName);
				if (member == null)
					continue;
				String currentUserNick = member.getNickName();
				Hint newHint = roomHintService.selectRandomHintByJob(currentUserJob.getJobNo(), currentUserNick);
				if (newHint != null) {
					RoomHint roomHint = new RoomHint(roomNo, currentUserName, newHint.getHint(), currentUserNick);
					roomHintService.insertRoomHint(roomHint);
				}
			}
			eventManager.broadcastSystemEvent(roomNo, "해커가 SNS 힌트를 교란시켰습니다!");
			break;
		}
	}

	public void castVote(int roomNo, String voterName, String targetName) {
		PhaseBroadcaster broadcaster = roomManager.getPhaseBroadcaster(roomNo);
		if (broadcaster == null || !"VOTE".equals(broadcaster.getCurrentPhase())) {
			notifyAbilityResult(roomNo, voterName, "투표 시간이 아닙니다.");
			return;
		}

		Job voterJob = roomManager.getJobForUser(roomNo, voterName);
		Job targetJob = roomManager.getJobForUser(roomNo, targetName);

		if (voterJob == null || targetJob == null || voterJob.getJobName().toLowerCase().contains("ghost")
				|| targetJob.getJobName().toLowerCase().contains("ghost")) {
			notifyAbilityResult(roomNo, voterName, "죽은 플레이어에게는 투표할 수 없습니다.");
			return;
		}

		GameRoom room = roomManager.selectRoom(roomNo);
		if (room == null)
			return;

		int dayNo = room.getDayNo();
		try {
			Kill killData = gameRoomService.selectKill(roomNo, dayNo);
			if (killData == null) {
				killData = new Kill(roomNo, dayNo, "[]", "[]", "[]");
				gameRoomService.insertKill(killData);
			}

			List<String> votes = mapper.readValue(killData.getVote(), new TypeReference<List<String>>() {
			});
			votes.add(targetName);
			String voteJson = mapper.writeValueAsString(votes);
			killData.setVote(voteJson);
			gameRoomService.updateKill(killData);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleLoverChainDeath(int roomNo, String deadUserName, Set<String> diedTonight,
			Set<String> sacrifices) {
		if (sacrifices.contains(deadUserName)) {
			return;
		}

		Job deadUserJob = roomManager.getJobForUser(roomNo, deadUserName);
		if (deadUserJob == null || (deadUserJob.getJobNo() != 5 && deadUserJob.getJobNo() != 6)) {
			return;
		}
		int partnerJobNo = (deadUserJob.getJobNo() == 5) ? 6 : 5;

		Optional<String> partnerOpt = roomManager.getUserJobs(roomNo).entrySet().stream().filter(e -> {
			Job job = e.getValue();
			return job.getJobNo() == partnerJobNo && !job.getJobName().toLowerCase().contains("ghost");
		}).map(Map.Entry::getKey).findFirst();

		if (partnerOpt.isPresent()) {
			String partnerName = partnerOpt.get();
			if (!diedTonight.contains(partnerName)) {
				diedTonight.add(partnerName);
			}
		}
	}

	public void processVote(int roomNo) {
		GameRoom room = roomManager.selectRoom(roomNo);
		if (room == null)
			return;

		Kill killData = gameRoomService.selectKill(roomNo, room.getDayNo());
		if (killData == null || killData.getVote() == null || killData.getVote().equals("[]")) {
			eventManager.broadcastSystemEvent(roomNo, "아무도 지목되지 않았습니다. 밤이 찾아옵니다.");
			return;
		}

		try {
			List<String> votes = mapper.readValue(killData.getVote(), new TypeReference<List<String>>() {
			});
			Map<String, Long> voteCounts = votes.stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()));
			Optional<Map.Entry<String, Long>> mostVotedEntry = voteCounts.entrySet().stream()
					.max(Map.Entry.comparingByValue());

			if (mostVotedEntry.isPresent()) {
				String mostVotedUser = mostVotedEntry.get().getKey();
				Job targetJob = roomManager.getJobForUser(roomNo, mostVotedUser);
				if (targetJob == null)
					return;

				boolean isPolitician = (targetJob.getJobNo() == 4);
				if (!isPolitician) {
					roomManager.updateJobToGhost(roomNo, mostVotedUser);
				}

				eventManager.broadcastVoteResultEvent(roomNo, mostVotedUser, isPolitician);

				PhaseBroadcaster broadcaster = roomManager.getPhaseBroadcaster(roomNo);
				if (broadcaster != null) {
					broadcaster.broadcastCurrentPhaseState();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void processNightActions(int roomNo) {
		NightlyAction actions = nightlyActions.get(roomNo);
		if (actions == null)
			return;

		Set<String> killedByMafia = new HashSet<>(actions.getKillTargets());
		Set<String> healedByDoctor = new HashSet<>(actions.getHealTargets());

		Set<String> diedTonight = new HashSet<>();
		Set<String> sacrifices = new HashSet<>();

		// 1. 희생 규칙 적용
		for (String target : killedByMafia) {
			if (healedByDoctor.contains(target))
				continue;

			Job targetJob = roomManager.getJobForUser(roomNo, target);
			if (targetJob == null)
				continue;

			if (targetJob.getJobNo() == 5 || targetJob.getJobNo() == 6) { // 대상이 연인일 경우
				int partnerJobNo = (targetJob.getJobNo() == 5) ? 6 : 5;
				Optional<String> partnerOpt = roomManager.getUserJobs(roomNo).entrySet().stream().filter(e -> {
					Job job = e.getValue();
					return job.getJobNo() == partnerJobNo && !job.getJobName().toLowerCase().contains("ghost");
				}).map(Map.Entry::getKey).findFirst();

				if (partnerOpt.isPresent()) {
					String partnerName = partnerOpt.get();
					diedTonight.add(partnerName);
					sacrifices.add(partnerName);
					continue;
				}
			}
			if (targetJob.getJobNo() == 9) {
				roomManager.updateJob(roomNo, target, 1009);
				continue;
			}
			diedTonight.add(target);
		}

		// 2. 연쇄 죽음 규칙 적용 (희생자가 아닌 경우에만)
		for (String deadUser : new HashSet<>(diedTonight)) {
			handleLoverChainDeath(roomNo, deadUser, diedTonight, sacrifices);
		}

		// 3. 부활 처리
		String revivedUser = actions.getRevivedTarget();
		if (revivedUser != null && diedTonight.contains(revivedUser)) {
			diedTonight.remove(revivedUser);
			roomManager.updateJob(roomNo, actions.getReviver(), 1010);
			Job revivedUserStartJob = roomManager.getStartJobForUser(roomNo, revivedUser);
			if (revivedUserStartJob != null) {
				roomManager.updateJob(roomNo, revivedUser, revivedUserStartJob.getJobNo());
			}
		}

		// 4. 기타 능력 처리 (스파이, 도둑)
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
		String robber = actions.getRobber();
		if (robber != null) {
			Job stolenStartJob = roomManager.getStartJobForUser(roomNo, actions.getRobbedTarget());
			if (stolenStartJob != null) {
				roomManager.updateJob(roomNo, robber, stolenStartJob.getJobNo());
				notifyAbilityResult(roomNo, robber, "당신은 [" + stolenStartJob.getJobVisible() + "]의 직업을 훔쳤습니다!");
			}
		}

		// 5. 최종 사망 처리
		diedTonight.forEach(deadUser -> {
			roomManager.updateJobToGhost(roomNo, deadUser);
			eventManager.broadcastMafiaKillEvent(roomNo, deadUser);
		});

		// 6. DB 업데이트 및 상태 브로드캐스트
		GameRoom room = roomManager.selectRoom(roomNo);
		if (room != null) {
			int dayNo = room.getDayNo();
			try {
				Kill killData = gameRoomService.selectKill(roomNo, dayNo);
				if (killData == null) {
					killData = new Kill(roomNo, dayNo, "[]", "[]", "[]");
					gameRoomService.insertKill(killData);
				}
				String mafiaVoteJson = mapper.writeValueAsString(new ArrayList<>(actions.getKillTargets()));
				String doctorHealJson = mapper.writeValueAsString(new ArrayList<>(actions.getHealTargets()));
				killData.setKillUser(mafiaVoteJson);
				killData.setHealUser(doctorHealJson);
				gameRoomService.updateKill(killData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		actions.clearNightActions();

		PhaseBroadcaster broadcaster = roomManager.getPhaseBroadcaster(roomNo);
		if (broadcaster != null) {
			broadcaster.broadcastCurrentPhaseState();
		}
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

	private static class NightlyAction {
		private final Map<String, String> mafiaKills = new ConcurrentHashMap<>();
		private final Map<String, String> doctorHeals = new ConcurrentHashMap<>();
		private final Map<String, String> policeChecks = new ConcurrentHashMap<>();
		private final Map<String, List<String>> spyChecks = new ConcurrentHashMap<>();
		private final Map<String, String> necromancerRevives = new ConcurrentHashMap<>();
		private final Map<String, String> robberTargets = new ConcurrentHashMap<>();

		public void setMafiaKill(String mafia, String target) {
			mafiaKills.put(mafia, target);
		}

		public Set<String> getKillTargets() {
			return new HashSet<>(mafiaKills.values());
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

		public void clearNightActions() {
			mafiaKills.clear();
			doctorHeals.clear();
			policeChecks.clear();
			spyChecks.clear();
			necromancerRevives.clear();
			robberTargets.clear();
		}
	}
}