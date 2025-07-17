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
import com.mafia.game.board.model.vo.BoardLikeDTO;
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
	public ArrayList<Board> topLikedList(HashMap<String, String> filterMap) {
		return dao.topLikedList(sqlSession, filterMap);
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
	public int deleteBoard(Board board) {
		
		int result = 1;
		
		ArrayList<Reply> replyList = board.getReplyList();
		for(Reply reply : replyList) { //댓글 및 댓글에 올린 파일 삭제
			if(reply.getFileNo() != 0) {
				result *= dao.deleteFileOfReply(sqlSession, reply); 
			}
			result *= dao.deleteReply(sqlSession, reply);
		}
		
		String statusOfFile = board.getFileList().get(0).getStatus();
		
		if("Y".equals(statusOfFile)) { //게시글 첨부파일이 존재한다면
			for(BoardFile file : board.getFileList()) {
				result *= dao.deleteFileOfBoard(sqlSession,file.getFileNo());
			}
		}
		
		result *= dao.deleteLoungeBoard(sqlSession, board);
		
		return result; 
	}
	
	@Override
	@Transactional
	public int updateLoungeBoard(Board board, BoardFile file, boolean deleteFile) {
		
	
		
		//게시글 삭제
		int result = dao.updateLoungeBoard(sqlSession, board);
		

		if(file != null) {//변경한 파일 있다면
			result *= dao.insertFile(sqlSession, file); //변경 파일 추가
			
			
			String statusOfFile = board.getFileList().get(0).getStatus();
			if("Y".equals(statusOfFile)) { // 기존 파일 삭제
				result *= dao.deleteFileOfBoard(sqlSession, board.getFileList().get(0).getFileNo());
			}
		}else if(deleteFile){//변경한 파일 없고, 기존파일 삭제버튼 눌렀다면
			
			
			result *= dao.deleteFileOfBoard(sqlSession, board.getFileList().get(0).getFileNo());
		}
		
		
		return result;
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
	
	@Override
	@Transactional
	public int toggleBoardLike(BoardLikeDTO dto) {
		
		
		String existingType = dao.checkBoardLike(sqlSession, dto); //없으면 null 반환
		
		int result = 1;
		
		if(existingType != null) { //이미 해당 게시물에 좋아요 OR 싫어요를 했다면
			
			if(existingType.equals(dto.getType())) { //기존 타입과 현재 등록할 타입이 일치한다면
				result *= dao.deleteBoardLikeHistory(sqlSession, dto); //DB에서 해당 기록 제거
				result *= dao.decreaseBoardLike(sqlSession, dto); //DB에서 좋아요 OR 싫어요 감소시키기
				
				if(result == 0) {
					return result;
				}else { //잘 됐다면
					return -1;
				}
				
			}else {//기존 타입과 현재 등록할 타입이 일치하지 않는다면
				result *= dao.insertBoardLikeHistory(sqlSession, dto);
				result *= dao.increaseBoardLike(sqlSession, dto);
				dto.setType(existingType);
				result *= dao.deleteBoardLikeHistory(sqlSession, dto); //DB에서 해당 기록 제거
				result *= dao.decreaseBoardLike(sqlSession, dto); //DB에서 좋아요 OR 싫어요 감소시키기
				
				if(result == 0) {
					return result;
				}else {//잘 됐다면
					return -100;
				}
			}
			
			
		}else { //한 적 없다면
			result *= dao.insertBoardLikeHistory(sqlSession, dto); //DB에서 해당 기록 추가
			result *= dao.increaseBoardLike(sqlSession, dto); //DB에서 좋아요 OR 싫어요 증가시키기
			
			return result;
		}
	}
	
	@Override
	@Transactional
	public int toggleReplyLike(HashMap<String, Object> needed) {
		
		int exists = dao.checkReplyLike(sqlSession, needed);
		int result = 1;
		if(exists > 0) { //유저가 해당 댓글에 좋아요 누른 기록이 있다면
			
			result *= dao.deleteReplyLikeHistory(sqlSession, needed);
			result *= dao.decreaseReplyLike(sqlSession, needed);
			
			if(result > 0) {
				return -1;
			}else {
				return result;
			}
			
		}else { //유저가 해당 댓글에 좋아요 누른 기록이 없다면
			
			result *= dao.insertReplyLikeHistory(sqlSession, needed);
			result *= dao.increaseReplyLike(sqlSession, needed);
			
			return result;
		}
	}
	
	@Override
	public ArrayList<BoardFile> videoList() {
		return dao.videoList(sqlSession);
	}
	
	@Override
	public int getViewCount(int boardNo) {
		return dao.getViewCount(sqlSession, boardNo);
	}
	
	@Override
	public BoardFile videoDetail(int boardNo) {
		return dao.videoDetail(sqlSession, boardNo);
	}
	
	
	

}
