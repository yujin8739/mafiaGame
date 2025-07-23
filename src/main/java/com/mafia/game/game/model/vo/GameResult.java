package com.mafia.game.game.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GameResult {
	private int resultNo;//	RESULTNO	NUMBER
	private String userName;//	USERNAME	VARCHAR2(255 BYTE)
	private int jobNo;//	JOBNO	NUMBER
	private String team;//	TEAM	VARCHAR2(20 BYTE)
	private String teamResult;//	TEAMRESULT	VARCHAR2(10 BYTE)
	private Date gameDate;//	GAMEDATE	DATE
}
