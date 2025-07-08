package com.mafia.game.job.model.service;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mafia.game.job.model.dao.JobDao;

@Service
public class JobServiceImpl implements JobService{

	@Autowired
	private SqlSessionTemplate sqlSession;
	
	@Autowired
	private JobDao dao;
	

	@Override
	public List<String> essentialJob8() {
		return dao.essentialJob8(sqlSession);
	}

	@Override
	public List<String> optionalJob8() {
		return dao.optionalJob8(sqlSession);
	}

	@Override
	public List<String> neutralJob8() {
		return dao.neutralJob8(sqlSession);
	}
	
	@Override
	public String playerList(int roomNo) {
		return dao.playerList(sqlSession, roomNo);
	}
	
	@Override
	public String userNickName(String userName) {
		return dao.userNickName(sqlSession, userName);
	}
	
}
