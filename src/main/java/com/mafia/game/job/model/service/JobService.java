package com.mafia.game.job.model.service;

import java.util.List;

public interface JobService {

	List<String> essentialJob8();

	List<String> optionalJob8();

	List<String> neutralJob8();

	String playerList(int roomNo);

	String userNickName(String userName);

}
