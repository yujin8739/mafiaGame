package com.mafia.game.webSocket.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

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
