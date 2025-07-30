package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.member.model.vo.Member;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceSignalServer extends TextWebSocketHandler {

    private static final Map<Integer, Map<String, WebSocketSession>> roomVoiceSessions = new ConcurrentHashMap<>();
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session);
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (loginUser == null || roomNo == -1) {
            session.close(new CloseStatus(4001, "Invalid session or room for voice chat"));
            return;
        }

        String userName = loginUser.getUserName();
        session.getAttributes().put("roomNo", roomNo);
        session.getAttributes().put("userName", userName);

        // 방이 없으면 새로 만들고, 유저의 음성 세션을 추가합니다.
        roomVoiceSessions.computeIfAbsent(roomNo, k -> new ConcurrentHashMap<>()).put(userName, session);
        System.out.println("[VoiceServer] User '" + userName + "' connected to voice room " + roomNo);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> raw = mapper.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() {});
        
        int roomNo = (int) session.getAttributes().get("roomNo");
        String fromUser = (String) session.getAttributes().get("userName");
        
        // 원본 메시지에 발신자 정보를 추가하여 다시 직렬화
        raw.put("from", fromUser); 
        String json = mapper.writeValueAsString(raw);

        String target = (String) raw.get("target");

        Map<String, WebSocketSession> currentRoomSessions = roomVoiceSessions.get(roomNo);
        if (currentRoomSessions == null) {
            return; // 방에 아무도 없으면 종료
        }
        
        if (target != null && !target.isBlank()) {
            // 특정 대상에게 메시지 전송
            WebSocketSession targetSession = currentRoomSessions.get(target);
            if (targetSession != null && targetSession.isOpen()) {
                sendAsync(targetSession, json);
            } else {
                System.out.println("[VoiceServer] Target user '" + target + "' not found or session closed in room " + roomNo);
            }
        } else {
            // 방 전체에 브로드캐스트 (자기 자신 제외)
            for (WebSocketSession s : currentRoomSessions.values()) {
                if (s.isOpen() && !s.getId().equals(session.getId())) {
                    sendAsync(s, json);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer roomNo = (Integer) session.getAttributes().get("roomNo");
        String userName = (String) session.getAttributes().get("userName");

        if (roomNo == null || userName == null) return;

        Map<String, WebSocketSession> currentRoomSessions = roomVoiceSessions.get(roomNo);
        if (currentRoomSessions != null) {
            currentRoomSessions.remove(userName);
            System.out.println("[VoiceServer] User '" + userName + "' disconnected from voice room " + roomNo);
            if (currentRoomSessions.isEmpty()) {
                roomVoiceSessions.remove(roomNo);
                System.out.println("[VoiceServer] Voice room " + roomNo + " is empty and removed.");
            }
        }
    }

    // 비동기 메시지 전송 헬퍼
    private void sendAsync(WebSocketSession session, String payload) {
        executor.submit(() -> {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                System.err.println("Failed to send voice signal message to session " + session.getId() + ": " + e.getMessage());
            }
        });
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