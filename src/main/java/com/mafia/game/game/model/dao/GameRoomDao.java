package com.mafia.game.game.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.job.model.vo.Job;

@Repository
public class GameRoomDao {

    public int insertRoom(SqlSessionTemplate sqlSession,GameRoom room) {
        return sqlSession.insert("gameRoomMapper.insertRoom", room);
    }

    public int deleteRoom(SqlSessionTemplate sqlSession, int roomNo) {
        return sqlSession.delete("gameRoomMapper.deleteRoom", roomNo);
    }

    public GameRoom selectRoom(SqlSessionTemplate sqlSession, int roomNo) {
        return sqlSession.selectOne("gameRoomMapper.selectRoom", roomNo);
    }

    public int updateUserList(SqlSessionTemplate sqlSession, int roomNo, String updatedList) {
        Map<String, Object> param = new HashMap<>();
        param.put("roomNo", roomNo);
        param.put("userList", updatedList);
        return sqlSession.update("gameRoomMapper.updateUserList", param);
    }

    public List<GameRoom> selectAllRooms(SqlSessionTemplate sqlSession) {
    	return sqlSession.selectList("gameRoomMapper.selectAllRooms");
    }

	public int updateReadyList(SqlSessionTemplate sqlSession, int roomNo, String updatedList) {
		Map<String, Object> param = new HashMap<>();
        param.put("roomNo", roomNo);
        param.put("userList", updatedList);
        return sqlSession.update("gameRoomMapper.updateReadyList", param);
	}

	public String getReadyCount(SqlSessionTemplate sqlSession, int roomNo) {
		return sqlSession.selectOne("gameRoomMapper.getReadyCount",roomNo);
	}

	public int updateRoomMaster(SqlSessionTemplate sqlSession, int roomNo, String userName) {
		Map<String, Object> param = new HashMap<>();
		param.put("roomNo",roomNo);
		param.put("userName", userName);
		return sqlSession.update("gameRoomMapper.updateRoomMaster",param);
	}

	public int updateStart(SqlSessionTemplate sqlSession, int roomNo, String updatedJob) {
		Map<String, Object> param = new HashMap<>();
		param.put("roomNo",roomNo);
		param.put("updatedJob",updatedJob);
		return sqlSession.update("gameRoomMapper.updateStart", param);
	}

	public List<Job> selectRandomJobs(SqlSessionTemplate sqlSession, int mafiaCount, int citizenCount, int neutralCount) {
		Map<String, Object> param = new HashMap<>();
		param.put("mafiaCount", mafiaCount);
		param.put("citizenCount", citizenCount);
		param.put("neutralCount", neutralCount);
		return sqlSession.selectList("gameRoomMapper.selectRandomJobs",param);
	}

	public Map<String, Object> getRoomJob(SqlSessionTemplate sqlSession, int roomNo, String userName) {
		Map<String, Object> param = new HashMap<>();
		param.put("userName", userName);
		param.put("roomNo", roomNo);
		return sqlSession.selectOne("gameRoomMapper.getRoomJob",param);
	}

	public Job getJobDetail(SqlSessionTemplate sqlSession, int myJob) {
		return sqlSession.selectOne("gameRoomMapper.getJobDetail",myJob);
	}
}
