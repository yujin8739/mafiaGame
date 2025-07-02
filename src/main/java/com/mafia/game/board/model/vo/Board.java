package com.mafia.game.board.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Board {
	
	private int boardNo;//	BOARDNO	NUMBER(11,0)
	private String userName;//	USERNAME	VARCHAR2(255 BYTE)
	private String title;//	TITLE	VARCHAR2(255 BYTE)
	private String detail;//	DETAIL	CLOB
	private String type;//	TYPE	VARCHAR2(10 BYTE)
	
	
}
