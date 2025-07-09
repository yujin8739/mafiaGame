package com.mafia.game.game.model.dao;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.game.model.vo.GameRoom;

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
        Map<String, Object> param = new java.util.HashMap<>();
        param.put("roomNo", roomNo);
        param.put("userList", updatedList);
        return sqlSession.update("gameRoomMapper.updateUserList", param);
    }

    public List<GameRoom> selectAllRooms(SqlSessionTemplate sqlSession) {
    	return sqlSession.selectList("gameRoomMapper.selectAllRooms");
    }

	public int updateReadyList(SqlSessionTemplate sqlSession, int roomNo, String updatedList) {
		Map<String, Object> param = new java.util.HashMap<>();
        param.put("roomNo", roomNo);
        param.put("userList", updatedList);
        return sqlSession.update("gameRoomMapper.updateReadyList", param);
	}

	public String getReadyCount(SqlSessionTemplate sqlSession, int roomNo) {
		return sqlSession.selectOne("gameRoomMapper.getReadyCount",roomNo);
	}
}
