package com.mafia.game.job.model.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.job.model.vo.Player;

@Repository
public class JobDao {

	public List<String> essentialJob8(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.essentialJob8");
	}

	public List<String> optionalJob8(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.optionalJob8");
	}

	public List<String> neutralJob8(SqlSessionTemplate sqlSession) {
		return sqlSession.selectList("jobMapper.neutralJob8");
	}

	public String playerList(SqlSessionTemplate sqlSession, int roomNo) {
		return sqlSession.selectOne("jobMapper.selectPlayer", roomNo);
	}

	public String userNickName(SqlSessionTemplate sqlSession, String userName) {
		return sqlSession.selectOne("jobMapper.selectNickName", userName);
	}

	public Integer jobNo(SqlSessionTemplate sqlSession, String job) {
		return sqlSession.selectOne("jobMapper.jobNo", job);
	}

	public void playerInfo(SqlSessionTemplate sqlSession, int jobNo, String playerName, int roomNo) {
		Map<String, Object> playerMap = new HashMap<>();
		playerMap.put("jobNo", jobNo);
		playerMap.put("playerName", playerName);
		playerMap.put("roomNo", roomNo);
		sqlSession.insert("jobMapper.playerInfo", playerMap);
	}

	public ArrayList<Player> player(SqlSessionTemplate sqlSession, int roomNo) {
		return (ArrayList)sqlSession.selectList("jobMapper.player", roomNo);
	}

}
