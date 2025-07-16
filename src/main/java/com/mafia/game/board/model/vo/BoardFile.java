package com.mafia.game.board.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BoardFile {
	private int fileNo;//	FILENO	NUMBER(11,0)
	private int boardNo;//	BOARDNO	NUMBER(11,0)
	private String type;//	TYPE	VARCHAR2(10 BYTE)
	private String originName;//	ORIGINNAME	VARCHAR2(255 BYTE)
	private Date uploadDate;//	UPLOADDATE	DATE
	private String status;//	status	CHAR(1 BYTE)
	private String changeName;//	CHANGENAME	VARCHAR2(255 BYTE)
	private int fileLevel;//	FILELEVEL	NUMBER(1,0)
	
	private String title;
	private String detail;
	private Date createDate;
	private String userName;
	private String nickName;
	private int viewCount;
	private int likeCount;
	private int dislikeCount;
	private int replyCount;
	private int rankPoint;
	private String profileUrl;
}

