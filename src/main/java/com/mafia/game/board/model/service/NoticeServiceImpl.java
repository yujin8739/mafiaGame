package com.mafia.game.board.model.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mafia.game.board.model.dao.NoticeDao;
import com.mafia.game.board.model.vo.Notice;
import com.mafia.game.common.model.vo.PageInfo;

@Service
public class NoticeServiceImpl implements NoticeService {

	@Autowired
	private SqlSessionTemplate sqlSession;
	
	@Autowired
	private NoticeDao dao;
	
	
	@Override
	public int noticeCount(HashMap<String, String> noticeMap) {
		return dao.noticeCount(sqlSession, noticeMap);
	}
	
	@Override
	public ArrayList<Notice> noticeList(HashMap<String, String> noticeMap, PageInfo pi) {
		return dao.noticeList(sqlSession, noticeMap, pi);
	}
	
	@Override
	public Notice selectNotice(int noticeNo) {
		return dao.selectNotice(sqlSession, noticeNo);
	}
	
	@Override
	public void increaseCount(int noticeNo) {
		dao.increaseCount(sqlSession, noticeNo);
	}
	
}
