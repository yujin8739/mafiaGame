package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.service.RoomHintService;
import com.mafia.game.game.model.vo.*;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.webSocket.timer.PhaseBroadcaster;
import com.mafia.game.webSocket.timer.RoomCleanupScheduler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.util.*;
import java.util.concurrent.*;

@Component
public class GameRoomManager {

	private final GameRoomService gameRoomService;
	private final RoomHintService roomHintService;
	private final GameChatManager gameChatManager;
	private final GameEventManager gameEventManager;
	private final AbilityManager abilityManager;
	private final MemberService memberService;
	private final ScheduledExecutorService removalScheduler;

	@Value("${game.room.empty-delete-delay-ms:30000}")
	private long cleanupDelayMs;

	private RoomCleanupScheduler roomCleanupScheduler;
	private final Map<Integer, GameRoom> gameRoomMap = new ConcurrentHashMap<>();
	private final Map<Integer, PhaseBroadcaster> phaseBroadcasters = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, WebSocketSession>> roomGameSessions = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, WebSocketSession>> roomChatSessions = new ConcurrentHashMap<>();
	private final Map<Integer, String> roomMasterCache = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, Job>> userJobs = new ConcurrentHashMap<>();
	private final Map<Integer, Map<String, Job>> userStartJobs = new ConcurrentHashMap<>();
	private final ObjectMapper mapper = new ObjectMapper();
	private final Map<String, ScheduledFuture<?>> pendingRemovals = new ConcurrentHashMap<>();
	private final Map<String, Long> userHeartbeats = new ConcurrentHashMap<>();
	private final ScheduledExecutorService heartbeatCheckerScheduler = Executors.newSingleThreadScheduledExecutor();

	@Autowired
	public GameRoomManager(GameRoomService gameRoomService, RoomHintService roomHintService,
			@Lazy GameChatManager gameChatManager, @Lazy GameEventManager gameEventManager,
			@Lazy AbilityManager abilityManager, @Lazy MemberService memberService,
			ScheduledExecutorService removalScheduler) {
		this.gameRoomService = gameRoomService;
		this.roomHintService = roomHintService;
		this.gameChatManager = gameChatManager;
		this.gameEventManager = gameEventManager;
		this.abilityManager = abilityManager;
		this.memberService = memberService;
		this.removalScheduler = removalScheduler;
	}

