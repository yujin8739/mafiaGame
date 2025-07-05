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
	public List<Integer> essentialJob6() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> optionalJob6() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> neutralJob6() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
