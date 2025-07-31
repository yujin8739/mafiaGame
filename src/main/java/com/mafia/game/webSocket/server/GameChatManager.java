package com.mafia.game.webSocket.server;

import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.vo.Message;

import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy; // ✨ @Lazy 임포트
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class GameChatManager {

    private final ChatService chatService;
    private final GameRoomManager gameRoomManager;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    public GameChatManager(ChatService chatService, @Lazy GameRoomManager gameRoomManager) { // ✨ @Lazy 추가
        this.chatService = chatService;
        this.gameRoomManager = gameRoomManager;
    }

    public void sendMessage(Message msg) {
        chatService.sendMessage(msg);
    }

    public void forwardSignal(int roomNo, String signalJson, String targetUser) {
        WebSocketSession targetSession = gameRoomManager.getSessionByUser(roomNo, targetUser);
        if (targetSession != null && targetSession.isOpen()) {
            executor.submit(() -> {
                try {
                    targetSession.sendMessage(new TextMessage(signalJson));
                } catch (Exception e) {
                    System.err.println("Failed to forward text WebRTC signal: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
    
    public List<Message> loadPreviousMessages(int roomNo, int page, int size, String jobName) {
        // RowBounds는 메모리에서 페이징하므로, 원하는 페이지의 데이터를 얻으려면 
        // 충분한 데이터를 미리 가져와야 합니다. (page * size)
        int offset = 0;
        int limit = page * size;
        RowBounds rowBounds = new RowBounds(offset, limit);
        
        List<Message> allMessages = new ArrayList<>();
        
        // 1. 볼 수 있는 모든 타입의 채팅을 충분히 가져옵니다.
        // selectTypeMessagesByRoom 쿼리는 'chat', 'EVENT' 등을 포함하므로,
        // 각 타입별로만 호출하면 됩니다.
        if (jobName != null) {
            if (jobName.contains("Ghost") || "spiritualists".equals(jobName)) {
                allMessages.addAll(chatService.getMessages(roomNo, "death", rowBounds));
            }
            if (jobName.equals("mafia")) {
                allMessages.addAll(chatService.getMessages(roomNo, "mafia", rowBounds));
            }
            if (jobName.contains("marriage")) {
                allMessages.addAll(chatService.getMessages(roomNo, "lovers", rowBounds));
            }
        }
        
        // 만약 위 특수 타입에 해당하지 않는다면, 기본 채팅만 조회
        // (영매사 등은 기본 채팅도 봐야하므로, 아래 조건문은 항상 실행되도록 조정 가능)
        if (allMessages.isEmpty() || "spiritualists".equals(jobName) || "mafia".equals(jobName) || "marriage".equals(jobName)) {
             allMessages.addAll(chatService.getMessages(roomNo, rowBounds));
        }

        // 2. 중복 제거 및 시간순으로 재정렬
        List<Message> distinctMessages = allMessages.stream()
                .distinct()
                .sorted(Comparator.comparing(Message::getChatDate).reversed())
                .collect(Collectors.toList());
        
        // 3. 메모리에서 정확한 페이징 처리
        int start = (page - 1) * size;
        int end = Math.min(start + size, distinctMessages.size());

        if (start >= distinctMessages.size()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(distinctMessages.subList(start, end));
    }
}