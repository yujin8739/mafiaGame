package com.mafia.game.board.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Notice {

	private int noticeNo;
	private String userName;
	private String title;
	private String content;
	private Date createDate;
	private int count;
	private String status;
	
}
