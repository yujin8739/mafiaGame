package com.mafia.game.job.model.service;

import java.util.ArrayList;
import java.util.List;

import com.mafia.game.job.model.vo.Player;

public interface JobService {

	List<String> essentialJob8();

	List<String> optionalJob8();

	List<String> neutralJob8();

	String playerList(int roomNo);

	String userNickName(String userName);

	Integer jobNo(String job);

	void playerInfo(int jobNo, String playerName, int roomNo);

	ArrayList<Player> player(int roomNo);

}
