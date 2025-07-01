package com.mafia.game.webSockert.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.webSockert.model.vo.Message;



public class WebSocketMemberServer extends TextWebSocketHandler{

	private Set<WebSocketSession> users = new CopyOnWriteArraySet<>();
	
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		
		System.out.println("로그인 서버 접속!");
		System.out.println(session.getAttributes());
		Member loginUser = (Member)session.getAttributes().get("loginUser");
		users.add(session); // 접속하면 접속자 정보를 저장소에 담아주기
		System.out.println(loginUser);
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
		
		//아이디 추출해서 붙이기 
		String userId = ((Member)session.getAttributes().get("loginUser")).getUserName();
		
		//메시지 변경하기 
		String msg = userId+time+message.getPayload();
		
		//사용자 아이디,전달받은 메시지,현재시간문자열
		Message mvo = new Message(0, msg, time, msg, userId, null);
		
		//위에 만든 VO를 JSON문자열 처리하여 해당 문자열을 전달하기
		String jsonMsg = new Gson().toJson(mvo);
		
		message = new TextMessage(jsonMsg); //새 메시지 담아서 객체 생성
		
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
