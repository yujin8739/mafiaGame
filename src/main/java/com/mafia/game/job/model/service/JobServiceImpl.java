package com.mafia.game.job.model.service;

import java.util.ArrayList;
import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mafia.game.job.model.dao.JobDao;
import com.mafia.game.job.model.vo.Player;

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
	
	@Override
	public Integer jobNo(String job) {
		return dao.jobNo(sqlSession, job);
	}
	
	@Override
	public void playerInfo(int jobNo, String playerName, int roomNo) {
		dao.playerInfo(sqlSession, jobNo, playerName, roomNo);
	}
	
	@Override
	public ArrayList<Player> player(int roomNo) {
		return dao.player(sqlSession, roomNo);
	}
	
}
