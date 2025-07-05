package com.mafia.game.board.model.dao;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.board.model.vo.Board;
import com.mafia.game.common.model.vo.PageInfo;

@Repository
public class BoardDao {

	public int listCount(SqlSessionTemplate sqlSession, HashMap<String, String> filterMap) {
		return sqlSession.selectOne("boardMapper.listCount",filterMap);
	}

	public ArrayList<Board> boardList(SqlSessionTemplate sqlSession, HashMap<String, String> filterMap, PageInfo pi) {
		int limit = pi.getBoardLimit();
		int offset = (pi.getCurrentPage()-1) * limit;
		
		return (ArrayList)sqlSession.selectList("boardMapper.boardList", filterMap, new RowBounds(offset,limit));
	}

	
}
