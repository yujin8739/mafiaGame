package com.mafia.game.game.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Kill;
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

	public Map<String, Object> getRoomJob(SqlSessionTemplate sqlSession, int roomNo) {
		return sqlSession.selectOne("gameRoomMapper.getRoomJob",roomNo);
	}

	public Job getJobDetail(SqlSessionTemplate sqlSession, int myJob) {
		return sqlSession.selectOne("gameRoomMapper.getJobDetail",myJob);
	}
	
	public List<Job> getJobDetails(SqlSessionTemplate sqlSession, List<Integer> jobList){
		return sqlSession.selectList("gameRoomMapper.getJobDetails", Map.of("jobList", jobList));
	}

	public List<String> getDeathList(SqlSessionTemplate sqlSession, int roomNo) {
		return sqlSession.selectList("gameRoomMapper.getDeathList",roomNo);
	}
	
	public Kill selectKill(SqlSessionTemplate sqlSession, int roomNo, int dayNo) {
		Map<String, Object> param = new HashMap<>();
		param.put("roomNo", roomNo);
		param.put("dayNo", dayNo);
		
		return sqlSession.selectOne("gameRoomMapper.selectKill",param);
	}

	public void updateKill(SqlSessionTemplate sqlSession, Kill kill) {		
		sqlSession.update("gameRoomMapper.updateKill", kill);
	}

	public void updateDayNo(SqlSessionTemplate sqlSession, int roomNo, int dayNo) {
		Map<String, Object> param = new HashMap<>();
		param.put("roomNo", roomNo);
		param.put("dayNo", dayNo);
		sqlSession.update("gameRoomMapper.updateDayNo", param);
	}

	public void insertKill(SqlSessionTemplate sqlSession, Kill kill) {
		sqlSession.insert("gameRoomMapper.insertKill", kill);
	}

	public void updateJob(SqlSessionTemplate sqlSession, int roomNo, String updatedJobJson) {
		Map<String, Object> param = new HashMap<>();
		param.put("roomNo", roomNo);
		param.put("job", updatedJobJson);
		
        sqlSession.update("gameRoomMapper.updateJob", param);
	}

	public void updateStop(SqlSessionTemplate sqlSession, int roomNo) {
		sqlSession.update("gameRoomMapper.updateStop",roomNo);
	}

	public void deleteAllGameRooms(SqlSessionTemplate sqlSession) {
		sqlSession.delete("gameRoomMapper.deleteAllGameRooms");
	}
	
    /**
     * 페이징된 방 목록 조회
     */
    public List<GameRoom> selectRoomsPaged(SqlSessionTemplate sqlSession, int offset, int limit) {
        Map<String, Object> param = new HashMap<>();
        param.put("offset", offset);
        param.put("limit", limit);
        return sqlSession.selectList("gameRoomMapper.selectRoomsPaged", param);
    }
    
    /**
     * 전체 방 개수 조회
     */
    public int selectTotalRoomCount(SqlSessionTemplate sqlSession) {
        return sqlSession.selectOne("gameRoomMapper.selectTotalRoomCount");
    }
    
    /**
     * 필터링된 방 목록 조회 (검색 + 페이징)
     */
    public List<GameRoom> selectRoomsFiltered(SqlSessionTemplate sqlSession, Map<String, Object> searchParams) {
        return sqlSession.selectList("gameRoomMapper.selectRoomsFiltered", searchParams);
    }
    
    /**
     * 필터링된 방 개수 조회
     */
    public int selectFilteredRoomCount(SqlSessionTemplate sqlSession, Map<String, Object> searchParams) {
        return sqlSession.selectOne("gameRoomMapper.selectFilteredRoomCount", searchParams);
    }

	public int insertGameResult(SqlSessionTemplate sqlSession, Map<String, Object> gameResultMap) {
		return sqlSession.insert("gameRoomMapper.insertGameResult", gameResultMap);
	}

	public int getTotalRecodeCount(SqlSessionTemplate sqlSession, String userName) {
		return sqlSession.selectOne("gameRoomMapper.getTotalRecodeCount", userName);
	}

	public List<String> getRecodeList(SqlSessionTemplate sqlSession, PageInfo pi, String userName) {
		Map<String, Object> param = new HashMap<>();
		
		int limit = pi.getBoardLimit();
		int offset = (pi.getCurrentPage()-1) * limit;
		param.put("rowBounds", new RowBounds(offset, limit));
		param.put("userName", userName);
		return sqlSession.selectList("gameRoomMapper.getRecodeList", param);
	}
	
}
