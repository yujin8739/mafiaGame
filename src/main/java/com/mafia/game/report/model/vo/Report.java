package com.mafia.game.report.model.vo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Report {
	private int reportid; //신고번호
	private String reporterId; //신고자
	private String reportedName; //신고대상
	private String reason; //신고사유
	private Date reportedAt; //신고시간
	private String reportedNickName; //신고대상 닉네임
}
