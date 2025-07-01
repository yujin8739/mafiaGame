package com.mafia.game.security.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mafia.game.member.model.vo.Member;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    public Member getMember() {
        return member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한을 문자열로 가지는 경우 예: "ROLE_USER"
        return Collections.singleton(() -> "ROLE_USER");
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail(); // 로그인에 사용하는 식별자 (예: 이메일)
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부 (필요 시 member 필드에서 관리)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠김 여부
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 여부
    }

    @Override
    public boolean isEnabled() {
    	return member.getStatus() == 'Y';
    }
}
