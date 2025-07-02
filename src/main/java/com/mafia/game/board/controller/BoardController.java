package com.mafia.game.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mafia.game.board.model.service.BoardService;

@Controller
@RequestMapping("community")
public class BoardController {
	
	@Autowired
	private BoardService service;
	
	@GetMapping("free")
	public String communityPage() {
		
		return "board/community";
		
	}
	
	//07/02 푸쉬 테스트
	
	
	
	
	
}
