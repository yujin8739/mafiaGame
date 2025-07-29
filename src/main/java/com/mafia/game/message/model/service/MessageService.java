package com.mafia.game.message.model.service;

import java.util.ArrayList;
import java.util.Map;
import com.mafia.game.message.model.vo.UserMessage;

public interface MessageService {

	// 쪽지 보내기
    int sendMessage(UserMessage message);

    // 받은 쪽지함 조회
    ArrayList<UserMessage> getReceivedMessages(String receiverUserName);

    // 보낸 쪽지함 조회
    ArrayList<UserMessage> getSentMessages(String senderUserName);

    // 쪽지 상세 조회
    UserMessage getMessageDetail(int privateMsgNo);

    // 쪽지 읽음 처리
    int markAsRead(int privateMsgNo, String receiverUserName);

    // 쪽지 삭제 (보낸사람용)
    int deleteBySender(int privateMsgNo);

    // 쪽지 삭제 (받은사람용)
    int deleteByReceiver(int privateMsgNo);

    // 안읽은 쪽지 개수 조회
    int getUnreadCount(String receiverUserName);
    
    // 받은 쪽지함 페이지네이션 조회
    ArrayList<UserMessage> getReceivedMessagesWithPaging(String receiverUserName, int offset, int pageSize);

    // 보낸 쪽지함 페이지네이션 조회  
    ArrayList<UserMessage> getSentMessagesWithPaging(String senderUserName, int offset, int pageSize);

    // 받은 쪽지 전체 개수 조회
    int getTotalReceivedMessagesCount(String receiverUserName);

    // 보낸 쪽지 전체 개수 조회
    int getTotalSentMessagesCount(String senderUserName);
    
    
    // ========================= 관리자용 ===========================
        
    // 관리자용 전체 쪽지 조회
    ArrayList<UserMessage> getAllMessagesForAdmin(int offset, int pageSize);
        
    // 관리자용 전체 쪽지 개수    
    int getTotalMessagesCount();
        
    // 관리자용 쪽지 검색
    ArrayList<UserMessage> searchMessagesForAdmin(Map<String, Object> searchParams);    
    
    // 관리자용 검색 결과 개수  
    int getSearchMessagesCount(Map<String, Object> searchParams);
        
    // 관리자용 쪽지 강제 삭제 (완전 삭제)     
    int forceDeleteMessage(int privateMsgNo);
        
    // 관리자용 쪽지 수정    
    int updateMessageByAdmin(UserMessage message);
        
    // 리자용 일괄 삭제 
    int bulkDeleteMessages(ArrayList<Integer> messageIds);
      
    // 사용자 쪽지 기능 차단    
    int blockUserMessage(String targetUserName, Integer blockDays, String reason, String adminUserName);
        
    // 사용자 쪽지 차단 상태 확인
    boolean isUserMessageBlocked(String userName);
        
    // 관리자용 쪽지 복구 (삭제 취소)
    int restoreMessage(int privateMsgNo, String restoreType);
        
    // 관리자용 쪽지 상태 변경
    int updateMessageStatus(int privateMsgNo, String readYn, String deleteSender, String deleteReceiver);
    
}