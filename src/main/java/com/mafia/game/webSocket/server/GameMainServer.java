package com.mafia.game.webSocket.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;


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
}
