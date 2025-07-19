package com.mafia.game.game.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.game.model.vo.Hint;
import com.mafia.game.game.model.vo.RoomHint;

@Repository
public class RoomHintDao {
	
	public List<Hint> selectRandomHintByJobs(SqlSessionTemplate sqlSession, List<Integer> jobNo, List<String> userNick) {
		Map<String, Object> param = new HashMap<>();
		param.put("jobNo", jobNo);
		param.put("userNick", userNick);
		return sqlSession.selectList("gameRoomMapper.selectRandomHintByJobs", param);
	}
	
	public int insertRoomHints(SqlSessionTemplate sqlSession, List<RoomHint> roomHint) {
		return sqlSession.insert("gameRoomMapper.insertRoomHints", roomHint);
	}
	
	public Hint selectRandomHintByJob(SqlSessionTemplate sqlSession, int jobNo, String userNick) {
		Map<String, Object> param = new HashMap<>();
		param.put("jobNo", jobNo);
		param.put("userNick", userNick);
		return sqlSession.selectOne("gameRoomMapper.selectRandomHintByJob", param);
	}
	
	public int insertRoomHint(SqlSessionTemplate sqlSession, RoomHint roomHint) {
		return sqlSession.insert("gameRoomMapper.insertRoomHint", roomHint);
	}

	public List<RoomHint> selectRoomHintList(SqlSessionTemplate sqlSession, int roomNo) {
		return sqlSession.selectList("gameRoomMapper.selectRoomHintList", roomNo);
	}
}
