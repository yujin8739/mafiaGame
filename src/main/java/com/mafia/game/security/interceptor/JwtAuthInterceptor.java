package com.mafia.game.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.security.config.JwtUtil;


@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MemberService memberService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        // Authorization 헤더가 없으면 통과시키되, 보안이 필요한 API라면 컨트롤러에서 체크
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.getWriter().write("{\"success\":false,\"message\":\"토큰이 없습니다.\"}");
            return false;
        }

        try {
            // Bearer 접두사 제거 후 토큰만 추출
            String token = authHeader.substring(7);

            // 토큰 유효성 검증
            if (!jwtUtil.vaildateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"message\":\"유효하지 않은 토큰입니다.\"}");
                return false;
            }

            // 토큰에서 userId 추출
            String userName = jwtUtil.getUserIdNameFromToken(token);

            // DB에서 유저 조회 (선택적)
            Member loginUser = memberService.loginDo(userName);
            if (loginUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"message\":\"사용자를 찾을 수 없습니다.\"}");
                return false;
            }

            // 유저 정보를 request attribute에 담아서 컨트롤러에서 꺼내쓸 수 있게 하기
            request.setAttribute("loginUser", loginUser);
            return true; // 컨트롤러 실행

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"message\":\"토큰 검증 중 오류가 발생했습니다.\"}");
            return false;
        }
    }
}
