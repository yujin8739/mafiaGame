package com.mafia.game.game.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Kill {
	private int roomNo;//	ROOMNO	NUMBER 방번호
	private int dayNo;//	DAYNO	NUMBER 일자
	private String vote;//	KILL	CLOB 투표 (베열)
	private String killUser;//	KILLUSER	VARCHAR2(255 BYTE)
	private String healUser;//	HEALUSER	VARCHAR2(255 BYTE)
}
