package com.mafia.game.webSockert.server;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mafia.game.member.model.vo.Member;
import com.mafia.game.webSockert.model.vo.Message;



public class WebSocketPrivateChatServer extends TextWebSocketHandler{

	//private Set<WebSocketSession> users = new CopyOnWriteArraySet<>();
	//특정 대상에게만 채팅을 보내려면 대상을 특정지어야한다. 이때 사용할 키값은 사용자 아이디
	//사용자 아이디 와 그 사용자의 websocket session 정보를 묶어서 담아주기 
	//map에 동기화 처리 : Collections.synchronizedMap(맵);
	private Map<String,WebSocketSession> users = Collections.synchronizedMap(new HashMap<>());
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		
		Member loginUser = (Member)session.getAttributes().get("loginUser");
		
		String userId = loginUser.getUserName();
		
		System.out.println(userId+"님 접속을 환영합니다!");
		
		//map에 키-값 형태로 넣기 
		//로그인한 아이디가 키값, 해당 대상의 session이 값 
		users.put(userId, session);
		
		System.out.println("현재 접속자 수 : "+users.size());
	}
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
	
		//저장소에 저장되어있는 회원 정보중 내가 원하는 대상이 가지고 있는 session에만 메시지 전달하기 
		//전달받은 데이터 중 상대방의 아이디값을 이용하여 저장소에서 해당 session 찾아내기 
		
		JsonObject jsonObj = JsonParser.parseString(message.getPayload()).getAsJsonObject();
		
		//값을 추출해보기 
		String userId = jsonObj.get("userId").getAsString();
		String otherId = jsonObj.get("otherId").getAsString();
		String msg = jsonObj.get("msg").getAsString();
		
		System.out.println("내 아이디 : "+userId);
		System.out.println("상대방 아이디 : "+otherId);
		System.out.println("전달 메시지: "+msg);
		
		//접속해있는 사용자 아이디중에 상대방 아이디가 있는지 찾아보기 
		//map.containsKey(키) : 해당 맵에 키값이 존재하는지 판별 메소드
		
		System.out.println(otherId+"님 접속 여부 : "+users.containsKey(otherId));
		
		
		//현재시간 추출
		String time = new SimpleDateFormat("[HH:mm:ss]").format(new Date());
		
		//사용자 아이디,전달받은 메시지,현재시간문자열
		Message mvo = new Message(0, msg, time, msg, userId, null);
		
		//위에 만든 VO를 JSON문자열 처리하여 해당 문자열을 전달하기
		String jsonMsg = new Gson().toJson(mvo);
		
		message = new TextMessage(jsonMsg); //새 메시지 담아서 객체 생성
		
		if(users.containsKey(otherId)) {//상대방이 접속해있다면 
			
			//세션꺼내기 
			WebSocketSession otherSession = users.get(otherId);
			
			//해당 세션에 메시지 보내기 
			otherSession.sendMessage(message);
		}
		
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		
		Member loginUser = (Member)session.getAttributes().get("loginUser");
		
		System.out.println("접속종료! "+loginUser.getUserName()+"님 안녕히 가세요.");
		//접속 종료할때 위 저장소에 담아놓은 session 정보 삭제하고 가기
		users.remove(loginUser.getUserName());
		System.out.println("현재 접속자 수 : "+users.size());
	}
	
	
	
}
