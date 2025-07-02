package com.mafia.game.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/mypage")
public class MemberController {

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
	
	@PostMapping("/update")
	public String updateMember(Member m, 
							@RequestParam String beforeEmail,  
							@RequestParam String beforePassword, 
							@RequestParam String hiddenPassword,
							RedirectAttributes redirectAttributes,
							HttpSession session) {
		
		if(bcrypt.matches(beforePassword, hiddenPassword)) {
			if(!beforeEmail.equals(m.getEmail())) {
				m.setStatus('N');
				session.invalidate();
				redirectAttributes.addFlashAttribute("msg", "이메일이 변경 되었습니다. \n재 로그인 후 메일 인증을 진행해 주세요");
			} else {
				m.setStatus('Y');
			}
			
			if(m.getPassword() == null || m.getPassword().isEmpty()) {
				m.setPassword(beforePassword);
			} else {
				session.invalidate();
			}
			m.setPassword(bcrypt.encode(m.getPassword()));
			ms.updateMember(m);
			return "redirect:/";
		} 
		
		redirectAttributes.addFlashAttribute("msg", "이전 비밀번호가 일치하지 않습니다.");
		
		return "redirect:/mypage";
	}
}