package com.mafia.game.webSocket.server;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
//import com.mafia.game.webSocket.timer.RoomCleanupScheduler;

import jakarta.annotation.PostConstruct;

@Component
public class GameRoomManager {

	@Autowired
	private GameRoomService gameRoomService;
	@Autowired
	private ChatService chatService;
	@Autowired
	private MemberService memberService;
	@Autowired
	private RoomHintService roomHintService;
	
    @Value("${game.room.empty-delete-delay-ms:300000}") // 5분
    private long cleanupDelayMs;
	
	//private RoomCleanupScheduler roomCleanupScheduler; // 수동 생성

	private final Map<Integer, GameRoom> gameRoomMap = new ConcurrentHashMap<>();
	private final Map<Integer, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
	private final Map<Integer, PhaseBroadcaster> phaseBroadcasters = new ConcurrentHashMap<>();

	/** NEW: roomNo -> (userName -> session) */
	private final Map<Integer, Map<String, WebSocketSession>> roomUserSessions = new ConcurrentHashMap<>();
	/** NEW: cache master */
	private final Map<Integer, String> roomMasterCache = new ConcurrentHashMap<>();

	private final ObjectMapper mapper = new ObjectMapper();
	
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	 @PostConstruct
	 public void initScheduler() {
		// this.roomCleanupScheduler = new RoomCleanupScheduler(gameRoomService, this, cleanupDelayMs);
	 }
	    
