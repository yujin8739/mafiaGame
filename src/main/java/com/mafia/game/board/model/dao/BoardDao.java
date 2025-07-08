package com.mafia.game.board.model.dao;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.Reply;
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

	public Board loungeBoardDetail(SqlSessionTemplate sqlSession, int boardNo) {
		return sqlSession.selectOne("boardMapper.loungeBoardDetail",boardNo);
	}

	public int writeLoungeBoard(SqlSessionTemplate sqlSession, Board board) {
		return sqlSession.insert("boardMapper.writeLoungeBoard",board);
	}

	public int getBoardNo(SqlSessionTemplate sqlSession) {
		return sqlSession.selectOne("boardMapper.getBoardNo");
	}

	public int insertFile(SqlSessionTemplate sqlSession,BoardFile file) {
		return sqlSession.insert("boardMapper.insertFile",file);
	}

	public int increaseCount(SqlSessionTemplate sqlSession,int boardNo) {
		return sqlSession.update("boardMapper.increaseCount",boardNo);
	}

	public int getFileNo(SqlSessionTemplate sqlSession) {
		return sqlSession.selectOne("boardMapper.getFileNo");
	}

	public int uploadFileOfReply(SqlSessionTemplate sqlSession, BoardFile file) {
		return sqlSession.insert("boardMapper.uploadFileOfReply",file);
	}

	public int uploadReply(SqlSessionTemplate sqlSession, Reply reply) {
		return sqlSession.insert("boardMapper.uploadReply",reply);
	}

	public ArrayList<Reply> getReplyList(SqlSessionTemplate sqlSession, int boardNo) {
		return (ArrayList)sqlSession.selectList("boardMapper.getReplyList", boardNo);
	}

	public Reply selectReply(SqlSessionTemplate sqlSession, int replyNo) {
		return sqlSession.selectOne("boardMapper.selectReply", replyNo);
	}

	public int deleteReply(SqlSessionTemplate sqlSession, Reply reply) {
		return sqlSession.update("boardMapper.deleteReply", reply);
	}

	public int deleteFileOfReply(SqlSessionTemplate sqlSession, Reply reply) {
		return sqlSession.update("boardMapper.deleteFileOfReply", reply);
	}


	
}
