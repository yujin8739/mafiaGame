package com.mafia.game.friend.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FriendRelation {
    private int relationNo;             // 관계번호 (PK)
    private String requesterName;       // 요청한 사용자
    private String receiverName;        // 요청받은 사용자
    private String receiverStatus;      // 상태 (PENDING/ACCEPTED/REJECTED)
    private Date requestDate;           // 요청날짜
    private Date responseDate;          // 응답날짜
    private String requesterNickName;   // 요청한 사용자의 닉네임
    private String receiverNickName;    // 요청받은 사용자의 닉네임
}
