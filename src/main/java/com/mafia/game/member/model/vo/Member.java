package com.mafia.game.member.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Member {
	private String userName;
	private String password;
	private String nickName;
	private String email;
	private char status;
}
