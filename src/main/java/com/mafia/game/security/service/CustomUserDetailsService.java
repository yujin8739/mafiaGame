package com.mafia.game.security.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.security.auth.CustomUserDetails;
import com.mafia.game.system.exception.EmailNotVerifiedException;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private MemberService memberService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberService.loginDo(username);
        
        if (member == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        if ('Y'!=member.getStatus()) {
            throw new EmailNotVerifiedException("Email not verified",member); // 커스텀 실패 처리에서 분기할 수 있도록 예외 처리
        }
        
        return new CustomUserDetails(member);
    }

}
