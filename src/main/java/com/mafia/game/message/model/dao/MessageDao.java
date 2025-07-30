package com.mafia.game.message.model.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.message.model.vo.UserMessage;

@Repository
public class MessageDao {

    // 쪽지 보내기
    public int sendMessage(SqlSessionTemplate sqlSession, UserMessage message) {
        return sqlSession.insert("messageMapper.sendMessage", message);
    }
    
    // 받은 쪽지함 조회
    public ArrayList<UserMessage> getReceivedMessages(SqlSessionTemplate sqlSession, String receiverUserName) {
        return new ArrayList<>(sqlSession.selectList("messageMapper.getReceivedMessages", receiverUserName));
    }
    
    // 보낸 쪽지함 조회
    public ArrayList<UserMessage> getSentMessages(SqlSessionTemplate sqlSession, String senderUserName) {     
        return new ArrayList<>(sqlSession.selectList("messageMapper.getSentMessages", senderUserName));
    }
    
    // 쪽지 상세 조회
    public UserMessage getMessageDetail(SqlSessionTemplate sqlSession, int privateMsgNo) {
        return sqlSession.selectOne("messageMapper.getMessageDetail", privateMsgNo);
    }
    
    // 쪽지 읽음 처리
    public int markAsRead(SqlSessionTemplate sqlSession, int privateMsgNo, String receiverUserName) {
        Map<String, Object> params = new HashMap<>();
        params.put("privateMsgNo", privateMsgNo);
        params.put("receiverUserName", receiverUserName);
        return sqlSession.update("messageMapper.markAsRead", params);
    }
    
    // 쪽지 삭제 (보낸사람용)
    public int deleteBySender(SqlSessionTemplate sqlSession, int privateMsgNo) {
        return sqlSession.update("messageMapper.deleteBySender", privateMsgNo);
    }
    
    // 쪽지 삭제 (받은사람용)
    public int deleteByReceiver(SqlSessionTemplate sqlSession, int privateMsgNo) {
        return sqlSession.update("messageMapper.deleteByReceiver", privateMsgNo);
    }
    
    // 안읽은 쪽지 개수 조회
    public int getUnreadCount(SqlSessionTemplate sqlSession, String receiverUserName) {
        return sqlSession.selectOne("messageMapper.getUnreadCount", receiverUserName);
    }
    
    public ArrayList<UserMessage> getReceivedMessagesWithPaging(SqlSessionTemplate sqlSession, String receiverUserName, int offset, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("receiverUserName", receiverUserName);
        params.put("offset", offset);
        params.put("pageSize", pageSize);
        return new ArrayList<>(sqlSession.selectList("messageMapper.getReceivedMessagesWithPaging", params));
    }

    // 보낸 쪽지함 페이지네이션 조회
    public ArrayList<UserMessage> getSentMessagesWithPaging(SqlSessionTemplate sqlSession, String senderUserName, int offset, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("senderUserName", senderUserName);
        params.put("offset", offset);
        params.put("pageSize", pageSize);
        return new ArrayList<>(sqlSession.selectList("messageMapper.getSentMessagesWithPaging", params));
    }

    // 받은 쪽지 전체 개수 조회
    public int getTotalReceivedMessagesCount(SqlSessionTemplate sqlSession, String receiverUserName) {
        return sqlSession.selectOne("messageMapper.getTotalReceivedMessagesCount", receiverUserName);
    }

    // 보낸 쪽지 전체 개수 조회
    public int getTotalSentMessagesCount(SqlSessionTemplate sqlSession, String senderUserName) {
        return sqlSession.selectOne("messageMapper.getTotalSentMessagesCount", senderUserName);
    }
    
    
    // =================================== 관리자용 새로운 메소드들만 ===================================
    
    
    // 관리자용 전체 쪽지 조회
    public ArrayList<UserMessage> getAllMessagesForAdmin(SqlSessionTemplate sqlSession, Map<String, Object> params) {
        return new ArrayList<>(sqlSession.selectList("messageMapper.getAllMessagesForAdmin", params));
    }
    
    // 관리자용 전체 쪽지 개수 
    public int getTotalMessagesCount(SqlSessionTemplate sqlSession) {
        return sqlSession.selectOne("messageMapper.getTotalMessagesCount");
    }
    
    // 관리자용 쪽지 검색    
    public ArrayList<UserMessage> searchMessagesForAdmin(SqlSessionTemplate sqlSession, Map<String, Object> searchParams) {
        return (ArrayList)sqlSession.selectList("messageMapper.searchMessagesForAdmin", searchParams);
    }

   
    // 관리자용 검색 결과 개수   
    public int getSearchMessagesCount(SqlSessionTemplate sqlSession, Map<String, Object> searchParams) {
        return sqlSession.selectOne("messageMapper.getSearchMessagesCount", searchParams);
    }

    // 관리자용 쪽지 강제 삭제 (완전 삭제)
    public int forceDeleteMessage(SqlSessionTemplate sqlSession, int privateMsgNo) {
        return sqlSession.delete("messageMapper.forceDeleteMessage", privateMsgNo);
    }
   
   // 관리자용 쪽지 수정
    public int updateMessageByAdmin(SqlSessionTemplate sqlSession, UserMessage message) {
        return sqlSession.update("messageMapper.updateMessageByAdmin", message);
    }
    
    // 관리자용 일괄 삭제    
    public int bulkDeleteMessages(SqlSessionTemplate sqlSession, ArrayList<Integer> messageIds) {
        return sqlSession.delete("messageMapper.bulkDeleteMessages", messageIds);
    }

    // 사용자 차단 메시지 조회
    public UserMessage getLatestBlockMessage(SqlSessionTemplate sqlSession, String userName) {
        return sqlSession.selectOne("messageMapper.getLatestBlockMessage", userName);
    }

    // 관리자용 쪽지 복구
    public int restoreMessage(SqlSessionTemplate sqlSession, int privateMsgNo, String restoreType) {
        Map<String, Object> params = Map.of("privateMsgNo", privateMsgNo, "restoreType", restoreType);
        return sqlSession.update("messageMapper.restoreMessage", params);
    }

    // 관리자용 쪽지 상태 변경
    public int updateMessageStatus(SqlSessionTemplate sqlSession, int privateMsgNo, String readYn, 
                                 String deleteSender, String deleteReceiver) {
        Map<String, Object> params = Map.of(
            "privateMsgNo", privateMsgNo,
            "readYn", readYn,
            "deleteSender", deleteSender,
            "deleteReceiver", deleteReceiver
        );
        return sqlSession.update("messageMapper.updateMessageStatus", params);
    }
    
}