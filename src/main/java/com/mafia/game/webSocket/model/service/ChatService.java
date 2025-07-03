package com.mafia.game.webSocket.model.service;

import java.util.List;

import com.mafia.game.webSocket.model.vo.GameRoom;
import com.mafia.game.webSocket.model.vo.Message;

public interface ChatService {

	List<GameRoom> getAllRooms();

	GameRoom getRoom(int roonNo);

	void sendMessage(Message message);

	List<Message> getMessages(int roonNo);

	void createRoom(GameRoom room);

}
