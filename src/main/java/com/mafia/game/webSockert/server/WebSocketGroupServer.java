package com.mafia.game.webSockert.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


public class WebSocketGroupServer extends TextWebSocketHandler{

	/*
	 * 사용자가 접속 버튼을 누를때마다 새로운 websocketSession 객체가 생성된다
	 * 각 세션 정보를 저장하여 사용자를 구분하고 그룹화 시켜서 메시지를 전달해보자
	 * 
	 * 각 세션정보는 구분되어야 하기 때문에 저장하는 저장소는 중복이 불가능한 Set 저장소를 이용한다.
	 * Set 저장소 중 동기화 처리까지 되어있는 Set 저장소를 사용하여 동시접근도 막아놓기
	 * 
	 * */
	
	//각 사용자정보(WebSocketSession)을 담을 수 있는 저장소 준비
	//동기화 처리가 되어있는 Set 저장소 : CopyOnWriteArraySet 
	private Set<WebSocketSession> users = new CopyOnWriteArraySet<>();
	
	
	
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		//연결이 되었을때 동작하는 메소드 
		//WebSocketSession과 HttpSession은 다른 session 이니 혼동하지 말것
		//WebSocket의 정보가 담겨있는 객체다.
		
		System.out.println("접속!");
		System.out.println("WebSocketSession : "+session);
		
		users.add(session); // 접속하면 접속자 정보를 저장소에 담아주기
		System.out.println("현재 접속자 수 : "+users.size());
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
		//users에 담겨있는 세션들에 모두 메시지 보내주기 
		
		//현재시간 추출
		String time = new SimpleDateFormat("[HH:mm:ss]").format(new Date());
		//메시지 변경하기 
		String msg = time+message.getPayload();
		
		message = new TextMessage(msg); //새 메시지 담아서 객체 생성
		
		//반복문을 이용하여 users에 담겨있는 세션들에게 모두 메시지 보내기 
		for(WebSocketSession user : users) {
			user.sendMessage(message);
		}
		
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		
		System.out.println("접속종료!!");
		//접속 종료할때 위 저장소에 담아놓은 session 정보 삭제하고 가기
		users.remove(session);
		System.out.println("현재 접속자 수 : "+users.size());
	}
	
	
	
}
