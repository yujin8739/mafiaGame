
package com.mafia.game.system.cleaner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.mafia.game.game.model.service.GameRoomService;

//게임 서버 실행시 모든 게임룸 삭제

@Component
public class GameRoomCleaner {

	@Autowired
	private GameRoomService gameRoomService;

	@EventListener(ApplicationReadyEvent.class)
	public void clearOnStartup() {
		gameRoomService.deleteAllGameRooms();
		System.out.println(">>> 서버 시작: GAME_ROOM 데이터 초기화 완료");
	}
}
