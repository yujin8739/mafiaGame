package com.mafia.game.job.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Skill {

	private int skillNo; //능력번호
	private String skillType; //능력 타입(살해, 조사, 보호 등)
	private String used; //사용 유무
	
}
