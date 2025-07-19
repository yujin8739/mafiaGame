package com.mafia.game.webSocket.server;

import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.service.RoomHintService;
import com.mafia.game.game.model.vo.*;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.webSocket.timer.PhaseBroadcaster;

@Component
public class GameRoomManager {

    @Autowired private GameRoomService gameRoomService;
    @Autowired private ChatService chatService;
    @Autowired private MemberService memberService;
    @Autowired private RoomHintService roomHintService;

    private final Map<Integer, GameRoom> gameRoomMap = new ConcurrentHashMap<>();
    private final Map<Integer, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<Integer, PhaseBroadcaster> phaseBroadcasters = new ConcurrentHashMap<>();

    /** NEW: roomNo -> (userName -> session) */
    private final Map<Integer, Map<String, WebSocketSession>> roomUserSessions = new ConcurrentHashMap<>();
    /** NEW: cache master */
    private final Map<Integer, String> roomMasterCache = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    /* ---------- 세션 등록 ---------- */
    public void addSession(int roomNo, WebSocketSession session) {
        roomSessions.putIfAbsent(roomNo, new CopyOnWriteArrayList<>());
        roomSessions.get(roomNo).add(session);
    }

    /* ---------- 유저 ↔ 세션 바인딩 (Failover 시 교체) ---------- */
    public void bindUserSession(int roomNo, String userName, WebSocketSession session) {
        roomUserSessions.computeIfAbsent(roomNo, rn -> new ConcurrentHashMap<>());
        Map<String, WebSocketSession> m = roomUserSessions.get(roomNo);
        WebSocketSession old = m.put(userName, session);
        if (old != null && old.isOpen() && !old.getId().equals(session.getId())) {
            try { old.close(new CloseStatus(4000, "REPLACED")); } catch (Exception ignore) {}
        }
    }

    public WebSocketSession getSessionByUser(int roomNo, String userName) {
        Map<String, WebSocketSession> m = roomUserSessions.get(roomNo);
        return (m == null ? null : m.get(userName));
    }

    public void setRoomMasterCache(int roomNo, String userName) {
        roomMasterCache.put(roomNo, userName);
    }

    public String getRoomMasterUser(int roomNo) {
        String cached = roomMasterCache.get(roomNo);
        if (cached != null) return cached;
        GameRoom room = gameRoomService.selectRoom(roomNo);
        if (room != null) {
            roomMasterCache.put(roomNo, room.getMaster());
            return room.getMaster();
        }
        return null;
    }

