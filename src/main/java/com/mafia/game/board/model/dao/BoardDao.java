package com.mafia.game.board.model.dao;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.BoardLikeDTO;
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

	public int deleteLoungeBoard(SqlSessionTemplate sqlSession, Board board) {
		return sqlSession.update("boardMapper.deleteLoungeBoard", board);
	}

	public int deleteFileOfBoard(SqlSessionTemplate sqlSession, int fileNo) {
		return sqlSession.update("boardMapper.deleteFileOfBoard", fileNo);
	}

	public int updateLoungeBoard(SqlSessionTemplate sqlSession, Board board) {
		return sqlSession.update("boardMapper.updateLoungeBoard", board);
	}

	public String checkBoardLike(SqlSessionTemplate sqlSession, BoardLikeDTO dto) {
		
		return sqlSession.selectOne("boardMapper.checkBoardLike", dto);
	}

	public int deleteBoardLikeHistory(SqlSessionTemplate sqlSession, BoardLikeDTO dto) {
		
		return sqlSession.delete("boardMapper.deleteBoardLikeHistory", dto);
	}

	public int insertBoardLikeHistory(SqlSessionTemplate sqlSession, BoardLikeDTO dto) {
		
		return sqlSession.insert("boardMapper.insertBoardLikeHistory", dto);
	}

	public int decreaseBoardLike(SqlSessionTemplate sqlSession, BoardLikeDTO dto) {
		
		return sqlSession.update("boardMapper.decreaseBoardLike", dto);
	}

	public int increaseBoardLike(SqlSessionTemplate sqlSession, BoardLikeDTO dto) {
		
		return sqlSession.update("boardMapper.increaseBoardLike", dto);
	}

	public ArrayList<Board> topLikedList(SqlSessionTemplate sqlSession, HashMap<String, String> filterMap) {
		return (ArrayList)sqlSession.selectList("boardMapper.topLikedList", filterMap);
	}

	public int checkReplyLike(SqlSessionTemplate sqlSession, HashMap<String, Object> needed) {
		return sqlSession.selectOne("boardMapper.checkReplyLike", needed);
	}

	public int deleteReplyLikeHistory(SqlSessionTemplate sqlSession, HashMap<String, Object> needed) {
		return sqlSession.delete("boardMapper.deleteReplyLikeHistory", needed);
	}

	public int decreaseReplyLike(SqlSessionTemplate sqlSession, HashMap<String, Object> needed) {
		return sqlSession.update("boardMapper.decreaseReplyLike", needed);
	}

	public int insertReplyLikeHistory(SqlSessionTemplate sqlSession, HashMap<String, Object> needed) {
		return sqlSession.insert("boardMapper.insertReplyLikeHistory", needed);
	}

	public int increaseReplyLike(SqlSessionTemplate sqlSession, HashMap<String, Object> needed) {
		return sqlSession.update("boardMapper.increaseReplyLike", needed);
	}

	public ArrayList<BoardFile> videoList(SqlSessionTemplate sqlSession) {
		return (ArrayList)sqlSession.selectList("boardMapper.videoList");
	}

	public int getViewCount(SqlSessionTemplate sqlSession, int boardNo) {
		return sqlSession.selectOne("boardMapper.getViewCount", boardNo);
	}

	public BoardFile videoDetail(SqlSessionTemplate sqlSession, int boardNo) {
		return sqlSession.selectOne("boardMapper.videoDetail", boardNo);
	}



	
}
