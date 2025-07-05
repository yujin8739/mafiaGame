package com.mafia.game.game.model.service;

import java.util.List;

import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;

public interface ChatService {

	void sendMessage(Message message);

	List<Message> getMessages(int roonNo);

	void createRoom(GameRoom room);

}
