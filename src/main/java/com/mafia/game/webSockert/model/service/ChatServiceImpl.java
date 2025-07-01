package com.mafia.game.webSockert.model.service;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.webSockert.model.dao.ChatDao;
import com.mafia.game.webSockert.model.vo.GameRoom;
import com.mafia.game.webSockert.model.vo.Message;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

	@Autowired
    private ChatDao chatDAO;
	@Autowired
    private SqlSessionTemplate sqlSession;

    @Override
    public List<GameRoom> getAllRooms() {
        return chatDAO.selectAllRooms(sqlSession);
    }

    @Override
    public GameRoom getRoom(int roonNo) {
        return chatDAO.selectRoomById(sqlSession, roonNo);
    }

    @Override
    public void sendMessage(Message message) {
        chatDAO.insertMessage(sqlSession, message);
    }

    @Override
    public List<Message> getMessages(int roonNo) {
        return chatDAO.selectMessagesByRoom(sqlSession, roonNo);
    }

    @Override
    public void createRoom(GameRoom room) {
        chatDAO.insertRoom(sqlSession, room);
    }
}
