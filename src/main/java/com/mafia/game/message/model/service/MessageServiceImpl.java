package com.mafia.game.message.model.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Map;

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

    // ======================== 관리자용 새로운 메소드들 ========================
    
    @Override
    public ArrayList<UserMessage> getAllMessagesForAdmin(Map<String, Object> params) {
        return messageDao.getAllMessagesForAdmin(sqlSession, params);
    }

    @Override
    public int getTotalMessagesCount() {
        return messageDao.getTotalMessagesCount(sqlSession);
    }

    @Override
    public ArrayList<UserMessage> searchMessagesForAdmin(Map<String, Object> searchParams) {
        return messageDao.searchMessagesForAdmin(sqlSession, searchParams);
    }

    @Override
    public int getSearchMessagesCount(Map<String, Object> searchParams) {
        return messageDao.getSearchMessagesCount(sqlSession, searchParams);
    }

    @Override
    public int forceDeleteMessage(int privateMsgNo) {
        return messageDao.forceDeleteMessage(sqlSession, privateMsgNo);
    }

    @Override
    public int updateMessageByAdmin(UserMessage message) {
        return messageDao.updateMessageByAdmin(sqlSession, message);
    }

    @Override
    public int bulkDeleteMessages(ArrayList<Integer> messageIds) {
        return messageDao.bulkDeleteMessages(sqlSession, messageIds);
    }

    @Override
    public int blockUserMessage(String targetUserName, Integer blockDays, String reason, String adminUserName) {
        try {
            // 시스템 쪽지로 차단 정보 저장
            UserMessage blockMessage = new UserMessage();
            blockMessage.setSenderUserName("SYSTEM");
            blockMessage.setReceiverUserName(targetUserName);
            blockMessage.setTitle("쪽지 기능 제재");
            
            // 차단 종료일 계산
            Date endDate = null;
            if (blockDays != null && blockDays > 0) {
                long endTime = System.currentTimeMillis() + (blockDays * 24 * 60 * 60 * 1000L);
                endDate = new Date(endTime);
            }
            
            // 내용에 차단 정보 포함
            String content = String.format(
                "관리자: %s\n차단 사유: %s\n차단 기간: %s일\n차단 종료일: %s",
                adminUserName,
                reason != null ? reason : "관리자 판단",
                blockDays != null ? blockDays : "무기한",
                endDate != null ? endDate.toString() : "무기한"
            );
            
            blockMessage.setContent(content);
            blockMessage.setMessageType("BLOCK");
            blockMessage.setSendDate(new Date(System.currentTimeMillis()));
            
            return messageDao.sendMessage(sqlSession, blockMessage);
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public boolean isUserMessageBlocked(String userName) {
        try {
            // messageType='BLOCK'인 최신 쪽지 조회
            UserMessage blockMessage = messageDao.getLatestBlockMessage(sqlSession, userName);
            
            if (blockMessage == null) {
                return false;
            }
            
            // content에서 차단 종료일 파싱
            String content = blockMessage.getContent();
            if (content.contains("무기한")) {
                return true; // 무기한 차단
            }
            
            // 차단 종료일 확인 (간단한 구현)
            // 실제로는 더 정교한 파싱이 필요
            try {
                if (content.contains("차단 종료일:")) {
                    String[] parts = content.split("차단 종료일:");
                    if (parts.length > 1) {
                        String dateStr = parts[1].trim();
                        Date endDate = Date.valueOf(dateStr);
                        Date now = new Date(System.currentTimeMillis());
                        return now.before(endDate);
                    }
                }
            } catch (Exception e) {
                // 파싱 실패시 차단된 것으로 간주
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int restoreMessage(int privateMsgNo, String restoreType) {
        return messageDao.restoreMessage(sqlSession, privateMsgNo, restoreType);
    }

    @Override
    public int updateMessageStatus(int privateMsgNo, String readYn, String deleteSender, String deleteReceiver) {
        return messageDao.updateMessageStatus(sqlSession, privateMsgNo, readYn, deleteSender, deleteReceiver);
    }
}