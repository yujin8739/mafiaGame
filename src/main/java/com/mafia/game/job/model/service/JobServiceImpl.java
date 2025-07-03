package com.mafia.game.job.model.service;

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
	
}
