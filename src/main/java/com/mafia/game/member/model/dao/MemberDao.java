package com.mafia.game.member.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	public int updateMember(SqlSessionTemplate sqlSession, Member m) {
		return sqlSession.update("memberMapper.updateMember",m);
	}

	
	 
	public int deleteMember(SqlSessionTemplate sqlSession, String userName) {
	    return sqlSession.update("memberMapper.deleteMember", userName);
	}

	public List<String> getUserNickList(SqlSessionTemplate sqlSession, String userList) {
		// TODO Auto-generated method stub
		ObjectMapper mapper = new ObjectMapper();
        List<String> userIdList = null;
		try {
			userIdList = mapper.readValue(userList, List.class);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sqlSession.selectList("memberMapper.getUserNickList",userIdList);
	}

	public Member adminLoginDo(SqlSessionTemplate sqlSession, String username) {
		return sqlSession.selectOne("memberMapper.adminLoginDo",username);
	}

	
	
}
