package com.mafia.game.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.report.model.service.ReportService;
import com.mafia.game.report.model.vo.Report;
import com.mafia.game.security.config.JwtUtil;
import com.mafia.game.shop.model.service.ShopService;
import com.mafia.game.shop.model.vo.Shop;

@RestController
@RequestMapping("/api") 
public class ApiController {
	
	@Autowired
	private MemberService mService;
	
	@Autowired 
	private GameRoomService gService;
	
	@Autowired
	private ReportService rService;
	
	@Autowired
	private BCryptPasswordEncoder bcrypt;

	@Autowired
	private JwtUtil jwtUtil;
	

    @Autowired
    private ShopService shopService;
	
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
	
	@GetMapping("/gameroom/list")
	public ResponseEntity<Map<String, Object>> gameRoomList (@RequestParam int offset, @RequestParam int limit) {
		Map<String, Object> response = new HashMap<>();
		try {
			List<GameRoom> gameRooms = gService.getRoomsPaged(offset, limit);
			int totalCount = gService.getTotalRoomCount();
			response.put("gameRooms", gameRooms);
			response.put("totalCount", totalCount);
			response.put("success", true);
			response.put("message", "성공");
		} catch (Exception e) {
			response.put("error", e);
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}

    // 아트샵 게시글 목록 조회 (페이징 처리 없이 전체 데이터 반환)
    @GetMapping("/artShop/artList")
    public ResponseEntity<Map<String, Object>> artList(@RequestParam int offset, @RequestParam int limit) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 전체 게시글 목록 조회 
            List<Shop> shopList = shopService.selectAllArtworks(offset, limit);
            int totalCount = shopService.getListCount();
            response.put("artList", shopList);
            response.put("totalCount", totalCount);
            response.put("success", true);
            response.put("message", "성공");
        } catch (Exception e) {
            response.put("error", e);
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/artDelete")
    public ResponseEntity<?> deleteArt(@RequestBody Map<String, Integer> data) {
        Integer artId = data.get("artId");

        if (artId == null) {
            return ResponseEntity.badRequest().body("artId 누락");
        }

        try {
            Shop shop = shopService.selectArtworkById(artId);

            if (shop == null) {
                return ResponseEntity.status(404).body("작품을 찾을 수 없습니다.");
            }

            // 실제 이미지 파일 삭제 (선택)
            String imagePath = shop.getImagePath(); // DB에 저장된 경로
            if (imagePath != null) {
                File file = new File("C:" + imagePath); // 윈도우 기준. 리눅스면 경로 조정
                if (file.exists()) file.delete();
            }

            int result = shopService.deleteArt(artId);

            if (result > 0) {
                return ResponseEntity.ok("삭제 성공");
            } else {
                return ResponseEntity.status(500).body("DB 삭제 실패");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("삭제 처리 중 오류 발생");
        }
    }
	
	@GetMapping("/getReportList")
	public ResponseEntity<Map<String, Object>> getReportList (@RequestParam int offset, @RequestParam int limit) {
		Map<String, Object> response = new HashMap<>();
		try {
			List<Report> reports = rService.getReportList(offset, limit);
			int totalCount = rService.getReportCount();
			response.put("report", reports);
			response.put("totalCount", totalCount);
			response.put("success", true);
			response.put("message", "성공");
		} catch (Exception e) {
			response.put("error", e);
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/blockUser") 
	public ResponseEntity<Map<String, Object>> blockUser (@RequestParam String userName, @RequestParam int reportId) {
		Map<String, Object> response = new HashMap<>();
		try {
			int result = mService.blockUser(userName);
			
			rService.rejectReport(reportId);
			response.put("result", result);
			response.put("success", true);
			response.put("message", "성공");
		} catch (Exception e) {
			response.put("error", e);
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/rejectReport")
	public ResponseEntity<Map<String, Object>> rejectReport (@RequestParam int reportId) {
		Map<String, Object> response = new HashMap<>();
		try {
			int result = rService.rejectReport(reportId);
			response.put("result", result);
			response.put("success", true);
			response.put("message", "성공");
		} catch (Exception e) {
			response.put("error", e);
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}
}