    /* ---------- 시그널 전달 ---------- */
    public void forwardSignal(int roomNo, String signalJson, String targetUser) {
        WebSocketSession targetSession = getSessionByUser(roomNo, targetUser);
        if (targetSession != null && targetSession.isOpen()) {
            try { targetSession.sendMessage(new TextMessage(signalJson)); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    /* ---------- 세션 제거 ---------- */
    public void removeSession(int roomNo, WebSocketSession session, CloseStatus status) {
        List<WebSocketSession> sessions = roomSessions.get(roomNo);
        if (sessions != null) sessions.remove(session);

        Member member = (Member) session.getAttributes().get("loginUser");
        String userName = (member != null ? member.getUserName() : null);

        if (userName != null) {
            Map<String, WebSocketSession> userMap = roomUserSessions.get(roomNo);
            if (userMap != null) {
                userMap.remove(userName);
                if (userMap.isEmpty()) roomUserSessions.remove(roomNo);
            }
        }

        // 이하 기존 DB 갱신 로직 (네가 주신 코드에서 그대로 유지)
        // ... (userList/readyList 갱신, master 변경, phaseBroadcaster stop 등) ...
        // ↓ 아래는 간략본. 전체버전에는 너가 올린 removeSession 내용 merge 하세요.
        try {
            GameRoom room = gameRoomService.selectRoom(roomNo);
            if (room != null && userName != null) {
                List<String> userList = parseJsonList(room.getUserList());
                List<String> readyList = parseJsonList(room.getReadyUser());
                userList.remove(userName);
                readyList.remove(userName);

                if (userList.isEmpty()) {
                    stopPhaseBroadcast(roomNo);
                    gameRoomService.deleteRoom(roomNo);
                    roomSessions.remove(roomNo);
                    roomMasterCache.remove(roomNo);
                } else {
                    if(!"Y".equals(room.getIsGaming())) {
                        gameRoomService.updateUserList(roomNo, mapper.writeValueAsString(userList));
                        gameRoomService.updateReadyList(roomNo, mapper.writeValueAsString(readyList));
                        gameRoomService.updateRoomMaster(roomNo, userList.get(0));
                        roomMasterCache.put(roomNo, userList.get(0));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (sessions != null && sessions.isEmpty()) {
            roomSessions.remove(roomNo);
        }
    }

    /* ---------- 기존 너의 addUserToRoom/Ready 등 그대로 ---------- */
    public void addUserToRoom(int roomNo, String userName) {
        GameRoom room = gameRoomService.selectRoom(roomNo);
        if (room == null) return;

        List<String> users = parseJsonList(room.getUserList());
        if(!users.contains(userName)) {
            users.add(userName);
            try {
                gameRoomService.updateUserList(roomNo, mapper.writeValueAsString(users));
                if(!users.isEmpty()) {
                    gameRoomService.updateRoomMaster(roomNo, users.get(0));
                    roomMasterCache.put(roomNo, users.get(0));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void addReadyToRoom(int roomNo, String userName) {
        GameRoom room = gameRoomService.selectRoom(roomNo);
        if (room == null) return;
        List<String> ready = parseJsonList(room.getReadyUser());
        if(!ready.contains(userName)) {
            ready.add(userName);
            try { gameRoomService.updateReadyList(roomNo, mapper.writeValueAsString(ready)); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void removeReady(int roomNo, String userName) {
        GameRoom room = gameRoomService.selectRoom(roomNo);
        if (room == null) return;
        List<String> ready = parseJsonList(room.getReadyUser());
        if(ready.remove(userName)) {
            try { gameRoomService.updateReadyList(roomNo, mapper.writeValueAsString(ready)); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    /* ---------- 게임 시작 ---------- */
    public void updateStart(int roomNo) {
        // (네가 주신 긴 로직 그대로 붙여넣기 가능)
        // 여기서는 최소형 예시
        GameRoom room = gameRoomService.selectRoom(roomNo);
        if (room == null) return;

        List<String> users = parseJsonList(room.getUserList());
        int size = users.size();
        int mafia = 1, citizen = Math.max(size-1, 0), neutral = 0; // 간단형

        List<Job> jobList = gameRoomService.selectRandomJobs(mafia, citizen, neutral);
        List<Integer> jobNos = new ArrayList<>();
        for (Job j : jobList) jobNos.add(j.getJobNo());

        try {
            String updatedJob = mapper.writeValueAsString(jobNos);
            gameRoomService.updateStart(roomNo, updatedJob);
        } catch (Exception e) { e.printStackTrace(); }

        // 타이머 시작
        List<WebSocketSession> sessions = getSessions(roomNo);
        if (phaseBroadcasters.containsKey(roomNo)) phaseBroadcasters.get(roomNo).stop();
        PhaseBroadcaster pb = new PhaseBroadcaster(sessions, roomNo, this);
        pb.startPhases();
        phaseBroadcasters.put(roomNo, pb);
    }

    public void stopPhaseBroadcast(int roomNo) {
        PhaseBroadcaster broadcaster = phaseBroadcasters.remove(roomNo);
        if (broadcaster != null) broadcaster.stop();
    }

    /* ---------- 기타: DB 전달 ---------- */
    public void sendMessage(Message msg) { chatService.sendMessage(msg); }
    public GameRoom selectRoom(int roomNo) { return gameRoomService.selectRoom(roomNo); }
    public List<WebSocketSession> getSessions(int roomNo) { return roomSessions.getOrDefault(roomNo, Collections.emptyList()); }

    public static String clobToString(Clob clob) {
        if (clob == null) return null;
        try (Reader reader = clob.getCharacterStream(); StringWriter writer = new StringWriter()) {
            char[] buffer = new char[2048];
            int length;
            while ((length = reader.read(buffer)) != -1) writer.write(buffer, 0, length);
            return writer.toString();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
    
	public void addJobToSession(int roomNo, WebSocketSession session) {
		Member loginUser = (Member) session.getAttributes().get("loginUser");
		if (loginUser == null) {
			return;
		}
		String userName = loginUser.getUserName();

		System.out.println(">> 로그인 유저: " + userName);
		Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
		String userListJson = clobToString((Clob) result.get("USERLIST"));
		String jobJson = (String) result.get("JOB");
		ObjectMapper mapper = new ObjectMapper();
		try {
			if (userListJson != null && jobJson != null) {
				System.out.println(">> userListJson: " + userListJson);
				System.out.println(">> jobJson: " + jobJson);
				List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {
				});
				List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {
				});

				int index = userList.indexOf(userName);
				String userNick = loginUser.getNickName();
				if (!jobList.isEmpty()) {
					int myJob = jobList.get(index);
					session.getAttributes().put("job", gameRoomService.getJobDetail(myJob));

					Hint hint = roomHintService.selectRandomHintByJob(myJob, userNick);

					RoomHint roomHint = new RoomHint(roomNo, userName, hint.getHint(), userNick);

					roomHintService.insertRoomHint(roomHint);
				}

			}

		} catch (JsonProcessingException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void mafiaKill(int roomNo) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		Kill kill = gameRoomService.selectKill(roomNo, room.getDayNo());
		String deathUser = null;
		
		if (kill == null) return;
		try{
			ObjectMapper objectMapper = new ObjectMapper();
			List<String> kills = objectMapper.readValue(kill.getKillUser(), new TypeReference<List<String>>() {});
			List<String> heals = objectMapper.readValue(kill.getHealUser(), new TypeReference<List<String>>() {});
			
			Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
			String userListJson = clobToString((Clob) result.get("USERLIST"));
			String jobJson = (String) result.get("JOB");
			
			List<String> userList = objectMapper.readValue(userListJson, new TypeReference<List<String>>() {});
			List<Integer> jobList = objectMapper.readValue(jobJson, new TypeReference<List<Integer>>() {}); 

			//마피아가 여러명이고 각각 죽일수 있으므로 for문으로 확인
			for(String killedUser : kills) {
				boolean isHealSuccess = false;
				String updatedJobJson = null;
				int index = userList.indexOf(killedUser);
				if (index != -1 && index < jobList.size()) {
					jobList.set(index, 0);
					updatedJobJson = objectMapper.writeValueAsString(jobList);
					
					//의사가 힐할 대상이 죽는지 확인
					//의사도 여러명일 수 있음
					for(String healUser : heals) {
						if(healUser.equals(killedUser)) {
							isHealSuccess = true;
						}
					}
				}
				
				//의사 치료 적중 실패시에만 동작
				if(!isHealSuccess) { 
					ObjectMapper mapper = new ObjectMapper();
					Map<String, Object> payload = new HashMap<>();
					
					payload.put("userName", "시스템"); // nickName
					payload.put("msg", chatService.selectEvent(6, memberService.getMemberByUserName(killedUser).getNickName()));     // 메시지 본문
					payload.put("type", "EVENT");
					gameRoomService.updateJob(roomNo,updatedJobJson);
					
					String json = mapper.writeValueAsString(payload);
					
					for (WebSocketSession s : getSessions(roomNo)) {
						s.sendMessage(new TextMessage(json));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			updateDayNo(roomNo);
		}
		
	}
    
    public void voteKill(int roomNo) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		Kill kill = gameRoomService.selectKill(roomNo, room.getDayNo());
		String mostKilldUser = null;
			        
		if (kill == null) return;
		
		try{
			ObjectMapper objectMapper = new ObjectMapper();
			List<String> kills = objectMapper.readValue(kill.getVote(), new TypeReference<List<String>>() {});
			
			Map<String, Integer> countMap = new HashMap<>();
			for (String name : kills) {
				countMap.put(name, countMap.getOrDefault(name, 0) + 1);
			}
			
			mostKilldUser = Collections.max(countMap.entrySet(), Map.Entry.comparingByValue()).getKey();
			
			
			Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
			String userListJson = clobToString((Clob) result.get("USERLIST"));
			String jobJson = (String) result.get("JOB");
			
			List<String> userList = objectMapper.readValue(userListJson, new TypeReference<List<String>>() {});
			List<Integer> jobList = objectMapper.readValue(jobJson, new TypeReference<List<Integer>>() {});
			
			int index = userList.indexOf(mostKilldUser);
			if (index != -1 && index < jobList.size()) {
				jobList.set(index, 0);
				String updatedJobJson = objectMapper.writeValueAsString(jobList);
				gameRoomService.updateJob(roomNo,updatedJobJson);
			}
			
			String targetName = memberService.getMemberByUserName(mostKilldUser).getNickName();
			
			ObjectMapper mapper = new ObjectMapper();
			
	        Map<String, Object> payload = new HashMap<>(); 
	        payload.put("userName", "시스템"); // nickName
	        payload.put("msg", chatService.selectEvent(9, targetName));     // 메시지 본문
	        payload.put("type", "EVENT");

	        String json = mapper.writeValueAsString(payload);
	        
	        for (WebSocketSession s : getSessions(roomNo)) {
	        	s.sendMessage(new TextMessage(json));
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public String checkWinner(int roomNo) {
        Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
        String jobJson = (String) result.get("JOB");
        ObjectMapper mapper = new ObjectMapper();

        try {
            if (jobJson == null) {
                return "NO_GAME_DATA";
            }

            // JSON → List<Integer>
            List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {});

            // MyBatis 호출
            List<Job> jobDetails = gameRoomService.getJobDetails(jobList);

            // 카운트
            int mafiaCount = 0;
            int citizenCount = 0;
            int neutralityCount = 0;

            for (Job job : jobDetails) {
                if (job.getJobClass() == 1) {
                    mafiaCount++;
                } else if (job.getJobClass() == 2) {
                    citizenCount++;
                } else if (job.getJobClass() == 3) {
                	neutralityCount++;
                }
            }

            // 승리 조건 체크
            if (mafiaCount > (citizenCount + neutralityCount)) {
                return "MAFIA_WIN";
            } else if (mafiaCount == 0 && neutralityCount == 0) {
                return "CITIZEN_WIN";
            } else if (mafiaCount == 0 && citizenCount == 0) {
            	return "NEUTRALITY_WIN";
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return "CONTINUE";
    }
    
	public void updateDayNo(int roomNo) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		gameRoomService.updateDayNo(roomNo, room.getDayNo()+1);
	}
	
	public void updateStop(int roomNo) {
		gameRoomService.updateStop(roomNo);
	}

    /* ---------- util ---------- */
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
