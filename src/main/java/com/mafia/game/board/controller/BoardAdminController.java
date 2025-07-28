package com.mafia.game.board.controller;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mafia.game.board.model.service.BoardService;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;

@RequestMapping("/api/board")
@RestController
public class BoardAdminController {
	
	@Autowired
	private BoardService service;
	
	@GetMapping("/boardList")
	public ArrayList<Board> getBoardList(@RequestParam(defaultValue = "1") int currentPage, String typeName, String condition,
										String keyword){
		int listCount = service.listCount(null); // 가져올 게시글 개수

		int pageLimit = 10;
		int boardLimit = 20;

		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit); // 페이징 처리를 위한 정보
		return service.boardList(null, pi);
	}
	
	
	@PostMapping("/delete")
	public void boardDelete(int boardNo) {
		System.out.println("boardNo : " + boardNo);
		
	}
}
