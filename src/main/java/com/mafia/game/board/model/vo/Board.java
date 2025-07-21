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
	private String title;//	TITLE	VARCHAR2(255 BYTE)
	private String detail;//	DETAIL	CLOB
	private String status;//	STATUS	VARCHAR2(1 BYTE)
	private Date createDate;//	CREATEDATE	DATE
	private int viewCount;//	VIEWCOUNT	NUMBER
	private int likeCount;//	LIKECOUNT	NUMBER
	private int dislikeCount;//	DISLIKECOUNT	NUMBER
	private String typeName; // 게시판 타입 db명
	private int typeClass; // 게시판 분류용클래스 : 자유:1/플레이:2/직업:3/갤러리:4/비디오:5
	
	
	private String nickName;
	private String changeName;
	private int rankPoint; //글 작성자 랭크포인트
	private String profileUrl;//글 작성자 티어이미지 url
	private int replyCount; //리뷰 몇개인지
	private String hasFile; //첨부파일 있는지
	private boolean isNew; //오늘 올라온 게시글인지
	
	
	//[Collection용] 첨부파일 담을 변수
	private ArrayList<BoardFile> fileList;
	private ArrayList<Reply> replyList;
	
}
