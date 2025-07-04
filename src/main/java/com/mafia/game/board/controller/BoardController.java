package com.mafia.game.board.controller;

import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mafia.game.board.model.service.BoardService;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;

@Controller
@RequestMapping("board")
public class BoardController {
	
	@Autowired
	private BoardService service;
	
	
	@GetMapping("lounge") // 라운지 - 일반 게시판
	public String loungeBoardList(@RequestParam(defaultValue = "1") int currentPage
								 ,String type
								 ,String condition
								 ,String keyword
							     ,Model model) {
		
		
		HashMap<String, String> filterMap = new HashMap<>(); // 필터링을 위한 상태값,조건값,키워드 맵
		filterMap.put("type", type);
		filterMap.put("condition", condition);
		filterMap.put("keyword", keyword);
		
		int listCount = service.listCount(filterMap); //가져올 게시글 개수
		
		System.out.println("게시글 개수 :" + listCount);
		int pageLimit = 10;
		int boardLimit = 20;
		
		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit); //페이징 처리를 위한 정보
		
		ArrayList<Board> boardList = service.boardList(filterMap,pi); //가져올 게시글 목록
		
		model.addAttribute("boardList", boardList);
		model.addAttribute("pi",pi);
		model.addAttribute("filterMap",filterMap); 
		
		return "board/lounge";
		
	}
	
	@GetMapping("/lounge/write") // 라운지 게시글 작성 폼으로 이동
	public String loungeWriteForm() {
		
		return "board/loungeWriteForm";
	}
	
	@PostMapping("/lounge/write") // 라운지에 게시글 작성하기
	public String writeLoungeBoard() {
		return null;
	}
	
	
	
	
	
}
