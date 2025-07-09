package com.mafia.game.board.model.vo;

import java.sql.Date;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Board { //일반 게시글, 사진 게시글, 동영상 게시글
	
	private int boardNo;//	BOARDNO	NUMBER(11,0)
	private String userName;//	USERNAME	VARCHAR2(255 BYTE)
	private int typeNo;//	TYPENO	NUMBER
	private String title;//	TITLE	VARCHAR2(255 BYTE)
	private String detail;//	DETAIL	CLOB
	private String status;//	STATUS	VARCHAR2(1 BYTE)
	private Date createDate;//	CREATEDATE	DATE
	private int viewCount;//	VIEWCOUNT	NUMBER
	private String typeName; // 게시판 타입 db명
	private String displayName; //사용자에게 보여지는 게시판 타입명
	
	private String nickName;
	private String changeName;
	
	//[Collection용] 첨부파일 담을 변수
	private ArrayList<BoardFile> fileList;
	private ArrayList<Reply> replyList;
	
}
