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
	

}
