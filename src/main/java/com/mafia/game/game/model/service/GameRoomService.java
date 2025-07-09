package com.mafia.game.game.model.service;

import java.util.List;

import com.mafia.game.game.model.vo.GameRoom;

public interface GameRoomService {
	
    int createRoom(GameRoom room);

	GameRoom selectRoom(int roomNo);

	int updateUserList(int roomNo, String updatedList);

	int deleteRoom(int roomNo);
	
	List<GameRoom> getAllRooms();

	int updateReadyList(int roomNo, String updatedList);

	String getReadyCount(int roomNo);

	int updateRoomMaster(int roomNo, String string);
}
