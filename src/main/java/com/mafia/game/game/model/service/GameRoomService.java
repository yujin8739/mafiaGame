package com.mafia.game.game.model.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Kill;
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

	Map<String, Object> getRoomJob(int roomNo);

	Job getJobDetail(int myJob);

	List<String> getDeathList(int roomNo);

	void updateKill(Kill kill);

	Kill selectKill(int roomNo, int dayNo);

	void updateDayNo(int roomNo, int dayNo);

	void updateJob(int roomNo, String updatedJobJson);

	void insertKill(Kill kill);
	
	List<Job> getJobDetails(List<Integer> jobList);

	void updateStop(int roomNo);

	void deleteAllGameRooms();
	
	/**
	 * 페이징된 방 목록 조회
	 */
	List<GameRoom> getRoomsPaged(int offset, int limit);
	
	/**
	 * 전체 방 개수 조회
	 */
	int getTotalRoomCount();
	
	/**
	 * 필터링된 방 목록 조회 (검색 + 페이징)
	 */
	List<GameRoom> searchRooms(Map<String, Object> searchParams);
	
	/**
	 * 필터링된 방 개수 조회
	 */
	int getFilteredRoomCount(Map<String, Object> searchParams);

	int insertGameResult(Map<String, Object> gameResultMap);

	int getTotalRecodeCount(String userName);

	List<Map<String,Object>> getRecodeList(PageInfo pi, String userName);

	void deleteKillData(int roomNo);
	
	
	
}
