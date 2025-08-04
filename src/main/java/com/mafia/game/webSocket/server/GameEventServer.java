package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.member.model.vo.Member;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameEventServer extends TextWebSocketHandler {

    private static final Map<Integer, List<WebSocketSession>> roomEventSessions = new ConcurrentHashMap<>();
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session);
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (loginUser == null || roomNo == -1) {
            session.close(new CloseStatus(4001, "Invalid session for event server"));
            return;
        }

        session.getAttributes().put("roomNo", roomNo);

        roomEventSessions.computeIfAbsent(roomNo, k -> new CopyOnWriteArrayList<>()).add(session);
        //System.out.println("[EventServer] Session " + session.getId() + " connected to event room " + roomNo);

        String nickName = loginUser.getNickName();
        Map<String, String> enterPayload = Map.of(
                "type", "enter",
                "userName", nickName,
                "msg", nickName + "님이 입장하셨습니다."
        );
        broadcast(roomNo, mapper.writeValueAsString(enterPayload), null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 클라이언트로부터 메시지를 받지 않으므로 비워둡니다.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer roomNo = (Integer) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (roomNo == null || loginUser == null) return;
        
        List<WebSocketSession> currentRoomSessions = roomEventSessions.get(roomNo);
        if (currentRoomSessions != null) {
            currentRoomSessions.remove(session);
            //System.out.println("[EventServer] Session " + session.getId() + " disconnected from event room " + roomNo);
            
            String nickName = loginUser.getNickName();
            Map<String, String> leavePayload = Map.of(
                    "type", "leave",
                    "userName", nickName,
                    "msg", nickName + "님이 퇴장하셨습니다."
            );
            broadcast(roomNo, mapper.writeValueAsString(leavePayload), session.getId());

            if (currentRoomSessions.isEmpty()) {
                roomEventSessions.remove(roomNo);
                //System.out.println("[EventServer] Event room " + roomNo + " is empty and removed.");
            }
        }
    }

    public void broadcastEvent(int roomNo, String payload) {
        broadcast(roomNo, payload, null);
    }

    private void broadcast(int roomNo, String payload, String excludeSessionId) {
        List<WebSocketSession> sessions = roomEventSessions.get(roomNo);
        if (sessions == null) return;

        for (WebSocketSession s : sessions) {
            if (s.isOpen() && (excludeSessionId == null || !s.getId().equals(excludeSessionId))) {
                executor.submit(() -> {
                    try {
                        s.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        System.err.println("Failed to send event message: " + e.getMessage());
                    }
                });
            }
        }
    }
    
    private int extractRoomNo(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("roomNo=")) {
            try {
                return Integer.parseInt(query.split("=")[1]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}