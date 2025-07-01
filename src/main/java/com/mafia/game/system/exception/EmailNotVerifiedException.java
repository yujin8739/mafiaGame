package com.mafia.game.system.exception;

import org.springframework.security.authentication.DisabledException;
import com.mafia.game.member.model.vo.Member;

/**
 * 로그인시  실패하면 DisableException을 이용하는데 
 * 이메일 인증확인 실패시에는 맴버를 전달하여 이메일 재전송 혹은 이메일 변경이 가능해야 하므로 상속받아 처리
 */
public class EmailNotVerifiedException extends DisabledException {
    private final Member member;

    public EmailNotVerifiedException(String msg, Member member) {
        super(msg);
        this.member = member;
    }

    public Member getMember() {
        return member;
    }
}