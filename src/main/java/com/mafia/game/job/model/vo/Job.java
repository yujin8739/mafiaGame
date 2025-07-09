package com.mafia.game.job.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Job {

	private int jobNo; //직업번호
	private int skillNo; //능력번호
	private String jobName; //직업명
	private int jobClass; //직업 소속(시민, 마피아, 중립)
	private String optional; //필수 직업 유무
	private String matchCode;
	private String probablity;
	private String jobVisible;
	
}