	@PostConstruct
	public void initScheduler() {
		this.roomCleanupScheduler = new RoomCleanupScheduler(gameRoomService, this, cleanupDelayMs);
		this.heartbeatCheckerScheduler.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);
	}

	public void addSession(int roomNo, WebSocketSession session, String userName) {
		if (pendingRemovals.containsKey(userName)) {
			ScheduledFuture<?> pendingTask = pendingRemovals.remove(userName);
			pendingTask.cancel(false);
			System.out.println("[GameRoomManager] User '" + userName + "' reconnected. Removal cancelled.");
		}
		recordHeartbeat(userName);
		roomGameSessions.computeIfAbsent(roomNo, k -> new ConcurrentHashMap<>()).put(userName, session);
		addUserToRoom(roomNo, userName);
	}

	public void removeSession(int roomNo, WebSocketSession session, String userName) {
		if (pendingRemovals.containsKey(userName)) {
			return;
		}
		System.out.println("[Network Drop] User '" + userName + "' disconnected. Scheduling removal in 5 seconds.");
		ScheduledFuture<?> removalTask = removalScheduler.schedule(() -> {
			System.out.println("[Network Drop] Grace period expired for '" + userName + "'. Executing final removal.");
			handleUserLeave(roomNo, userName);
			pendingRemovals.remove(userName);
		}, 5, TimeUnit.SECONDS);
		pendingRemovals.put(userName, removalTask);
	}

	public void leaveRoomImmediately(int roomNo, String userName) {
		if (pendingRemovals.containsKey(userName)) {
			return;
		}
		System.out.println(
				"[Intentional Leave Signal] User '" + userName + "' is leaving. Scheduling removal in 2 seconds.");
		ScheduledFuture<?> removalTask = removalScheduler.schedule(() -> {
			System.out.println(
					"[Intentional Leave] Grace period expired for '" + userName + "'. Executing final removal.");
			handleUserLeave(roomNo, userName);
			pendingRemovals.remove(userName);
		}, 2, TimeUnit.SECONDS);
		pendingRemovals.put(userName, removalTask);
	}

	public void recordHeartbeat(String userName) {
		userHeartbeats.put(userName, System.currentTimeMillis());
	}

	private void checkHeartbeats() {
		long now = System.currentTimeMillis();
		long timeout = 60000;

		roomGameSessions.forEach((roomNo, sessions) -> {
			sessions.keySet().forEach(userName -> {
				if (now - userHeartbeats.getOrDefault(userName, now) > timeout) {
					WebSocketSession session = sessions.get(userName);
					if (session != null && session.isOpen()) {
						System.out.println("[Heartbeat Timeout] No response from '" + userName
								+ "' for 5 minutes. Closing session.");
						try {
							session.close(new CloseStatus(4008, "Heartbeat timeout"));
						} catch (IOException e) {
							/* Ignore */ }
					}
				}
			});
		});
	}

	private void handleUserLeave(int roomNo, String userName) {
		try {
			userHeartbeats.remove(userName);
			GameRoom room = gameRoomService.selectRoom(roomNo);
			if (room == null)
				return;
			if (!"Y".equals(room.getIsGaming())) {
				List<String> userList = parseJsonList(room.getUserList());
				List<String> readyList = parseJsonList(room.getReadyUser());
				String currentHost = room.getMaster();
				boolean hostLeft = userName.equals(currentHost);
				boolean removed = userList.remove(userName);
				readyList.remove(userName);
				if (removed) {
					if (hostLeft && !userList.isEmpty()) {
						String newHostUserName = userList.get(0);
						gameRoomService.updateRoomMaster(roomNo, newHostUserName);
						roomMasterCache.put(roomNo, newHostUserName);
						readyList.remove(newHostUserName);
					} else if (userList.isEmpty()) {
						gameRoomService.updateRoomMaster(roomNo, null);
						roomMasterCache.remove(roomNo);
					}
					gameRoomService.updateUserList(roomNo, mapper.writeValueAsString(userList));
					gameRoomService.updateReadyList(roomNo, mapper.writeValueAsString(readyList));
					Member member = memberService.getMemberByUserName(userName);
					if (member != null) {
						gameEventManager.broadcastLeaveEvent(roomNo, member.getNickName());
					}
				}
				if (userList.isEmpty()) {
					roomCleanupScheduler.scheduleIfEmpty(roomNo);
					stopPhaseBroadcast(roomNo);
				} else {
					roomCleanupScheduler.cancel(roomNo);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeRoomCaches(int roomNo) {
		roomGameSessions.remove(roomNo);
		roomChatSessions.remove(roomNo);
		roomMasterCache.remove(roomNo);
		phaseBroadcasters.remove(roomNo);
		gameRoomMap.remove(roomNo);
		userJobs.remove(roomNo);
		userStartJobs.remove(roomNo);
	}

	public void bindChatSession(int roomNo, String userName, WebSocketSession session) {
		roomChatSessions.computeIfAbsent(roomNo, k -> new ConcurrentHashMap<>()).put(userName, session);
	}

	public void unbindChatSession(int roomNo, String userName) {
		Map<String, WebSocketSession> chatSessions = roomChatSessions.get(roomNo);
		if (chatSessions != null) {
			chatSessions.remove(userName);
			if (chatSessions.isEmpty())
				roomChatSessions.remove(roomNo);
		}
	}

	public Collection<WebSocketSession> getChatSessions(int roomNo) {
		Map<String, WebSocketSession> sessions = roomChatSessions.get(roomNo);
		return sessions == null ? Collections.emptyList() : sessions.values();
	}

	public WebSocketSession getGameSessionByUser(int roomNo, String userName) {
		Map<String, WebSocketSession> gameSessions = roomGameSessions.get(roomNo);
		return (gameSessions == null) ? null : gameSessions.get(userName);
	}

	public WebSocketSession getSessionByUser(int roomNo, String userName) {
		Map<String, WebSocketSession> chatSessions = roomChatSessions.get(roomNo);
		return (chatSessions == null) ? null : chatSessions.get(userName);
	}

	public void addUserToRoom(int roomNo, String userName) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		if (room == null)
			return;
		List<String> users = parseJsonList(room.getUserList());
		if (!users.contains(userName)) {
			users.add(userName);
			try {
				gameRoomService.updateUserList(roomNo, mapper.writeValueAsString(users));
				if (users.size() == 1) {
					gameRoomService.updateRoomMaster(roomNo, users.get(0));
					roomMasterCache.put(roomNo, users.get(0));
				}
				Member member = memberService.getMemberByUserName(userName);
				if (member != null) {
					gameEventManager.broadcastEnterEvent(roomNo, member.getNickName());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public GameRoom selectRoom(int roomNo) {
		return gameRoomService.selectRoom(roomNo);
	}

	public String getRoomMasterUser(int roomNo) {
		String cached = roomMasterCache.get(roomNo);
		if (cached != null)
			return cached;
		GameRoom room = gameRoomService.selectRoom(roomNo);
		if (room != null) {
			roomMasterCache.put(roomNo, room.getMaster());
			return room.getMaster();
		}
		return null;
	}

	public void addReadyToRoom(int roomNo, String userName) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		if (room == null)
			return;
		List<String> ready = parseJsonList(room.getReadyUser());
		if (!ready.contains(userName)) {
			ready.add(userName);
			try {
				gameRoomService.updateReadyList(roomNo, mapper.writeValueAsString(ready));
				Member member = memberService.getMemberByUserName(userName);
				if (member != null) {
					gameEventManager.broadcastReadyStateChanged(roomNo, member.getNickName(), true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void removeReady(int roomNo, String userName) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		if (room == null)
			return;
		List<String> ready = parseJsonList(room.getReadyUser());
		if (ready.remove(userName)) {
			try {
				gameRoomService.updateReadyList(roomNo, mapper.writeValueAsString(ready));
				Member member = memberService.getMemberByUserName(userName);
				if (member != null) {
					gameEventManager.broadcastReadyStateChanged(roomNo, member.getNickName(), false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void updateStart(int roomNo) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		if (room == null) {
			throw new RuntimeException("존재하지 않는 방 번호입니다: " + roomNo);
		}
		try {
			List<String> users = parseJsonList(room.getUserList());
			int totalPlayers = users.size();
			List<Integer> jobCounts = null;
			if (room.getCount() != null && !room.getCount().isBlank()) {
				jobCounts = mapper.readValue(room.getCount(), new TypeReference<>() {
				});
			}
			int mafiaCount = 0, citizenCount = 0, neutralCount = 0, spyCount = 0;
			if (jobCounts == null || jobCounts.isEmpty()) {
				if (totalPlayers == 6) {
					mafiaCount = 2;
					citizenCount = 4;
				} else if (totalPlayers == 7) {
					mafiaCount = 2;
					citizenCount = 5;
				} else if (totalPlayers == 8) {
					mafiaCount = 2;
					citizenCount = 5;
					neutralCount = 1;
				} else if (totalPlayers == 9) {
					mafiaCount = 2;
					citizenCount = 6;
					spyCount = 1;
				} else if (totalPlayers == 10) {
					mafiaCount = 3;
					citizenCount = 6;
					neutralCount = 1;
					spyCount = 1;
				} else if (totalPlayers == 11) {
					mafiaCount = 3;
					citizenCount = 7;
					neutralCount = 1;
					spyCount = 1;
				} else if (totalPlayers == 12) {
					mafiaCount = 3;
					citizenCount = 8;
					neutralCount = 1;
				} else if (totalPlayers == 13) {
					mafiaCount = 3;
					citizenCount = 8;
					neutralCount = 1;
					spyCount = 1;
				} else if (totalPlayers == 14) {
					mafiaCount = 3;
					citizenCount = 8;
					neutralCount = 2;
					spyCount = 1;
				} else if (totalPlayers == 15) {
					mafiaCount = 3;
					citizenCount = 9;
					neutralCount = 2;
					spyCount = 1;
				} else {
					throw new IllegalArgumentException("게임은 6명에서 15명까지만 가능합니다.");
				}
			} else {
				mafiaCount = jobCounts.get(0);
				citizenCount = jobCounts.get(1);
				neutralCount = jobCounts.get(2);
				spyCount = jobCounts.get(3);
			}
			List<Job> randomJobs = gameRoomService.selectRandomJobs(mafiaCount, citizenCount, neutralCount);
			List<Integer> jobArr = new ArrayList<>();
			for (Job j : randomJobs) {
				jobArr.add(j.getJobNo());
			}
			for (int i = 0; i < spyCount; i++) {
				jobArr.add(13);
			}
			Collections.shuffle(jobArr);
			String updatedJobJson = mapper.writeValueAsString(jobArr);
			gameRoomService.updateStart(roomNo, updatedJobJson);
			addInitialJobsToUsers(roomNo, users, jobArr);
			abilityManager.onGameStart(roomNo);
			List<WebSocketSession> allGameSessions = new ArrayList<>(roomGameSessions.get(roomNo).values());
			if (phaseBroadcasters.containsKey(roomNo)) {
				phaseBroadcasters.get(roomNo).stop();
			}
			PhaseBroadcaster broadcaster = new PhaseBroadcaster(allGameSessions, roomNo, this);
			broadcaster.startPhases();
			phaseBroadcasters.put(roomNo, broadcaster);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("게임 시작 중 오류 발생", e);
		}
	}

	private void addInitialJobsToUsers(int roomNo, List<String> users, List<Integer> jobNos) {
		Map<String, Job> currentJobs = new ConcurrentHashMap<>();
		Map<String, Job> startJobs = new ConcurrentHashMap<>();
		Map<String, WebSocketSession> gameSessions = roomGameSessions.get(roomNo);
		if (gameSessions == null)
			return;
		for (int i = 0; i < users.size(); i++) {
			String userName = users.get(i);
			int jobNo = jobNos.get(i);
			Job job = gameRoomService.getJobDetail(jobNo);
			currentJobs.put(userName, job);
			startJobs.put(userName, job);
			WebSocketSession session = gameSessions.get(userName);
			if (session != null && session.isOpen()) {
				try {
					String payload = mapper.writeValueAsString(Map.of("type", "jobInfo", "job", job, "startJob", job));
					session.sendMessage(new TextMessage(payload));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		userJobs.put(roomNo, currentJobs);
		userStartJobs.put(roomNo, startJobs);
	}

	public void useAbility(int roomNo, String userName, String targetUserName) {
		abilityManager.useAbility(roomNo, userName, targetUserName);
	}

	public void processNightActions(int roomNo) {
		abilityManager.processNightActions(roomNo);
	}

	public void processVote(int roomNo) {
		abilityManager.processVote(roomNo);
	}

	public void endGame(int roomNo, String winner) {
		gameEventManager.broadcastGameEndEvent(roomNo, winner);
		updateStop(roomNo);
		stopPhaseBroadcast(roomNo);
	}

	public void stopPhaseBroadcast(int roomNo) {
		PhaseBroadcaster broadcaster = phaseBroadcasters.remove(roomNo);
		if (broadcaster != null)
			broadcaster.stop();
	}

	public String checkWinner(int roomNo, int phaseIndex) {
		Map<String, Job> currentJobs = userJobs.get(roomNo);
		if (currentJobs == null || currentJobs.isEmpty())
			return "CONTINUE";
		for (Map.Entry<String, Job> entry : userStartJobs.get(roomNo).entrySet()) {
			if (entry.getValue().getJobNo() == 7) {
				String saboteurUser = entry.getKey();
				Job saboteurCurrentJob = getJobForUser(roomNo, saboteurUser);
				if (saboteurCurrentJob != null
						&& (saboteurCurrentJob.getJobNo() == 0 || saboteurCurrentJob.getJobName().contains("Ghost"))) {
					if (phaseIndex <= 1)
						return "NEUTRALITY_WIN";
				}
				break;
			}
		}
		int aliveMafia = 0, aliveCitizen = 0, aliveNeutral = 0;
		for (Job job : currentJobs.values()) {
			if (job.getJobNo() != 0 && !job.getJobName().contains("Ghost")) {
				int jobClass = job.getJobClass();
				if (jobClass == 1 || jobClass == 5)
					aliveMafia++;
				else if (jobClass == 2 || jobClass == 4)
					aliveCitizen++;
				else if (jobClass == 3 || jobClass == 6)
					aliveNeutral++;
			}
		}
		if (aliveMafia >= aliveCitizen + aliveNeutral)
			return "MAFIA_WIN";
		if (aliveMafia == 0 && aliveNeutral == 0 && aliveCitizen > 0)
			return "CITIZEN_WIN";
		if (aliveMafia == 0 && aliveCitizen == 0 && aliveNeutral > 0)
			return "NEUTRALITY_WIN";
		return "CONTINUE";
	}

	public void updateDayNo(int roomNo) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		if (room != null) {
			gameRoomService.updateDayNo(roomNo, room.getDayNo() + 1);
		}
	}

	public void updateStop(int roomNo) {
		gameRoomService.updateStop(roomNo);
	}

	public Job getJobForUser(int roomNo, String userName) {
		Map<String, Job> jobs = userJobs.get(roomNo);
		return (jobs != null) ? jobs.get(userName) : null;
	}

	public Map<String, Job> getUserJobs(int roomNo) {
		return userJobs.get(roomNo);
	}

	public Job getStartJobForUser(int roomNo, String userName) {
		Map<String, Job> jobs = userStartJobs.get(roomNo);
		return (jobs != null) ? jobs.get(userName) : null;
	}

	public void updateJob(int roomNo, String userName, int newJobNo) {
		Job newJob = gameRoomService.getJobDetail(newJobNo);
		if (newJob != null) {
			userJobs.get(roomNo).put(userName, newJob);
		}
	}

	public void updateJobToGhost(int roomNo, String userName) {
		Job startJob = getStartJobForUser(roomNo, userName);
		int ghostJobNo = 0;
		if (startJob != null) {
			int startJobClass = startJob.getJobClass();
			if (startJobClass == 1 || startJobClass == 5)
				ghostJobNo = 1000;
			else if (startJobClass == 3 || startJobClass == 6)
				ghostJobNo = 2000;
		}
		updateJob(roomNo, userName, ghostJobNo);
	}

	public RoomHintService getRoomHintService() {
		return this.roomHintService;
	}

	public List<String> parseJsonList(String json) {
		if (json == null || json.isBlank())
			return new ArrayList<>();
		try {
			return mapper.readValue(json, new TypeReference<>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	private String clobToString(Clob clob) {
		if (clob == null)
			return null;
		try (Reader reader = clob.getCharacterStream(); StringWriter writer = new StringWriter()) {
			char[] buffer = new char[2048];
			int length;
			while ((length = reader.read(buffer)) != -1)
				writer.write(buffer, 0, length);
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}