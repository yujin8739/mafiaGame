package com.mafia.game.member.model.service;

import java.util.List;

import com.mafia.game.member.model.vo.Member;


public interface MemberService {

	Member loginDo(String username);

	boolean isUsernameAvailable(String username);
	
	boolean isEmailAvailable(String username);

	int registerMember(Member member);

	int allowMailToken(String email);

	void updateEmail(String email, String userName);
}
