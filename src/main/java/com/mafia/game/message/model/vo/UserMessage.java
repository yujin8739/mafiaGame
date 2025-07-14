package com.mafia.game.message.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserMessage {
	
	private int privateMsgNo;           // PRIVATEMSGNO - 쪽지 번호 (PK)
    private String senderUserName;      // SENDERUSERNAME - 보내는 사람 아이디
    private String receiverUserName;    // RECEIVERUSERNAME - 받는 사람 아이디
    private String title;               // TITLE - 쪽지 제목
    private String content;             // CONTENT - 쪽지 내용
    private Date sendDate;              // SENDDATE - 보낸 시간
    private Date readDate;              // READDATE - 읽은 시간
    private char readYn;                // READYN - 읽음 여부 (Y/N)
    private char deleteSender;          // DELETESENDER - 보낸사람 삭제여부 (Y/N)
    private char deleteReceiver;        // DELETERECEIVER - 받은사람 삭제여부 (Y/N)
    private String messageType;         // MESSAGETYPE - 쪽지 타입
    private int parentPrivateMsgNo;     // PARENTPRIVATEMSGNO - 답장인 경우 원본 쪽지 번호
    private String senderNickName;      // 보내는 사람 닉네임 (USER_INFO 조인시 사용)
    private String receiverNickName;    // 받는 사람 닉네임 (USER_INFO 조인시 사용)
	
}
