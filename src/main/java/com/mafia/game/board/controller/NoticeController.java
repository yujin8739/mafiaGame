package com.mafia.game.board.controller;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mafia.game.board.model.service.NoticeService;
import com.mafia.game.board.model.vo.Notice;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("notice")
public class NoticeController {
	private static final Logger logger = LoggerFactory.getLogger(NoticeController.class);
	@Autowired
	private NoticeService service;
	
	@GetMapping("/list")
	public String noticeList(@RequestParam(defaultValue="1")
							 int currentPage
							,@RequestParam(defaultValue="")
							 String keyword
							,@RequestParam(defaultValue="title")
							 String condition
							,@RequestParam(defaultValue="byDate")
							 String sort
							,Model model) {
		
		HashMap<String, String> noticeMap = new HashMap<>();
		noticeMap.put("keyword", keyword);
		noticeMap.put("condition", condition);
		noticeMap.put("sort", sort);
		
		int noticeCount = service.noticeCount(noticeMap);
		int pageLimit = 10;
		int boardLimit = 10;
		
		PageInfo pi = Pagination.getPageInfo(noticeCount, currentPage, pageLimit, boardLimit);
		
		ArrayList<Notice> noticeList = service.noticeList(noticeMap, pi);
		
		model.addAttribute("noticeList", noticeList);
		model.addAttribute("pi", pi);
		model.addAttribute("noticeMap", noticeMap);
		
		return "board/notice";
	}
	
	@GetMapping("/detail/{noticeNo}")
	public String noticeDetail(@PathVariable int noticeNo, Model model, HttpSession session) {
		String userName = ((Member) session.getAttribute("loginUser")).getUserName();
	    logger.info("세션에 저장된 userName: {}", userName);
		service.increaseCount(noticeNo);
		
	    Notice notice = service.selectNotice(noticeNo);
	    model.addAttribute("notice", notice);
	    return "board/noticeDetail"; // 상세페이지 HTML
	}
	
	@GetMapping("/delete/{noticeNo}")
	public String deleteNotice(@PathVariable int noticeNo, Model model) {
		
		
		return null;
	}
	
}
