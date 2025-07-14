package com.mafia.game.message.model.service;

import java.util.ArrayList;
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
}