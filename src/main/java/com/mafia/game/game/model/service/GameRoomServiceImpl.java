package com.mafia.game.game.model.service;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.game.model.dao.GameRoomDao;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Vote;
import com.mafia.game.job.model.vo.Job;



@Service
@Transactional
public class GameRoomServiceImpl implements GameRoomService {

	@Autowired
	private SqlSessionTemplate sqlSession;
	
    @Autowired
    private GameRoomDao gameRoomDao;

    @Override
    public int createRoom(GameRoom room) {
        return gameRoomDao.insertRoom(sqlSession,room);
    }

	@Override
	public GameRoom selectRoom(int roomNo) {
		return gameRoomDao.selectRoom(sqlSession, roomNo);
	}

	@Override
	public int updateUserList(int roomNo, String updatedList) {
		return gameRoomDao.updateUserList(sqlSession, roomNo, updatedList);
	}

	@Override
	public int deleteRoom(int roomNo) {
		return gameRoomDao.deleteRoom(sqlSession, roomNo);
	}

    @Override
    public List<GameRoom> getAllRooms() {
        return gameRoomDao.selectAllRooms(sqlSession);
    }

	@Override
	public int updateReadyList(int roomNo, String updatedList) {
		return gameRoomDao.updateReadyList(sqlSession, roomNo, updatedList);
	}

	@Override
	public String getReadyCount(int roomNo) {
		return gameRoomDao.getReadyCount(sqlSession, roomNo);
	}

	@Override
	public int updateRoomMaster(int roomNo, String userName) {
		return gameRoomDao.updateRoomMaster(sqlSession, roomNo, userName);
	}

	@Override
	public int updateStart(int roomNo, String updatedJob) {
		return gameRoomDao.updateStart(sqlSession, roomNo, updatedJob);
	}

	@Override
	public List<Job> selectRandomJobs(int mafiaCount, int citizenCount, int neutralCount) {
		return gameRoomDao.selectRandomJobs(sqlSession, mafiaCount,citizenCount, neutralCount);
	}

	@Override
	public Map<String, Object> getRoomJob(int roomNo, String userName) {
		return gameRoomDao.getRoomJob(sqlSession,roomNo,userName);
	}

	@Override
	public Job getJobDetail(int myJob) {
		return gameRoomDao.getJobDetail(sqlSession,myJob);
	}

	@Override
	public List<String> getDeathList(int roomNo) {
		// TODO Auto-generated method stub
		return gameRoomDao.getDeathList(sqlSession, roomNo);
	}

	@Override
	public void updateVote(int roomNo, int dayNo, String updatedList) {
		gameRoomDao.updateVote(sqlSession, roomNo, dayNo, updatedList);
	}

	@Override
	public Vote selectVote(int roomNo, int dayNo) {
		return gameRoomDao.selectVote(sqlSession, roomNo, dayNo);
	}

	@Override
	public void updateDayNo(int roomNo, int dayNo) {
		gameRoomDao.updateDayNo(sqlSession, roomNo, dayNo);
		
	}
}