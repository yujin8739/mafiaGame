package com.mafia.game.member.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mafia.game.common.model.service.MailService;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/login")
public class LoginController {
	
	@Autowired
	private MemberService mService;
	
	@Autowired
	private MailService eService;
	
	@Autowired
	private BCryptPasswordEncoder bcrypt;
	
	private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

	@GetMapping("/register")
	public String showRegister() {
		return "member/signUp";
	}
	
	
	@GetMapping("/view")
	public String showLogin() {
		return "member/login";
	}
	
	
    @RequestMapping(value = "/check-username", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Boolean> checkUsername(@RequestParam(name = "username")String username) {
        boolean isAvailable = mService.isUsernameAvailable(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);
        return response;
    }
    
    @RequestMapping(value = "/check-email", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam(name = "email")String username) {
        boolean isAvailable = mService.isEmailAvailable(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);
        return response;
    }
    
    // 회원가입 처리
    @PostMapping("/sign-up")
    public String signUp(Member member) {
    	String password = member.getPassword();
    	password = bcrypt.encode(password);
    	member.setPassword(password);
    	
        int result = mService.registerMember(member);
        return "redirect:/login/view"; // 가입 후 로그인 화면으로
    }
    
    @RequestMapping(value = "/sendEmail" , produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Boolean> sendEmail(@RequestParam("email") String email, 
    									 @RequestParam("userName") String userName, 
    									 @RequestParam("mailUUID") String mailUUID, 
    									 HttpServletRequest request) {  	
        String token = UUID.randomUUID().toString() + mailUUID;
        TokenInfo info = new TokenInfo(token, Instant.now().toEpochMilli(), request.getRemoteAddr());
        tokenStore.put(email + "$" + userName + mailUUID, info);
        
    	String verifyUrl = "http://localhost/login/verify?token="+token;
    	
    	String mailText = "<!DOCTYPE html>" +
                "<html><body>" +
                "<h3>[GOD FATHER 0805] 인증메일 입니다.</h3>" +
                "<p>이메일 인증을 위해 아래 버튼을 클릭해주세요.</p>" +
                "<a href='" + verifyUrl + "' " +
                "style='display:inline-block;padding:10px 20px;font-size:16px;color:white;" +
                "background-color:#28a745;text-decoration:none;border-radius:5px;'>이메일 인증하기</a>" +
                "<p style='margin-top:20px;'>감사합니다.</p>" +
                "<p>인증URL은 1시간 동안만 유효합니다.</p>" +
                "</body></html>";
    	
    	eService.sendEmail(email, "[GOD FATHER 0805] 회원가입을 위해 메일 인증을 완료해주세요", mailText);
		return null;
    }
    
    @PostMapping("/updateEmail")
    public void updateEmail(@RequestParam("email") String email, 
			 				@RequestParam("userName") String userName) {
    	mService.updateEmail(email,userName);
    }
    
    
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, Model model, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        // tokenStore는 Map<String, TokenInfo>
        Optional<Map.Entry<String, TokenInfo>> matchedEntry = tokenStore.entrySet().stream()
            .filter(entry -> token.equals(entry.getValue().token))
            .findFirst();

        if (matchedEntry.isEmpty()) {
            model.addAttribute("message", "유효하지 않은 인증 링크입니다.");
            return "member/verifyResult";
        }

        TokenInfo info = matchedEntry.get().getValue();
        
        // 이메일 추출
        String key = matchedEntry.get().getKey(); // email + userName + mailUUID
        Map<String, String> userInfo = extractEmailAndUserName(key); // 필요시 정제
        String email = userInfo.get("email");
        String userName = userInfo.get("userName");
        
        model.addAttribute("email", email);
        model.addAttribute("userName",userName);

        // IP 비교
        if (!info.clientKey.equals(clientIp)) {
            model.addAttribute("message", "IP 정보가 일치하지 않아 인증에 실패했습니다.\n인증을 요청한 네트워크 환경에서 토큰인증을 완료해주세요");
            return "member/verifyResult";
        }

        // 시간 제한 (예: 10분)
        long now = System.currentTimeMillis();
        if (now - info.createdAt > 60 * 60 * 1000) {
            model.addAttribute("message", "인증 링크가 만료되었습니다.");
            return "member/verifyResult";
        }

        // 예: 사용자 인증 처리
        int tResult = mService.allowMailToken(email);
        
        if(tResult>0) {
        	model.addAttribute("message", "이메일 인증이 완료되었습니다.");
        	 return "member/verifyResult"; // JSP나 HTML로 메시지 출력
        } else {
        	model.addAttribute("message", "이메일 인증에 오류가 발생했습니다. 이메일을 확인해주세요.");
        	 return "member/verifyResult"; // JSP나 HTML로 메시지 출력
        }
    }
    
    @GetMapping("/verifyResult")
    public String verifyResultPage(HttpSession session, Model model) {
        Member member = (Member) session.getAttribute("unauthenticatedMember");

        if (member != null) {
            model.addAttribute("userName", member.getUserName());
            model.addAttribute("email", member.getEmail());
            model.addAttribute("message", "이메일 인증이 완료되지 않았습니다."); // 기본 메시지
        } else {
            model.addAttribute("message", "잘못된 접근입니다.");
        }

        return "member/verifyResult";
    }

    
    
    /**
     * 이메일 토큰으로 인해 저장된 정보를 가져올때 토큰의 데이터를 원하는 형식에 맞게 가져오는 메소드
     * @param key
     * @return
     */
    private static Map<String, String> extractEmailAndUserName(String key) {
        // UUID는 고정 길이 36자 (예: 8-4-4-4-12)
        String uuid = key.substring(key.length() - 36);
        String prefix = key.substring(0, key.length() - 36);

        // 이메일 패턴 찾기
        Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,}");
        Matcher matcher = emailPattern.matcher(prefix);

        if (matcher.find()) {
            String email = matcher.group(); // 이메일
            String userName =  prefix.substring(matcher.group().length()+1); // 나머지가 유저 아이디

            Map<String, String> result = new HashMap<>();
            result.put("email", email);
            result.put("userName", userName);
            result.put("uuid", uuid);
            return result;
        }

        return null;
    }

    
    private static class TokenInfo {
        public String token;
        public long createdAt;
        public String clientKey;

        public TokenInfo(String token, long createdAt, String clientKey) {
            this.token = token;
            this.createdAt = createdAt;
            this.clientKey = clientKey;
        }
    }
}
