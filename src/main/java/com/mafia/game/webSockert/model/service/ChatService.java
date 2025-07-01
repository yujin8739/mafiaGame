package com.mafia.game.webSockert.model.service;

import java.util.List;

import com.mafia.game.webSockert.model.vo.GameRoom;
import com.mafia.game.webSockert.model.vo.Message;

public interface ChatService {

	List<GameRoom> getAllRooms();

	GameRoom getRoom(int roonNo);

	void sendMessage(Message message);

	List<Message> getMessages(int roonNo);

	void createRoom(GameRoom room);

}