	 // 캐시 삭제 메서드 추가
	 public void removeRoomCaches(int roomNo) {
		 roomSessions.remove(roomNo);
		 roomUserSessions.remove(roomNo);
		 roomMasterCache.remove(roomNo);
		 phaseBroadcasters.remove(roomNo);
		 gameRoomMap.remove(roomNo);
	}

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
			try {
				old.close(new CloseStatus(4000, "REPLACED"));
			} catch (Exception ignore) {
			}
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
		if (cached != null)
			return cached;
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
			executor.submit(()->{
				try {
					targetSession.sendMessage(new TextMessage(signalJson));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	/* ---------- 세션 제거 ---------- */
	public void removeSession(int roomNo, WebSocketSession session, CloseStatus status) {
		List<WebSocketSession> sessions = roomSessions.get(roomNo);
		if (sessions != null)
			sessions.remove(session);

		Member member = (Member) session.getAttributes().get("loginUser");
		String userName = (member != null ? member.getUserName() : null);

		if (userName != null) {
			Map<String, WebSocketSession> userMap = roomUserSessions.get(roomNo);
			if (userMap != null) {
				userMap.remove(userName);
				if (userMap.isEmpty())
					roomUserSessions.remove(roomNo);
			}
		}

		try {
			GameRoom room = gameRoomService.selectRoom(roomNo);
			if (room != null && userName != null) {
				List<String> userList = parseJsonList(room.getUserList());
				List<String> readyList = parseJsonList(room.getReadyUser());

				userList.remove(userName);
				readyList.remove(userName);

				// 우선 DB에 현재 상태 반영 (게임 중 아닐 때만 업데이트)
				if (!"Y".equals(room.getIsGaming())) {
					gameRoomService.updateUserList(roomNo, mapper.writeValueAsString(userList));
					gameRoomService.updateReadyList(roomNo, mapper.writeValueAsString(readyList));
					if (!userList.isEmpty()) {
						gameRoomService.updateRoomMaster(roomNo, userList.get(0));
						roomMasterCache.put(roomNo, userList.get(0));
					} else {
						// 마스터 없앰
						gameRoomService.updateRoomMaster(roomNo, null);
						roomMasterCache.remove(roomNo);
					}
				} else {
					// 게임 중인데 유저 나감 → 게임 룰에 따라 별도 처리 필요하면 여기서
				}

				// 최종적으로 방 인원에 따라 삭제 예약 또는 예약 취소
				if (userList.isEmpty()) {
					// 게임 진행 여부와 무관하게 모두 나갔으면 삭제 예약
					//roomCleanupScheduler.scheduleIfEmpty(roomNo);
					// ※ 게임 진행용 타이머/브로드캐스트를 즉시 멈출지 여부는 선택
					stopPhaseBroadcast(roomNo); // 필요하면 유지 / 제거
				} else {
					// 아직 유저 있음 → 혹시 이전에 예약된 삭제를 취소
					//roomCleanupScheduler.cancel(roomNo);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (sessions != null && sessions.isEmpty()) {
			roomSessions.remove(roomNo);
		}
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
				if (!users.isEmpty()) {
					gameRoomService.updateRoomMaster(roomNo, users.get(0));
					roomMasterCache.put(roomNo, users.get(0));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/* ---------- 게임 시작 ---------- */
	public void updateStart(int roomNo) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		List<String> users = new ArrayList<>();
        
        if (room == null) {
            System.out.println("존재하지 않는 방 번호입니다: " + roomNo);  // 혹은 로깅 처리
            return; // 방이 없으면 더 이상 진행하지 않음
        }
        try {
        	users = new ObjectMapper().readValue(room.getUserList(), new TypeReference<List<String>>() {});
    		int totalPlayers = users.size();
    		List<Integer> jobCounts = null;
    		if(room.getCount()!=null) {
    			jobCounts = new ObjectMapper().readValue(room.getCount(), new TypeReference<List<Integer>>() {});
    		}
    		
    		int mafiaCount = 0; //마피아 인원
    	    int citizenCount = 0; //시민 인원
    	    int neutralCount = 0; //중립 인원
    	    
    		if(room.getCount() == null || jobCounts.size()<1) {
    			// 일반 모드일때 
//    			if (totalPlayers == 3) {
//	    	        mafiaCount = 1; citizenCount = 2; neutralCount = 0;
//	    	    } else 
	    	    	if (totalPlayers == 6) {
	    	        mafiaCount = 2; citizenCount = 4; neutralCount = 0;
	    	    } else if (totalPlayers == 7) {
	    	        mafiaCount = 2; citizenCount = 5; neutralCount = 0;
	    	    } else if (totalPlayers == 8) {
	    	        mafiaCount = 2; citizenCount = 5; neutralCount = 1;
	    	    } else if (totalPlayers == 9) {
	    	        mafiaCount = 3; citizenCount = 6; neutralCount = 0;
	    	    } else if (totalPlayers == 10) {
	    	        mafiaCount = 3; citizenCount = 6; neutralCount = 1;
	    	    } else if (totalPlayers == 11) {
	    	        mafiaCount = 3; citizenCount = 7; neutralCount = 1;
	    	    } else if (totalPlayers == 12) {
	    	        mafiaCount = 3; citizenCount = 8; neutralCount = 1;
	    	    } else if (totalPlayers == 13) {
	    	        mafiaCount = 4; citizenCount = 9; neutralCount = 0;
	    	    } else if (totalPlayers == 14) {
	    	        mafiaCount = 4; citizenCount = 9; neutralCount = 1;
	    	    } else if (totalPlayers == 15) {
	    	        mafiaCount = 4; citizenCount = 10; neutralCount = 1;
	    	    } else {
	    	        // 6~15명 범위를 벗어나는 경우에 대한 예외 처리
	    	        throw new IllegalArgumentException("게임은 6명에서 15명까지만 가능합니다.");
	    	    }
    		} else {
    			//커스텀 모드 일때 
    			mafiaCount = jobCounts.get(0);
    			citizenCount = jobCounts.get(1);
    			neutralCount = jobCounts.get(2);
    		}
    		
        	List<Job> jobList = gameRoomService.selectRandomJobs(mafiaCount, citizenCount, neutralCount);
        	List<Integer> jobArr = new ArrayList<>();
        	for (Job job : jobList) {
        	    // 각 Job 객체에서 jobNo를 가져와 새로운 리스트에 추가합니다.
        		jobArr.add(job.getJobNo());
        	}
        	String updatedJob = new ObjectMapper().writeValueAsString(jobArr);
        	int result = gameRoomService.updateStart(roomNo,updatedJob);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        List<WebSocketSession> sessions = getSessions(roomNo);
        
        if (phaseBroadcasters.containsKey(roomNo)) {
            phaseBroadcasters.get(roomNo).stop(); // 타이머 종료
        }
        
        PhaseBroadcaster broadcaster = new PhaseBroadcaster(sessions,roomNo,this);
        broadcaster.startPhases();

        phaseBroadcasters.put(roomNo, broadcaster); // Map에 등록
	}

	public void stopPhaseBroadcast(int roomNo) {
		PhaseBroadcaster broadcaster = phaseBroadcasters.remove(roomNo);
		if (broadcaster != null)
			broadcaster.stop();
	}

	/* ---------- 기타: DB 전달 ---------- */
	public void sendMessage(Message msg) {
		chatService.sendMessage(msg);
	}

	public GameRoom selectRoom(int roomNo) {
		return gameRoomService.selectRoom(roomNo);
	}

	public List<WebSocketSession> getSessions(int roomNo) {
		return roomSessions.getOrDefault(roomNo, Collections.emptyList());
	}

	public static String clobToString(Clob clob) {
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

		if (kill == null)
			return;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			List<String> kills = objectMapper.readValue(kill.getKillUser(), new TypeReference<List<String>>() {
			});
			List<String> heals = objectMapper.readValue(kill.getHealUser(), new TypeReference<List<String>>() {
			});

			Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
			String userListJson = clobToString((Clob) result.get("USERLIST"));
			String jobJson = (String) result.get("JOB");

			List<String> userList = objectMapper.readValue(userListJson, new TypeReference<List<String>>() {
			});
			List<Integer> jobList = objectMapper.readValue(jobJson, new TypeReference<List<Integer>>() {
			});

			// 마피아가 여러명이고 각각 죽일수 있으므로 for문으로 확인
			for (String killedUser : kills) {
				boolean isHealSuccess = false;
				String updatedJobJson = null;
				int index = userList.indexOf(killedUser);
				int targetJob = jobList.get(index);
				if(targetJob == 5) {// 연인 대신 죽어주기 기능
					index = jobList.indexOf(6);
				} else if(targetJob == 6) {
					index = jobList.indexOf(5);
				}
				if (index != -1 && index < jobList.size()) {
					if(targetJob == 9) { //군인인 경우 사용후로 변경
						jobList.set(index, 1009);
					} else {
						jobList.set(index, 0);
					}
					updatedJobJson = objectMapper.writeValueAsString(jobList);

					// 의사가 힐할 대상이 죽는지 확인
					// 의사도 여러명일 수 있음
					for (String healUser : heals) {
						if (healUser.equals(killedUser)) {
							isHealSuccess = true;
						}
					}
				}

				// 의사 치료 적중 실패시에만 동작
				if (!isHealSuccess) {
					Message msg = new Message(roomNo, UUID.randomUUID().toString(), "EVENT"
							,chatService.selectEvent(6, memberService.getMemberByUserName(killedUser).getNickName()),
							"시스템",new Date());
					
					sendMessage(msg);
					
					ObjectMapper mapper = new ObjectMapper();
					Map<String, Object> payload = new HashMap<>();

					payload.put("userName", msg.getUserName()); // nickName
					payload.put("msg", msg.getMsg()); // 메시지																								// 본문
					payload.put("type", "EVENT");
					gameRoomService.updateJob(roomNo, updatedJobJson);

					String json = mapper.writeValueAsString(payload);
					

					for (WebSocketSession s : getSessions(roomNo)) {
						executor.submit(()->{
							try {
								s.sendMessage(new TextMessage(json));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						});
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

		if (kill == null)
			return;

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			List<String> kills = objectMapper.readValue(kill.getVote(), new TypeReference<List<String>>() {
			});

			Map<String, Integer> countMap = new HashMap<>();
			for (String name : kills) {
				countMap.put(name, countMap.getOrDefault(name, 0) + 1);
			}

			mostKilldUser = Collections.max(countMap.entrySet(), Map.Entry.comparingByValue()).getKey();

			Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
			String userListJson = clobToString((Clob) result.get("USERLIST"));
			String jobJson = (String) result.get("JOB");

			List<String> userList = objectMapper.readValue(userListJson, new TypeReference<List<String>>() {
			});
			List<Integer> jobList = objectMapper.readValue(jobJson, new TypeReference<List<Integer>>() {
			});

			int index = userList.indexOf(mostKilldUser);
			//정치인은 투표로 죽지 않음
			if (index != -1 && index < jobList.size() && jobList.get(index) != 4) {
				jobList.set(index, 0);
				String updatedJobJson = objectMapper.writeValueAsString(jobList);
				gameRoomService.updateJob(roomNo, updatedJobJson);
			}

			String targetName = memberService.getMemberByUserName(mostKilldUser).getNickName();	
			
			Message msg = new Message(roomNo, UUID.randomUUID().toString(), "EVENT"
					,chatService.selectEvent(9, targetName),
					"시스템",new Date());
			sendMessage(msg);
			
			
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> payload = new HashMap<>();

			payload.put("userName", msg.getUserName()); // nickName
			payload.put("msg", msg.getMsg()); // 메시지																								// 본문
			payload.put("type", "EVENT");

			String json = mapper.writeValueAsString(payload);

			for (WebSocketSession s : getSessions(roomNo)) {
				executor.submit(()->{
					try {
						s.sendMessage(new TextMessage(json));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public String checkWinner(int roomNo, int phaseIndex) {
	    Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
	    String userListJson = clobToString((Clob) result.get("USERLIST")); // 유저 목록 JSON 가져오기
	    String jobJson = (String) result.get("JOB");               // 직업 목록 JSON 가져오기
	    String startJobJson = (String) result.get("STARTJOB");               // 시작 직업 목록 JSON 가져오기
	    ObjectMapper mapper = new ObjectMapper();

	    try {
	        // 유저 목록이나 직업 정보가 하나라도 없으면 게임 시작 전으로 간주
	        if (userListJson == null || jobJson == null || userListJson.isEmpty() || jobJson.isEmpty()) {
	            // 아직 게임 데이터가 완전히 준비되지 않음
	            return "CONTINUE";
	        }

	        //만일 훼방꾼이 첫날 이전에 죽으면 중립 승리
	        if(startJobJson.contains("7") && !jobJson.contains("7") && phaseIndex <= 1) {
	        	return "NEUTRALITY_WIN";
	        }
	        
	        // JSON을 List로 변환
	        List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {});
	        List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {});

	        // MyBatis를 통해 실제 Job 객체 정보를 가져옵니다. (이 부분은 기존과 동일)
	        // 참고: jobList가 비어있으면 jobDetails도 비어있게 됩니다.
	        List<Job> jobDetails = gameRoomService.getJobDetails(jobList);

	        // 방어 코드: 유저 수와 직업 수가 일치하는지 확인합니다.
	        // 수가 다르거나, 유저가 없다면 아직 승패를 판정할 단계가 아님
	        if (userList.isEmpty() || userList.size() != jobDetails.size()) {
	            return "CONTINUE"; // 직업 배정이 완료되지 않았으므로 게임 계속 진행
	        }

	        // --- 이 검사를 통과: 모든 유저에게 직업이 정상적으로 배정된 상태 ---

	        // 카운트
	        int mafiaCount = 0;
	        int citizenCount = 0;
	        int neutralityCount = 0;

	        for (Job job : jobDetails) {
	            // 생존 여부를 확인하는 로직이 필요할 수 있습니다.
	            // 예: if (job.isAlive()) { ... }
	            if (job.getJobClass() == 1) { // 마피아팀
	                mafiaCount++;
	            } else if (job.getJobClass() == 2 || job.getJobClass() == 4) { // 시민팀
	                citizenCount++;
	            } else if (job.getJobClass() == 3) { // 중립팀
	                neutralityCount++;
	            }
	        }

	        // 승리 조건 체크
	        if (mafiaCount >= (citizenCount + neutralityCount)) {
	            return "MAFIA_WIN";
	        } else if (mafiaCount == 0 && neutralityCount == 0) {
	            return "CITIZEN_WIN";
	        } else if (mafiaCount == 0 && citizenCount == 0) {
	            return "NEUTRALITY_WIN";
	        }

	    } catch (JsonProcessingException e) {
	        e.printStackTrace();
	        // JSON 파싱 중 에러가 발생해도 게임이 멈추지 않도록 CONTINUE 반환
	        return "CONTINUE";
	    }

	    // 어떤 승리 조건에도 해당하지 않으면 게임 계속
	    return "CONTINUE";
	}

	public void updateDayNo(int roomNo) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
		gameRoomService.updateDayNo(roomNo, room.getDayNo() + 1);
	}

	public void updateStop(int roomNo) {
		gameRoomService.updateStop(roomNo);
	}

	/* ---------- util ---------- */
	public List<String> parseJsonList(String json) {
		if (json == null || json.isBlank())
			return new ArrayList<>();
		try {
			return mapper.readValue(json, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
}
