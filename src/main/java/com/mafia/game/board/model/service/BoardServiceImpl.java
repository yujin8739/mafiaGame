package com.mafia.game.board.model.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mafia.game.board.model.dao.BoardDao;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.common.model.vo.PageInfo;

@Service
public class BoardServiceImpl implements BoardService{
	
	@Autowired
	private SqlSessionTemplate sqlSession;
	
	@Autowired
	private BoardDao dao;

	@Override
	public int listCount(HashMap<String, String> filterMap) {
		return dao.listCount(sqlSession,filterMap);
	}
	
	@Override
	public ArrayList<Board> boardList(HashMap<String, String> filterMap, PageInfo pi) {
		return dao.boardList(sqlSession,filterMap,pi);
	}
	

}
