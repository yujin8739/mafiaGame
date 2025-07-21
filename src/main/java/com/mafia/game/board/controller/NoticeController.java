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
import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${file.uploadNotice.path}")
    private String uploadPath;
	
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
	public String deleteNotice(@PathVariable int noticeNo
	                          ,RedirectAttributes redirectAttributes
	                          ,HttpSession session) {
		
		// 관리자 권한 검사
	    Member loginUser = (Member) session.getAttribute("loginUser");
	    if (loginUser == null || !"sh".equals(loginUser.getUserName())) {
	        return "common/accessDenied";
	    }

	    // 1. 삭제할 notice 객체 조회
	    Notice notice = service.selectNotice(noticeNo);

	    // 2. DB 삭제
	    int result = service.deleteNotice(noticeNo);

	    if (result > 0) {
	        redirectAttributes.addFlashAttribute("msg", "공지사항 삭제 성공");

	        // 3. 파일이 존재할 경우 외부 디렉토리에서 삭제
	        if (notice.getChangeName() != null && !notice.getChangeName().equals("")) {
	            String fullPath = uploadPath + notice.getChangeName().substring(notice.getChangeName().lastIndexOf("/") + 1);

	            File file = new File(fullPath);
	            if (file.exists()) {
	                file.delete();
	            }
	        }

	        return "redirect:/notice/list";
	    } else {
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
	public String updateNotice(Notice notice,
	                           RedirectAttributes redirectAttributes,
	                           MultipartFile reUploadFile,
	                           HttpSession session) {
		
		// 관리자 권한 검사
	    Member loginUser = (Member) session.getAttribute("loginUser");
	    if (loginUser == null || !"sh".equals(loginUser.getUserName())) {
	        return "common/accessDenied";
	    }

	    String deleteFile = null; // 삭제할 기존 파일명 저장용

	    // 새 파일이 업로드 되었을 경우
	    if (!reUploadFile.getOriginalFilename().equals("")) {

	        // 기존 첨부파일이 있었을 경우 삭제 대상 설정
	        if (notice.getChangeName() != null && !notice.getChangeName().equals("")) {
	            deleteFile = uploadPath + notice.getChangeName().substring(notice.getChangeName().lastIndexOf("/") + 1);
	        }

	        // 새로운 파일 업로드 및 Notice 객체에 파일 정보 반영
	        String changeName = saveFile(reUploadFile); // 경로 포함하지 않음
	        notice.setOriginName(reUploadFile.getOriginalFilename());
	        notice.setChangeName("/resources/uploadFile/" + changeName); // DB에 저장될 경로
	    }

	    // 공지사항 DB 업데이트
	    int result = service.updateNotice(notice);

	    if (result > 0) {
	        // 기존 파일 삭제
	        if (deleteFile != null) {
	            File file = new File(deleteFile);
	            if (file.exists()) {
	                file.delete();
	            }
	        }

	        redirectAttributes.addFlashAttribute("msg", "공지사항 수정 완료");
	    } else {
	        redirectAttributes.addFlashAttribute("msg", "공지사항 수정 실패");
	    }

	    return "redirect:/notice/detail/" + notice.getNoticeNo();
	}
	
	@GetMapping("/write")
	public String writeNotice() {
		return "board/noticeWriteForm";
	}
	
	@PostMapping("/write")
    public String writeNotice(Notice notice,
                               RedirectAttributes redirectAttributes,
                               MultipartFile uploadFile,
                               HttpSession session) {
		
		// 관리자 권한 검사
	    Member loginUser = (Member) session.getAttribute("loginUser");
	    if (loginUser == null || !"sh".equals(loginUser.getUserName())) {
	        return "common/accessDenied";
	    }

        if (!uploadFile.getOriginalFilename().equals("")) {
            String changeName = saveFile(uploadFile);

            notice.setOriginName(uploadFile.getOriginalFilename());
            notice.setChangeName("/resources/uploadFile/" + changeName);
        }

        int result = service.writeNotice(notice);

        if (result > 0) {
            redirectAttributes.addFlashAttribute("msg", "공지사항 등록");
            return "redirect:/notice/list";
        } else {
            redirectAttributes.addFlashAttribute("msg", "공지사항 등록 실패");
            return "redirect:/notice/write";
        }
    }

    public String saveFile(MultipartFile uploadFile) {
        String originName = uploadFile.getOriginalFilename();
        String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int ranNum = (int) (Math.random() * 90000 + 10000);
        String ext = originName.substring(originName.lastIndexOf("."));
        String changeName = currentTime + ranNum + ext;

        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();  // 경로가 없으면 자동 생성

        try {
            uploadFile.transferTo(new File(uploadPath + changeName));
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }

        return changeName;
    }
	
}
