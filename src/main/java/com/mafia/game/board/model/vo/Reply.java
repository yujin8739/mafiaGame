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
public class Reply {
	
	private int replyNo;//	REPLYNO	NUMBER(11,0)
	private int boardNo;//	BOARDNO	NUMBER(11,0)
	private int fileNo;//	FILENO	NUMBER(11,0)
	private String replyContent;//	REPLYCONTENT	VARCHAR2(255 BYTE)
	private Date createDate;//	CREATEDATE	DATE
	private String userName;//USERNAME	VARCHAR2(255 BYTE)
	private String status;//STATUS	VARCHAR2(1 BYTE)
	private int likeCount;//LIKECOUNT	NUMBER
	private String changeName;
	private String nickName;
	private int rankPoint;
	private String profileUrl;
}
