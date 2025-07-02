package com.mafia.game.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/mypage")
public class memberController {

	@Autowired
	private MemberService ms;
	
	@Autowired
    private BCryptPasswordEncoder bcrypt;
	
	@GetMapping("")
    public String myPage(HttpSession session, Model model) {
        Member loginUser = (Member) session.getAttribute("loginUser");
        
        if (loginUser == null) {
            return "redirect:/login/view";
        }
        
        // 최신 정보 조회
        Member member = ms.getMemberByUserName(loginUser.getUserName());
        model.addAttribute("member", member);
        return "member/mypage";
    }
}