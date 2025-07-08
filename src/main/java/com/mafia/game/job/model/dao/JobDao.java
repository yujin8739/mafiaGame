package com.mafia.game.job.model.dao;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JobDao {

	public List<String> essentialJob8(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.essentialJob8");
	}

	public List<String> optionalJob8(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.optionalJob8");
	}

	public List<String> neutralJob8(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.neutralJob8");
	}

	public String playerList(SqlSessionTemplate sqlSession, int roomNo) {
		return sqlSession.selectOne("jobMapper.selectPlayer", roomNo);
	}

	public String userNickName(SqlSessionTemplate sqlSession, String userName) {
		return sqlSession.selectOne("jobMapper.selectNickName", userName);
	}

}
