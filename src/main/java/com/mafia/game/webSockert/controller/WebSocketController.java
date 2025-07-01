package com.mafia.game.webSockert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


// /websocket/basic 

@Controller
@RequestMapping("/websocket")
public class WebSocketController {

	
	//기본 채팅 페이지로 이동 메소드 
	@GetMapping("/basic")
	public String basic() {
		
		return "websocket/basic";
	}
	
	//그룹 채팅 페이지로 이동 메소드
	@GetMapping("/group")
	public String group() {
		
		return "websocket/group";
	}
	
	//멤버 채팅 페이지로 이동 메소드
	@GetMapping("/member")
	public String member() {
		
		return "websocket/member";
	}
	
	
	//개인 채팅 페이지로 이동 메소드
	@GetMapping("/chat")
	public String chat() {
		
		return "websocket/privateChat";
	}
	
}
