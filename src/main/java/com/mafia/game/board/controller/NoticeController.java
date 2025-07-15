package com.mafia.game.board.controller;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mafia.game.board.model.service.NoticeService;
import com.mafia.game.board.model.vo.Notice;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/notice")
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
	public String noticeDetail(@PathVariable int noticeNo,
							   HttpSession session,
							   Model model) {
		String userName = ((Member) session.getAttribute("loginUser")).getUserName();
	    logger.info("세션에 저장된 userName: {}", userName);
		service.increaseCount(noticeNo);
		
	    Notice notice = service.selectNotice(noticeNo);
	    model.addAttribute("notice", notice);
	    return "board/noticeDetail"; // 상세페이지 HTML
	}
	
	@GetMapping("/delete/{noticeNo}")
	public String deleteNotice(@PathVariable int noticeNo, RedirectAttributes redirectAttributes) {
		int result = service.deleteNotice(noticeNo);
		
		if(result > 0) {
			redirectAttributes.addFlashAttribute("msg", "공지사항 삭제 성공");
			return "redirect:/notice/list";
		}else {
			redirectAttributes.addFlashAttribute("msg", "공지사항 삭제 실패");
			return "redirect:/notice/detail/" + noticeNo;
		}
	}
	
	@GetMapping("/update/{noticeNo}")
	public String updateNotice(@PathVariable int noticeNo, Model model) {
		Notice notice = service.selectNotice(noticeNo);
		model.addAttribute("notice", notice);
		
		return "board/noticeUpdateForm";
	}
	
	@PostMapping("/update")
	public String updateNotice(Notice notice, RedirectAttributes redirectAttributes) {
		int result = service.updateNotice(notice);
		
		if(result > 0) {
			redirectAttributes.addFlashAttribute("msg", "공지사항 수정 완료");
			return "redirect:/notice/detail/" + notice.getNoticeNo();
		}else {
			redirectAttributes.addFlashAttribute("msg", "공지사항 수정 실패");
			return "redirect:/notice/detail/" + notice.getNoticeNo();
		}
	}
	
	@GetMapping("/write")
	public String writeNotice() {
		return "board/noticeWriteForm";
	}
	
	@PostMapping("/write")
	public String writeNotice(Notice notice
							 ,RedirectAttributes redirectAttributes
							 ,MultipartFile uploadFile
							 ,HttpSession session) {
		if(!uploadFile.getOriginalFilename().equals("")) {
			String changeName = saveFile(uploadFile, session);
			
			notice.setOriginName(uploadFile.getOriginalFilename());
			notice.setChangeName("/resources/uploadFiles/" + changeName);
		}
		
		int result = service.writeNotice(notice);
		
		if(result > 0) {
			redirectAttributes.addFlashAttribute("msg", "공지사항 등록");
			return "redirect:/notice/list";
		}else {
			redirectAttributes.addFlashAttribute("msg", "공지사항 등록 실패");
			return "redirect:/notice/write";
		}
	}
	
	public String saveFile(MultipartFile uploadFile
						  ,HttpSession session) {
		String originName = uploadFile.getOriginalFilename();
		String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		int ranNum = (int)(Math.random()*90000+10000);
		String ext = originName.substring(originName.lastIndexOf("."));
		String changeName = currentTime + ranNum + ext;
		String savePath = session.getServletContext().getRealPath("/resources/uploadFiles/");
		
		try {
			uploadFile.transferTo(new File(savePath + changeName));
		} catch (IllegalStateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return changeName;
	}
	
}
