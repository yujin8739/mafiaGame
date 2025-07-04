package com.mafia.game.webSocket.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


public class HomeChatServer extends TextWebSocketHandler{
	
	private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public HomeChatServer() {
        System.out.println("HomeChatServer 생성자 호출");
    }
    
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		System.out.println("접속");
		System.out.println("WebSocketSession : "+session);
		sessions.add(session);
	}


	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(message.getPayload()));
            }
        }
		
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		 sessions.remove(session);
	}
	
	
	
}
