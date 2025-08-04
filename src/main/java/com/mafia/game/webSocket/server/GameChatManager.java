package com.mafia.game.webSocket.server;

import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.vo.Message;

import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    public GameChatManager(ChatService chatService, @Lazy GameRoomManager gameRoomManager) {
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
    
    /**
     * 과거 채팅 메시지를 불러오는 로직을 전면 수정합니다.
     * 모든 플레이어는 기본적으로 일반 채팅('chat')을 볼 수 있고,
     * 자신의 역할(사망자, 마피아, 연인)에 따라 추가적인 채팅을 볼 수 있도록
     * 메시지를 누적하여 합산하는 방식으로 변경합니다.
     */
    public List<Message> loadPreviousMessages(int roomNo, int page, int size, String jobName) {
        int offset = 0;
        int limit = page * size;
        RowBounds rowBounds = new RowBounds(offset, limit);
        
        List<Message> allMessages = new ArrayList<>();
        
        // 1. 모든 플레이어는 기본적으로 일반 채팅('chat' 타입)과 이벤트('EVENT')를 볼 수 있습니다.
        allMessages.addAll(chatService.getMessages(roomNo, rowBounds));

        // 2. 역할에 따라 볼 수 있는 채팅을 추가합니다.
        if (jobName != null) {
            // 사망자 또는 영매사는 사망자 채팅을 추가로 볼 수 있습니다.
            if (jobName.toLowerCase().contains("ghost") || jobName.equals("spiritualists")) {
                allMessages.addAll(chatService.getMessages(roomNo, "death", rowBounds));
            }
            // 마피아는 마피아 채팅을 추가로 볼 수 있습니다.
            if (jobName.equals("mafia")) {
                allMessages.addAll(chatService.getMessages(roomNo, "mafia", rowBounds));
            }
            // 연인은 연인 채팅을 추가로 볼 수 있습니다.
            if (jobName.contains("marriage")) {
                allMessages.addAll(chatService.getMessages(roomNo, "lovers", rowBounds));
            }
        }
        
        // 3. 합쳐진 모든 메시지에서 중복을 제거하고, 날짜 역순으로 정렬합니다.
        List<Message> distinctMessages = allMessages.stream()
                .distinct() // 혹시 모를 중복 제거
                .sorted(Comparator.comparing(Message::getChatDate).reversed())
                .collect(Collectors.toList());
        
        // 4. 메모리에서 정확한 페이징 처리를 수행합니다.
        int start = (page - 1) * size;
        int end = Math.min(start + size, distinctMessages.size());

        if (start >= distinctMessages.size()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(distinctMessages.subList(start, end));
    }
}