package com.mafia.game.friend.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FriendList {
    private int friendNo;               // 친구관계 번호 (PK)
    private String userName;            // 유저이름 (요청한 사람) (FK)
    private String friendUserName;      // 친구가 될 유저이름
    private String status;              // 관계 상태 (PENDING/ACCEPTED/REJECTED)
    private Date requestDate;           // 친구요청시간
    private Date acceptDate;            // 친구수락시간
    private String userNickName;        // 요청한 사람의 닉네임
    private String friendNickName;      // 친구의 닉네임
}
