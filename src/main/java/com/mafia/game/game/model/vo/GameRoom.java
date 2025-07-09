package com.mafia.game.game.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GameRoom {
	private int roomNo;//	방번호
	private String roomName;//	방제목
	private String userList;//유저리스트(JSONArray->String)
	private int headCount;//방 인원(해당 인원 되야 시작 가능)
	private String password;//방 비밀번호
	private String type;//방종류(랭크,일반,친선)
    private int currentUserCount; //현재 접속 인원
    private String isGaming;//게임중인지 표시
    private String readyUser;//게임 시작 준비중인 유저
    private String count; //커스텀모드 인원(마피아,시민,중립)
    private String master;//방장
}
 