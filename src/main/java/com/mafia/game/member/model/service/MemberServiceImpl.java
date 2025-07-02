package com.mafia.game.member.model.service;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.member.model.dao.MemberDao;
import com.mafia.game.member.model.vo.Member;

@Service
@Transactional
public class MemberServiceImpl implements MemberService {
	
	@Autowired
	private SqlSessionTemplate sqlSession;
	
	@Autowired
	private MemberDao dao;

	@Override
	public Member loginDo(String username) {
		return dao.loginDo(sqlSession, username);
	}

	@Override
	public boolean isUsernameAvailable(String username) {
        int count = dao.countByUsername(sqlSession, username);
        return count == 0;
	}

	@Override
	public boolean isEmailAvailable(String email) {
		// TODO Auto-generated method stub
		int count = dao.countByEmail(sqlSession, email);
		return count == 0;
	}
	
	@Override
	public int registerMember(Member member) {
		return dao.registerMember(sqlSession, member);
	}

	@Override
	public int allowMailToken(String email) {
		return dao.allowMailToken(sqlSession, email);
	}

	@Override
	public void updateEmail(String email, String userName) {
		dao.updateEmail(sqlSession, email, userName);
	}
	
	//회원조회
	@Override
	public Member getMemberByUserName(String userName) {
		// TODO Auto-generated method stub
		return dao.getMemberByUserName(sqlSession, userName);
	}

	@Override
	public int updateMember(Member m) {
		// TODO Auto-generated method stub
		return dao.updateMember(sqlSession, m);
	}
	
	
}
