package com.mafia.game.webSocket.server;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.mafia.game.webSocket.model.vo.GameRoom;

@Component
public class GameMainServer extends TextWebSocketHandler {

    @Autowired
    private GameRoomManager roomManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session); // URI에서 roomNo 추출
        session.getAttributes().put("roomNo", roomNo);

        roomManager.addSession(roomNo, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        int roomNo = (int) session.getAttributes().get("roomNo");

        for (WebSocketSession s : roomManager.getSessions(roomNo)) {
            if (s.isOpen()) {
                s.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        int roomNo = (int) session.getAttributes().get("roomNo");
        roomManager.removeSession(roomNo, session);
    }

    private int extractRoomNo(WebSocketSession session) {
        String query = session.getUri().getQuery(); // e.g., roomNo=3
        if (query != null && query.startsWith("roomNo=")) {
            return Integer.parseInt(query.split("=")[1]);
        }
        return -1;
    }
    
    /**
     * 게임룸을 관리하는 내부 클래스
     */
    @Component
    class GameRoomManager {

    	//현존하는 게임방의 객체 (vo)
        private final Map<Integer, GameRoom> gameRoomMap = new ConcurrentHashMap<>();
        //방의 세션
        private final Map<Integer, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

        public void addSession(int roomNo, WebSocketSession session) {
            roomSessions.putIfAbsent(roomNo, new CopyOnWriteArrayList<>());
            roomSessions.get(roomNo).add(session);
        }

        public void removeSession(int roomNo, WebSocketSession session) {
            List<WebSocketSession> sessions = roomSessions.get(roomNo);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomNo);
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
    }
}
