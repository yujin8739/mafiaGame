package com.mafia.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AdminInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
	    System.out.println("인터셉터 동작함: " + request.getRequestURI());

	    HttpSession session = request.getSession(false);
	    if (session != null && session.getAttribute("loginUser") != null) {
	        Member user = (Member) session.getAttribute("loginUser");
	        if ("sh".equals(user.getUserName())) {
	            return true;
	        }
	    }

	    response.sendRedirect(request.getContextPath() + "/accessDenied");
	    return false;
	}
}