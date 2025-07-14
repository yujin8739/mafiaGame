package com.mafia.game.game.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Vote {
	private int roomNo;//	ROOMNO	NUMBER 방번호
	private int dayNo;//	DAYNO	NUMBER 일자
	private String vote;//	VOTE	CLOB 투표 (베열)
}
