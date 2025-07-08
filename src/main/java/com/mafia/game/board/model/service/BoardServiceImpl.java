package com.mafia.game.board.model.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.board.model.dao.BoardDao;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.Reply;
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
	
	@Override
	public Board loungeBoardDetail(int boardNo) {
		return dao.loungeBoardDetail(sqlSession,boardNo);
	}
	
	@Override
	@Transactional
	public int writeLoungeBoard(Board board,BoardFile file) {
		
		int boardNo = dao.getBoardNo(sqlSession); //라운지 게시글 번호 먼저 가져오기
		if(boardNo > 0) {
			board.setBoardNo(boardNo);
		}else {
			return boardNo;
		}
		
		
		int result = dao.writeLoungeBoard(sqlSession, board); //라운지 게시글부터 등록
		
		if(result > 0) { //라운지 게시글 등록 성공
			
			if(file != null) { //첨부파일 존재할 경우
				file.setBoardNo(boardNo);
				int result2 = dao.insertFile(sqlSession,file);
				
				return result2;
			}else { //첨부파일 없을 경우
				return result;
			}
			
		}else { //라운지 게시글 등록 실패
			return result;
		}
		
	}
	
	@Override
	public int increaseCount(int boardNo) {
		return dao.increaseCount(sqlSession, boardNo);
	}
	
	@Override
	@Transactional
	public int uploadReply(Reply reply, BoardFile file) {
		
		int result = 1;
		
		if(file != null) {
			int fileNo = dao.getFileNo(sqlSession); //등록할 파일번호 가져오기
			if(fileNo > 0) { // 파일번호 제대로 가져왔을 때
				file.setFileNo(fileNo);
				file.setFileLevel(1);
				
				result = dao.uploadFileOfReply(sqlSession,file);
				if(result > 0) { //파일 등록 성공시
					reply.setFileNo(fileNo);
					result *= dao.uploadReply(sqlSession,reply);
				}else {
					return result;
				}
			}else {
				return fileNo;
			}
		}else { //업로드한 파일 없을 시
			result = dao.uploadReply(sqlSession, reply);
		}
		
		return result;
	}
	
	@Override
	public ArrayList<Reply> getReplyList(int boardNo) {
		return dao.getReplyList(sqlSession,boardNo);
	}
	
	@Override
	public Reply selectReply(int replyNo) {
		return dao.selectReply(sqlSession, replyNo);
	}
	
	@Override
	public int deleteReply(Reply reply) {
		
		String changeName = reply.getChangeName();
		
		int result = dao.deleteReply(sqlSession,reply);
		
		if(result > 0) { //REPLY 테이블에서 삭제 완료
			
			if(changeName != null) { //댓글에 파일 있을 경우
				result *= dao.deleteFileOfReply(sqlSession, reply);
			}
			
		}else { //REPLY 테이블에서 삭제 실패
			
			return result;
		}
		
		return result;
	}
	
	

}
