package com.mafia.game.message.model.service;

import java.util.ArrayList;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mafia.game.message.model.dao.MessageDao;
import com.mafia.game.message.model.vo.UserMessage;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private SqlSessionTemplate sqlSession;

    @Autowired
    private MessageDao messageDao;

    @Override
    public int sendMessage(UserMessage message) {
        return messageDao.sendMessage(sqlSession, message);
    }

    @Override
    public ArrayList<UserMessage> getReceivedMessages(String receiverUserName) {
        return messageDao.getReceivedMessages(sqlSession, receiverUserName);
    }

    @Override
    public ArrayList<UserMessage> getSentMessages(String senderUserName) {
        return messageDao.getSentMessages(sqlSession, senderUserName);
    }

    @Override
    public UserMessage getMessageDetail(int privateMsgNo) {
        return messageDao.getMessageDetail(sqlSession, privateMsgNo);
    }

    @Override
    public int markAsRead(int privateMsgNo, String receiverUserName) {
        return messageDao.markAsRead(sqlSession, privateMsgNo, receiverUserName);
    }

    @Override
    public int deleteBySender(int privateMsgNo) {
        return messageDao.deleteBySender(sqlSession, privateMsgNo);
    }

    @Override
    public int deleteByReceiver(int privateMsgNo) {
        return messageDao.deleteByReceiver(sqlSession, privateMsgNo);
    }

    @Override
    public int getUnreadCount(String receiverUserName) {
        return messageDao.getUnreadCount(sqlSession, receiverUserName);
    }
    
    @Override
    public ArrayList<UserMessage> getReceivedMessagesWithPaging(String receiverUserName, int offset, int pageSize) {
        return messageDao.getReceivedMessagesWithPaging(sqlSession, receiverUserName, offset, pageSize);
    }

    @Override
    public ArrayList<UserMessage> getSentMessagesWithPaging(String senderUserName, int offset, int pageSize) {
        return messageDao.getSentMessagesWithPaging(sqlSession, senderUserName, offset, pageSize);
    }

    @Override
    public int getTotalReceivedMessagesCount(String receiverUserName) {
        return messageDao.getTotalReceivedMessagesCount(sqlSession, receiverUserName);
    }

    @Override
    public int getTotalSentMessagesCount(String senderUserName) {
        return messageDao.getTotalSentMessagesCount(sqlSession, senderUserName);
    }
}