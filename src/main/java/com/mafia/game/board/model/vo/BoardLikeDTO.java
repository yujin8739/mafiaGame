package com.mafia.game.board.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class BoardLikeDTO {
	
	private int boardNo;
	private String type;
	private String userName;
}
