package com.mafia.game.job.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Player {
	
	private int playerNo;
	private int jobNo;
	private int roomNo;
	private String userName;
	private String status;
	private String usable;
	
}
