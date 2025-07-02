package com.mafia.game.webSockert.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GameRoom {
	private int roomNo;//	방번호
	private int roomName;//	방제목
	private String userList;//유저리스트(JSONArray->String)
	private int headCount;//방 인원(해당 인원 되야 시작 가능)
	private String password;//방 비밀번호
	private String type;//방종류(랭크,일반,친선)
}
 