package com.mafia.game.game.model.service;

import java.util.List;

import org.apache.ibatis.session.RowBounds;

import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;

public interface ChatService {

	void sendMessage(Message message);

	List<Message> getMessages(int roonNo, RowBounds rowBounds);

	List<Message> getMessages(int roomNo, String type, RowBounds rowBounds);

	void createRoom(GameRoom room);
	
	String selectEvent(int eventNo, String userName);


}
