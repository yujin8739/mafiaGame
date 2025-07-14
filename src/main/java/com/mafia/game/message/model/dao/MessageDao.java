package com.mafia.game.message.model.dao;

import java.util.ArrayList;
import java.util.HashMap;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.message.model.vo.UserMessage;

@Repository
public class MessageDao {

    /**
     * 쪽지 보내기
     */
    public int sendMessage(SqlSessionTemplate sqlSession, UserMessage message) {
        return sqlSession.insert("messageMapper.sendMessage", message);
    }
    
    /**
     * 받은 쪽지함 조회
     */
    public ArrayList<UserMessage> getReceivedMessages(SqlSessionTemplate sqlSession, String receiverUserName) {
        return new ArrayList<>(sqlSession.selectList("messageMapper.getReceivedMessages", receiverUserName));
    }
    
    /**
     * 보낸 쪽지함 조회
     */
    public ArrayList<UserMessage> getSentMessages(SqlSessionTemplate sqlSession, String senderUserName) {     
        return new ArrayList<>(sqlSession.selectList("messageMapper.getSentMessages", senderUserName));
    }
    
    /**
     * 쪽지 상세 조회

     */
    public UserMessage getMessageDetail(SqlSessionTemplate sqlSession, int privateMsgNo) {
        return sqlSession.selectOne("messageMapper.getMessageDetail", privateMsgNo);
    }
    
    /**
     * 쪽지 읽음 처리
     */
    public int markAsRead(SqlSessionTemplate sqlSession, int privateMsgNo, String receiverUserName) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("privateMsgNo", privateMsgNo);
        params.put("receiverUserName", receiverUserName);
        return sqlSession.update("messageMapper.markAsRead", params);
    }
    
    /**
     * 쪽지 삭제 (보낸사람용)
     */
    public int deleteBySender(SqlSessionTemplate sqlSession, int privateMsgNo) {
        return sqlSession.update("messageMapper.deleteBySender", privateMsgNo);
    }
    
    /**
     * 쪽지 삭제 (받은사람용)
     */
    public int deleteByReceiver(SqlSessionTemplate sqlSession, int privateMsgNo) {
        return sqlSession.update("messageMapper.deleteByReceiver", privateMsgNo);
    }
    
    /**
     * 안읽은 쪽지 개수 조회
     */
    public int getUnreadCount(SqlSessionTemplate sqlSession, String receiverUserName) {
        return sqlSession.selectOne("messageMapper.getUnreadCount", receiverUserName);
    }
    
    /**
     * 쪽지 번호 생성
     */
    public int getNextMessageNo(SqlSessionTemplate sqlSession) {
        return sqlSession.selectOne("messageMapper.getNextMessageNo");
    }
}