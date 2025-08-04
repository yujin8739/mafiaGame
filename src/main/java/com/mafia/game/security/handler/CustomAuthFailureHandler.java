package com.mafia.game.security.handler;

import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.ui.Model;

import com.mafia.game.member.model.vo.Member;
import com.mafia.game.system.exception.EmailNotVerifiedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import java.io.IOException;

public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request,
	                                    HttpServletResponse response,
	                                    AuthenticationException exception) throws IOException {

	    Throwable realCause = exception.getCause();

	    if (realCause instanceof EmailNotVerifiedException ex) {
	        Member member = ex.getMember();
	        request.getSession().setAttribute("unauthenticatedMember", member);
	        //System.out.println("Authentication failed: " + exception.getMessage());
	        if (exception.getCause() != null) {
	            //System.out.println("Cause: " + exception.getCause().getClass().getName());
	        }
	        response.sendRedirect("/login/verifyResult");
	        
	    } else {
	        response.sendRedirect("/login/view?error=true");
	    }
	}
}
