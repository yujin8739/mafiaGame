package com.mafia.game.webSockert.model.vo;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Message {
	private int roomNo;//	방번호
	private String msgNo;//	채팅번호UUID
	private String type;//	visibleEvnet(보이는 이벤트),  event(일반 이벤트),  moring,(낮)  night(밤), die(사망자채팅), one(1대1)
	private String msg;//	채팅
	private String userName;//	유저아이디
	private Date chatDate;//	채팅일자
}
