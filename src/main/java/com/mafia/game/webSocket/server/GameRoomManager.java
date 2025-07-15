package com.mafia.game.webSocket.server;

import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.webSocket.timer.PhaseBroadcaster;

import jakarta.servlet.http.HttpSession;

import com.mafia.game.job.model.vo.Job;

/**
 * 게임룸을 관리하는 내부 클래스
 */
@Component
public class GameRoomManager {

	@Autowired
	private GameRoomService gameRoomService;
	
	@Autowired
	private ChatService chatService;
	
	//현존하는 게임방의 객체 (vo)
    private final Map<Integer, GameRoom> gameRoomMap = new ConcurrentHashMap<>();
    //방의 세션
    private final Map<Integer, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    
    private final Map<Integer, PhaseBroadcaster> phaseBroadcasters = new ConcurrentHashMap<>();

    public void addSession(int roomNo, WebSocketSession session) {
        roomSessions.putIfAbsent(roomNo, new CopyOnWriteArrayList<>());
        roomSessions.get(roomNo).add(session);
    }

    public void removeSession(int roomNo, WebSocketSession session, CloseStatus status) {
        List<WebSocketSession> sessions = roomSessions.get(roomNo);
        if (sessions != null) {
            sessions.remove(session);

            // 유저 ID 추출
            Member member = (Member) session.getAttributes().get("loginUser");
            String userName = member.getUserName();

            // 2. DB에서 해당 방 조회
            GameRoom room = gameRoomService.selectRoom(roomNo);
            if (room != null && userName != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<String> userList = new ArrayList<>();
                    List<String> readyList = new ArrayList<>();

                    if (room.getUserList() != null && !room.getUserList().isEmpty()) {
                        userList = mapper.readValue(room.getUserList(), new TypeReference<List<String>>() {});
                    }
                    userList.remove(userName);
                    //게임방 세션 나갈시
                    if (room.getReadyUser() != null && !room.getReadyUser().isEmpty()) {
                    	readyList = mapper.readValue(room.getReadyUser(), new TypeReference<List<String>>() {});
                    }
                    readyList.remove(userName);
                    System.out.println(userList.toString());
                    if (userList.isEmpty()) {
                        // 남은 유저가 없으면 방 제거
                        gameRoomService.deleteRoom(roomNo);
                        roomSessions.remove(roomNo);
                    } else {
                        // 아니면 DB에 유저리스트만 갱신
                        String updatedList = mapper.writeValueAsString(userList);
                        String updateReady = mapper.writeValueAsString(readyList);
                        //게임 진행중에는 유저 리스트 업데이트 막기
                        if(!room.getIsGaming().equals("Y")) {
	                        gameRoomService.updateUserList(roomNo, updatedList);
	                        if(userList.size() >= 1) { //유저 갱신될때마다 방장 변경
	                        	gameRoomService.updateRoomMaster(roomNo, userList.get(0));
	                        }
	                        gameRoomService.updateReadyList(roomNo, updateReady);
                        }
                    }
                    
                    if (userList.isEmpty()) {
                        // 타이머 종료
                        stopPhaseBroadcast(roomNo);

                        // 기존 로직
                        gameRoomService.deleteRoom(roomNo);
                        roomSessions.remove(roomNo);
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // 로깅 처리 권 장
                }
            }

            // 메모리에서도 제거
            if (sessions.isEmpty()) {
                roomSessions.remove(roomNo);
            }
        }
    }

    
    public void addUserToRoom(int roomNo, String userName) {
        GameRoom room = gameRoomService.selectRoom(roomNo);
        
        if (room == null) {
            System.out.println("존재하지 않는 방 번호입니다: " + roomNo);  // 혹은 로깅 처리
            return; // 방이 없으면 더 이상 진행하지 않음
        }
        
        List<String> users = new ArrayList<>();
        try {
            users = new ObjectMapper().readValue(room.getUserList(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace(); // JSON 파싱 실패 시 빈 리스트 유지
        }

        if (!users.contains(userName)) {
            users.add(userName);
            try {
                String updatedList = new ObjectMapper().writeValueAsString(users);
                gameRoomService.updateUserList(roomNo, updatedList);
                if(users.size() >= 1) {//유저 갱신될때마다 방장 변경
                	gameRoomService.updateRoomMaster(roomNo, users.get(0));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    public void addReadyToRoom(int roomNo, String userName) { 
        GameRoom room = gameRoomService.selectRoom(roomNo);
        
        if (room == null) {
            System.out.println("존재하지 않는 방 번호입니다: " + roomNo);  // 혹은 로깅 처리
            return; // 방이 없으면 더 이상 진행하지 않음
        }
        
        List<String> users = new ArrayList<>();
        try {
            users = new ObjectMapper().readValue(room.getReadyUser(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace(); // JSON 파싱 실패 시 빈 리스트 유지
        }

        if (!users.contains(userName)) {
            users.add(userName);
            try {
                String updatedList = new ObjectMapper().writeValueAsString(users);
                gameRoomService.updateReadyList(roomNo, updatedList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	public void removeReady(int roomNo, String userName) {
		GameRoom room = gameRoomService.selectRoom(roomNo);
        
        if (room == null) {
            System.out.println("존재하지 않는 방 번호입니다: " + roomNo);  // 혹은 로깅 처리
            return; // 방이 없으면 더 이상 진행하지 않음
        }
        
        List<String> users = new ArrayList<>();
		
        try {
            users = new ObjectMapper().readValue(room.getReadyUser(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace(); // JSON 파싱 실패 시 빈 리스트 유지
        }
        if (users.contains(userName)) {
        	users.remove(userName);
        	try {
                String updatedList = new ObjectMapper().writeValueAsString(users);
                gameRoomService.updateReadyList(roomNo, updatedList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

	public void updateStart(int roomNo) {//게임 시작중 처리, 준비중 인원 초기화
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
    			if (totalPlayers == 3) {
	    	        mafiaCount = 2; citizenCount = 1; neutralCount = 0;
	    	    } else if (totalPlayers == 6) {
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
        PhaseBroadcaster broadcaster = new PhaseBroadcaster(sessions);
        broadcaster.startPhases();

        phaseBroadcasters.put(roomNo, broadcaster); // Map에 등록
	}
	
	public void stopPhaseBroadcast(int roomNo) {
	    PhaseBroadcaster broadcaster = phaseBroadcasters.remove(roomNo);
	    if (broadcaster != null) {
	        broadcaster.stop();
	    }
	}
	
    public void addJobToSession(int roomNo, WebSocketSession session) {
		Member loginUser = (Member) session.getAttributes().get("loginUser");
		if (loginUser == null) {
		    return;
		}
		String userName = loginUser.getUserName();
		System.out.println(">> 로그인 유저: " + userName);
		Map<String, Object> result = gameRoomService.getRoomJob(roomNo,userName);
		String userListJson = clobToString((Clob) result.get("USERLIST"));
		String jobJson = (String) result.get("JOB");
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			if(userListJson != null &&jobJson != null) {
				System.out.println(">> userListJson: " + userListJson);
				 System.out.println(">> jobJson: " + jobJson);
		    	List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {});
				List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {});
				
				int index = userList.indexOf(userName);
				if(!jobList.isEmpty()) {
					int myJob = jobList.get(index);
					session.getAttributes().put("job",gameRoomService.getJobDetail(myJob));
				}
								
			}
			
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    public GameRoom selectRoom(int roomNo) {
		return gameRoomService.selectRoom(roomNo);
    }

    public List<WebSocketSession> getSessions(int roomNo) {
        return roomSessions.getOrDefault(roomNo, Collections.emptyList());
    }

    public void createRoom(GameRoom room) {
        gameRoomMap.put(room.getRoomNo(), room);
    }

    public GameRoom getRoom(int roomNo) {
        return gameRoomMap.get(roomNo);
    }

	public void sendMessage(Message msg) {
		chatService.sendMessage(msg);
	}
	
    public static String clobToString(Clob clob) {
        if (clob == null) return null;

        try (Reader reader = clob.getCharacterStream();
             StringWriter writer = new StringWriter()) {
            char[] buffer = new char[2048];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, length);
            }
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}