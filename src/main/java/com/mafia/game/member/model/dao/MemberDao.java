package com.mafia.game.member.model.dao;

import java.util.HashMap;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.member.model.vo.Member;



@Repository
public class MemberDao {

	public Member loginDo(SqlSessionTemplate sqlSession, String username) {
		return sqlSession.selectOne("memberMapper.loginDo",username);
	}

	public int countByUsername(SqlSessionTemplate sqlSession, String username) {
		return sqlSession.selectOne("memberMapper.countByUsername", username);
	}

	public int registerMember(SqlSessionTemplate sqlSession, Member member) {
		return sqlSession.insert("memberMapper.insertMember",member);
	}

	public int countByEmail(SqlSessionTemplate sqlSession, String email) {
		return sqlSession.selectOne("memberMapper.countByEmail", email);
	}

	public int allowMailToken(SqlSessionTemplate sqlSession, String email) {
		return sqlSession.update("memberMapper.allowMailToken",email);
	}

	public void updateEmail(SqlSessionTemplate sqlSession, String email, String userName) {
		Map<String, Object> param = new HashMap<>();
	    param.put("userName", userName);
	    param.put("email", email);

		sqlSession.update("memberMapper.mailUpdate", param);
	}
	
	//회원 조회
	public Member getMemberByUserName(SqlSessionTemplate sqlSession, String userName) {
		return sqlSession.selectOne("memberMapper.getMemberByUserName", userName);
	}

	
	
}
