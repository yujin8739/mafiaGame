package com.mafia.game.webSocket.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.member.model.vo.Member;

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

                    if (room.getUserList() != null && !room.getUserList().isEmpty()) {
                        userList = mapper.readValue(room.getUserList(), new TypeReference<List<String>>() {});
                    }
                    userList.remove(userName);
                    System.out.println(userList.toString());
                    if (userList.isEmpty()) {
                        // 남은 유저가 없으면 방 제거
                        gameRoomService.deleteRoom(roomNo);
                        roomSessions.remove(roomNo);
                    } else {
                        // 아니면 DB에 유저리스트만 갱신
                        String updatedList = mapper.writeValueAsString(userList);
                        gameRoomService.updateUserList(roomNo, updatedList);
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // 로깅 처리 권장
                }
            }

            System.out.println("dd");
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
}