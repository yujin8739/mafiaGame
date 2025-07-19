package com.mafia.game.game.model.service;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.game.model.dao.RoomHintDao;
import com.mafia.game.game.model.vo.Hint;
import com.mafia.game.game.model.vo.RoomHint;

@Service
@Transactional
public class RoomHintServiceImpl implements RoomHintService {

	@Autowired
	private SqlSessionTemplate sqlSession;
	
	@Autowired
	private RoomHintDao roomHintDao;

	@Override
	public Hint selectRandomHintByJob(int jobNo, String userNick) {
		return roomHintDao.selectRandomHintByJob(sqlSession, jobNo, userNick);
	}

	@Override
	public int insertRoomHint(RoomHint roomHint) {
		return roomHintDao.insertRoomHint(sqlSession, roomHint);
	}
	
	@Override
	public List<Hint> selectRandomHintByJobs(List<Integer> jobNo, List<String> userNick) {
		return roomHintDao.selectRandomHintByJobs(sqlSession, jobNo, userNick);
	}

	@Override
	public int insertRoomHints(List<RoomHint> roomHint) {
		return roomHintDao.insertRoomHints(sqlSession, roomHint);
	}

	@Override
	public List<RoomHint> selectRoomHintList(int roomNo) {
		return roomHintDao.selectRoomHintList(sqlSession, roomNo);
	}
}
