package com.mafia.game.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	
}
