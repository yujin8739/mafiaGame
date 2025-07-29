package com.mafia.game.board.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FileDownloadDTO {
	private BoardFile file;
	private String typeName;
	private String changeName;
}
