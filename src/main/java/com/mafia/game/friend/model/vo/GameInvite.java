package com.mafia.game.friend.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GameInvite {
    private int inviteNo;               // 초대번호 (PK)
    private String senderName;          // 초대한 사용자
    private String receiverName;        // 초대받은 사용자
    private String roomNo;              // 방번호
    private String inviteStatus;        // 초대상태 (PENDING/ACCEPTED/REJECTED)
    private Date createDate;            // 초대생성일시
    private Date responseDate;          // 응답날짜
    private String senderNickName;      // 초대한 사용자의 닉네임
    private String receiverNickName;    // 초대받은 사용자의 닉네임
    private String roomName;            // 방 이름
}
