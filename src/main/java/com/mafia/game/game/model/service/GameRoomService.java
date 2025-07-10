package com.mafia.game.game.model.service;

import java.util.List;

import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.job.model.vo.Job;

public interface GameRoomService {
	
    int createRoom(GameRoom room);

	GameRoom selectRoom(int roomNo);

	int updateUserList(int roomNo, String updatedList);

	int deleteRoom(int roomNo);
	
	List<GameRoom> getAllRooms();

	int updateReadyList(int roomNo, String updatedList);

	String getReadyCount(int roomNo);

	int updateRoomMaster(int roomNo, String string);

	int updateStart(int roomNo, String updatedJob);
	
	List<Job> selectRandomJobs(int mafiaCount, int citizenCount, int neutralCount);
}
