package com.mafia.game.controller;


import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpSession;


//컨트롤러에 공통적으로 적용해야하는 기능을 모아 처리하는 클래스에 부여하는 어노테이션
//공통 예외처리 또는 공통 모델 데이터 등록과 같이 공통적으로 필요한 기능을 부여하여 
//컨트롤러 요청이 들어왔을때 동작시키는 도구
@ControllerAdvice
public class LoginControllerAdvice {
	
	@ModelAttribute("loginUser") // 이 메소드가 반환하는 값을 "loginUser" 라는 이름으로 model에 추가하겠다.
	public Member addLoginUser(HttpSession session) {
		
		//session에 loginUser가 담겨 있는지 판별 후 리턴 
		if(session.getAttribute("loginUser") == null) {
			return null; 
		}else {//있다면 해당 객체 반환
			return (Member)session.getAttribute("loginUser"); 
		}
		
	}
	
	
}
