package com.mafia.game.job.model.dao;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JobDao {

	public List<Integer> essentialJob6(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.essentialJob6");
	}

	public List<Integer> optionalJob6(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.optionalJob6");
	}

	public List<Integer> neutralJob6(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.neutralJob6");
	}

}
