package com.mafia.game.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mafia.game.board.model.service.NoticeService;
import com.mafia.game.board.model.vo.Notice;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.security.config.JwtUtil;

@RestController
@RequestMapping("/api") 
public class ApiController {
	
	@Autowired
	private MemberService mService;
	
	@Autowired
	private BCryptPasswordEncoder bcrypt;

	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	private NoticeService nService;
	
	@Value("${file.uploadNotice.path}")
    private String uploadPath;
	
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> loginMember(@RequestBody Member m, Model model) {

		// 사용자정보,토큰,메시지등등 응답 데이터
		Map<String, Object> response = new HashMap<>();

		try {

			Member loginUser =  mService.adminLoginDo(m.getUserName());

			System.out.println(loginUser.toString());
			System.out.println(m.toString());

			if (loginUser != null && bcrypt.matches(m.getPassword(), loginUser.getPassword())) { // 성공시

				String token = jwtUtil.generateToken(loginUser.getUserName());

				loginUser.setPassword(token);

				response.put("token", token);
				response.put("user", loginUser);
				response.put("success", true);
				response.put("message", "로그인 성공!");

				return ResponseEntity.ok(response);
			} else {

				response.put("success", false);
				response.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");

				return ResponseEntity.badRequest().body(response);
			} 
		} catch (Exception e) {
			response.put("success", false);
			response.put("message", "로그인 처리 중 오류 발생!");

			e.printStackTrace();

			return ResponseEntity.internalServerError().body(response);
		}

	}
	
	//공지사항 리스트
	@GetMapping("/notices")
	public HashMap<String, Object> getNotices(
				@RequestParam(defaultValue = "1") int currentPage,
				@RequestParam(defaultValue = "") String keyword,
	            @RequestParam(defaultValue = "title") String condition,
	            @RequestParam(defaultValue = "byDate") String sort
			) {
		HashMap<String, Object> result = new HashMap<>();
		
		HashMap<String, String> noticeMap = new HashMap<>();
        noticeMap.put("keyword", keyword);
        noticeMap.put("condition", condition);
        noticeMap.put("sort", sort);

        int noticeCount = nService.noticeCount(noticeMap);
        int pageLimit = 10;
        int boardLimit = 10;

        PageInfo pi = Pagination.getPageInfo(noticeCount, currentPage, pageLimit, boardLimit);
        ArrayList<Notice> noticeList = nService.noticeList(noticeMap, pi);

        result.put("noticeList", noticeList);
        result.put("pi", pi);

        return result;
	}
	
	//공지사항 삭제
	@DeleteMapping("/notices/{noticeNo}")
	public ResponseEntity<String> deleteNotice(@PathVariable int noticeNo) {
	    // 1. 삭제할 notice 객체 조회
	    Notice notice = nService.selectNotice(noticeNo);
	    if (notice == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 공지사항을 찾을 수 없습니다.");
	    }

	    // 2. DB 삭제
	    int result = nService.deleteNotice(noticeNo);

	    if (result > 0) {
	        // 3. 첨부파일 삭제
	        if (notice.getChangeName() != null && !notice.getChangeName().isEmpty()) {
	            // 파일 경로에서 마지막 파일명만 추출
	            String fileName = notice.getChangeName().substring(notice.getChangeName().lastIndexOf("/") + 1);
	            String fullPath = uploadPath + fileName;

	            File file = new File(fullPath);
	            if (file.exists()) {
	                file.delete();
	            }
	        }

	        return ResponseEntity.ok("공지사항이 삭제되었습니다.");
	    } else {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("공지사항 삭제에 실패했습니다.");
	    }
	}
}
