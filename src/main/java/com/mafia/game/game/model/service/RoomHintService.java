package com.mafia.game.game.model.service;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;

import com.mafia.game.game.model.vo.Hint;
import com.mafia.game.game.model.vo.RoomHint;

public interface RoomHintService {
	List<Hint> selectRandomHintByJobs(List<Integer> jobNo, List<String> userNick);
	int insertRoomHints(List<RoomHint> roomHint);
	Hint selectRandomHintByJob(int jobNo, String userNick);
	int insertRoomHint(RoomHint roomHint);
	List<RoomHint> selectRoomHintList(int roomNo);
}
