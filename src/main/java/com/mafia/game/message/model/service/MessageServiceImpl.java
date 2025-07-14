package com.mafia.game.message.model.service;

import java.util.ArrayList;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.message.model.dao.MessageDao;
import com.mafia.game.message.model.vo.UserMessage;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    @Autowired
    private SqlSessionTemplate sqlSession;

    @Autowired
    private MessageDao dao;

    @Override
    public int sendMessage(UserMessage message) {
        // 쪽지 번호 자동 생성
        int privateMsgNo = dao.getNextMessageNo(sqlSession);
        message.setPrivateMsgNo(privateMsgNo);
        
        return dao.sendMessage(sqlSession, message);
    }

    @Override
    public ArrayList<UserMessage> getReceivedMessages(String receiverUserName) {
        return dao.getReceivedMessages(sqlSession, receiverUserName);
    }

    @Override
    public ArrayList<UserMessage> getSentMessages(String senderUserName) {
        return dao.getSentMessages(sqlSession, senderUserName);
    }

    @Override
    public UserMessage getMessageDetail(int privateMsgNo) {
        return dao.getMessageDetail(sqlSession, privateMsgNo);
    }

    @Override
    public int markAsRead(int privateMsgNo, String receiverUserName) {
        return dao.markAsRead(sqlSession, privateMsgNo, receiverUserName);
    }

    @Override
    public int deleteBySender(int privateMsgNo) {
        return dao.deleteBySender(sqlSession, privateMsgNo);
    }

    @Override
    public int deleteByReceiver(int privateMsgNo) {
        return dao.deleteByReceiver(sqlSession, privateMsgNo);
    }

    @Override
    public int getUnreadCount(String receiverUserName) {
        return dao.getUnreadCount(sqlSession, receiverUserName);
    }
}