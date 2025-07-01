package com.mafia.game.webSockert.server;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/*
 * 상속 또는 구현을 통해 websocket 관련 메소드 구현하기
 * websocket 서버를 만들기 위해서는 TextWebSocketHandler 클래스 또는 
 * 							 WebScoketHandler 인터페이스를 구현한다.
 * 
 * 웹소켓 (WebSocket)
 * -web에서 수행하는 socket 통신으로 연결형 통신이다.(양방향)
 * -web의 기본 통신은 비연결형 통신을 사용한다.
 * -기본 통신은 Http로 진행하고 연결형 통신이 필요한 상황에서 WebSocket을 이용하면 된다.
 * -실시간 작업에 유리 (채팅,시세변동,실시간 알림 기능 등등....)
 * 
 * */


public class WebSocketBasicServer extends TextWebSocketHandler{

	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		//연결이 되었을때 동작하는 메소드 
		//WebSocketSession과 HttpSession은 다른 session 이니 혼동하지 말것
		//WebSocket의 정보가 담겨있는 객체다.
		
		System.out.println("접속!");
		
		System.out.println("WebSocketSession : "+session);
	}

	/*
	 * TextMessage 
	 * -payload : 전달된 데이터(메시지)
	 * -byteCount : 전달된 데이터 바이트수
	 * -last : 메시지가 전부 전달 되어 마지막부분인지 체크(메시지가 나눠서 왔다면)
	 * */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		//메시지를 보낸 세션 정보 : session
		//해당 session에 메시지를 보내주어 확인 가능하도록 한다.
		
		session.sendMessage(message);
		
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		
		System.out.println("접속종료!!");
		
	}
	
	
	
}
