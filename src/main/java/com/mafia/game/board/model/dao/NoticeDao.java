package com.mafia.game.board.model.dao;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.board.model.vo.Notice;
import com.mafia.game.common.model.vo.PageInfo;

@Repository
public class NoticeDao {

	public int noticeCount(SqlSessionTemplate sqlSession, HashMap<String, String> noticeMap) {
		return sqlSession.selectOne("noticeMapper.noticeCount", noticeMap);
	}

	public ArrayList<Notice> noticeList(SqlSessionTemplate sqlSession, HashMap<String, String> noticeMap, PageInfo pi) {
		int limit = pi.getBoardLimit();
		int offset = (pi.getCurrentPage()-1) * limit;
		
		return (ArrayList)sqlSession.selectList("noticeMapper.noticeList", noticeMap, new RowBounds(offset,limit));
	}

	public void increaseCount(SqlSessionTemplate sqlSession, int noticeNo) {
		sqlSession.update("noticeMapper.increaseCount", noticeNo);
	}

	public Notice selectNotice(SqlSessionTemplate sqlSession, int noticeNo) {
		return (Notice)sqlSession.selectOne("noticeMapper.selectNotice", noticeNo);
	}

	public int deleteNotice(SqlSessionTemplate sqlSession, int noticeNo) {
		return sqlSession.update("noticeMapper.deleteNotice", noticeNo);
	}

	public int updateNotice(SqlSessionTemplate sqlSession, Notice notice) {
		return sqlSession.update("noticeMapper.updateNotice", notice);
	}

	public int writeNotice(SqlSessionTemplate sqlSession, Notice notice) {
		return sqlSession.insert("noticeMapper.writeNotice", notice);
	}
	
}
