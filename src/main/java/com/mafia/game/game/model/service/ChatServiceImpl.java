package com.mafia.game.game.model.service;

import java.util.List;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.game.model.dao.ChatDao;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

	@Autowired
    private ChatDao chatDao;
	
	@Autowired
    private SqlSessionTemplate sqlSession;


    @Override
    public void sendMessage(Message message) {
    	chatDao.insertMessage(sqlSession, message);
    }

    @Override
    public List<Message> getMessages(int roomNo, RowBounds rowBounds) {
        return chatDao.selectMessagesByRoom(sqlSession, roomNo, rowBounds);
    }

    @Override
    public void createRoom(GameRoom room) {
    	chatDao.insertRoom(sqlSession, room);
    }

	@Override
	public String selectEvent(int eventNo, String userName) {
		return chatDao.selectEvent(sqlSession, eventNo, userName);
	}

	@Override
	public List<Message> getMessages(int roomNo, String type, RowBounds rowBounds) {
		return chatDao.selectMessagesByRoom(sqlSession, roomNo, type, rowBounds);
	}
}
