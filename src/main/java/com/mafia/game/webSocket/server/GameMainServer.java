package com.mafia.game.webSocket.server;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.member.model.vo.Member;


public class GameMainServer extends TextWebSocketHandler {

    @Autowired
    private GameRoomManager roomManager;
	
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session);
        session.getAttributes().put("roomNo", roomNo);

        // 로그인한 사용자 정보 추출
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (loginUser != null) {
            String userId = loginUser.getUserName();

            // 👉 userList에 userId를 저장하는 서비스 호출 등 처리
            roomManager.addUserToRoom(roomNo, userId);
        }

        roomManager.addSession(roomNo, session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 메시지 파싱
        ObjectMapper mapper = new ObjectMapper();
        Message msg = mapper.readValue(message.getPayload(), Message.class);

        // DB 저장
        msg.setMsgNo(UUID.randomUUID().toString());
        msg.setChatDate(new Date());
        
        roomManager.sendMessage(msg);
        // 해당 방 세션 목록 가져오기
        int roomNo = msg.getRoomNo();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", msg.getUserName()); // nickName
        payload.put("content", msg.getMsg());     // 메시지 본문

        String json = mapper.writeValueAsString(payload);

        // 브로드캐스트
        for (WebSocketSession s : roomManager.getSessions(roomNo)) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }
    

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        int roomNo = (int) session.getAttributes().get("roomNo");
        roomManager.removeSession(roomNo, session, status);
    }

    private int extractRoomNo(WebSocketSession session) {
        String query = session.getUri().getQuery(); // e.g., roomNo=3
        if (query != null && query.startsWith("roomNo=")) {
            return Integer.parseInt(query.split("=")[1]);
        }
        return -1;
    }
}
