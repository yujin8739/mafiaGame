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
import com.mafia.game.shop.model.service.ShopService;


import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/mypage")
public class MemberController {

	@Autowired
	private MemberService ms;
	

	
	@Autowired
    private BCryptPasswordEncoder bcrypt;
	
	@GetMapping("")
    public String myPage(HttpSession session, Model model,ShopService shopService) {
        Member loginUser = (Member) session.getAttribute("loginUser");
        
        if (loginUser == null) {
            return "redirect:/login/view";
        }
//        List<Shop> myItems = shopService.findByBuyer(loginUser.getUserName());
        
        // 최신 정보 조회
        Member member = ms.getMemberByUserName(loginUser.getUserName());
        model.addAttribute("member", member);
//        model.addAttribute("myItems", myItems);
        return "member/myPage";
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



	@GetMapping("/delete")
	public String deleteAccountPage(HttpSession session, Model model) {
		Member loginUser = (Member) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login/view";
		}

		model.addAttribute("loginUser", loginUser);
		return "member/deleteAccount";
	}

	@PostMapping("/delete")
	public String deleteAccount(@RequestParam String userName, @RequestParam String password,
			@RequestParam(required = false) String reason, @RequestParam(required = false) String reasonDetail,
			HttpSession session, RedirectAttributes redirectAttributes) {

		Member loginUser = (Member) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login/view";
		}

		// 현재 로그인한 사용자와 요청한 사용자가 같은지 확인
		if (!loginUser.getUserName().equals(userName)) {
			redirectAttributes.addFlashAttribute("error", "잘못된 접근입니다.");
			return "redirect:/mypage";
		}

		// 비밀번호 확인
		if (!bcrypt.matches(password, loginUser.getPassword())) {
			redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
			return "redirect:/mypage/delete";
		}

		try {
			// 회원 탈퇴 처리
			int result = ms.deleteMember(userName);

			if (result > 0) {
				// 세션 무효화
				session.invalidate();
				redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.");
				return "redirect:/";
			} else {
				redirectAttributes.addFlashAttribute("error", "회원 탈퇴 처리 중 오류가 발생했습니다.");
				return "redirect:/mypage/delete";
			}

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "회원 탈퇴 처리 중 오류가 발생했습니다.");
			return "redirect:/mypage/delete";
		}
	}

	
	
}


