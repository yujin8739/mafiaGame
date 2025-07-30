package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.vo.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameChatServer extends TextWebSocketHandler {

    @Autowired
    private GameRoomManager roomManager;

    @Autowired
    private GameChatManager chatManager;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session);
        Member loginUser = (Member) session.getAttributes().get("loginUser");
        if (loginUser == null || roomNo == -1) {
            session.close(); return;
        }
        session.getAttributes().put("roomNo", roomNo);
        session.getAttributes().put("userName", loginUser.getUserName());
        roomManager.bindChatSession(roomNo, loginUser.getUserName(), session);

        loadAndSendPreviousMessages(session, roomNo, 1);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> raw = mapper.readValue(message.getPayload(), new TypeReference<>() {});
        String type = (String) raw.get("type");
        int roomNo = (int) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");
        String userName = loginUser.getUserName();
        
        if ("loadMore".equals(type)) {
            int page = (int) raw.get("page");
            loadAndSendPreviousMessages(session, roomNo, page);
            return;
        }
        
        if (isRtcSignalType(type)) {
            handleRtcSignal(session, raw, loginUser);
            return;
        }
        
        if (isChatMessage(type)) {
            Message msg = mapper.convertValue(raw, Message.class);
            msg.setMsgNo(UUID.randomUUID().toString());
            msg.setChatDate(new Date());
            chatManager.sendMessage(msg);

            String json = mapper.writeValueAsString(raw);
            for (WebSocketSession s : roomManager.getChatSessions(roomNo)) {
                if (!s.isOpen()) continue;
                
                String targetUserName = (String) s.getAttributes().get("userName");
                Job senderJob = roomManager.getJobForUser(roomNo, loginUser.getUserName());
                Job receiverJob = roomManager.getJobForUser(roomNo, targetUserName);
                
                if (allowSend(type, roomManager.selectRoom(roomNo), senderJob, receiverJob)) {
                    executor.submit(() -> {
                        try { s.sendMessage(new TextMessage(json)); }
                        catch (IOException e) { e.printStackTrace(); }
                    });
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer roomNo = (Integer) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (roomNo != null && loginUser != null) {
            roomManager.unbindChatSession(roomNo, loginUser.getUserName());
        }
    }

    private void loadAndSendPreviousMessages(WebSocketSession session, int roomNo, int page) {
        final int pageSize = 30;
        String userName = (String) session.getAttributes().get("userName");
        Job job = roomManager.getJobForUser(roomNo, userName);
        String jobName = (job != null) ? job.getJobName() : null;

        List<Message> messages = chatManager.loadPreviousMessages(roomNo, page, pageSize, jobName);
        Collections.reverse(messages);

        try {
            String payload = mapper.writeValueAsString(Map.of("type", "load", "messages", messages));
            executor.submit(() -> {
                try { session.sendMessage(new TextMessage(payload)); }
                catch (IOException e) { e.printStackTrace(); }
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private boolean isRtcSignalType(String t) {
        return t != null && (t.equals("rtcHello") || t.equals("rtcOffer") || t.equals("rtcAnswer") || t.equals("rtcIce"));
    }

    private boolean isChatMessage(String t) {
        return "chat".equals(t) || "mafia".equals(t) || "death".equals(t) || "lovers".equals(t);
    }

    private void handleRtcSignal(WebSocketSession session, Map<String,Object> raw, Member loginUser) {
        int roomNo = (int) session.getAttributes().get("roomNo");
        String fromUser = loginUser.getUserName();
        raw.put("from", fromUser);

        String targetUser = (String) raw.getOrDefault("target", null);
        if (targetUser == null || targetUser.isBlank()) {
            targetUser = roomManager.getRoomMasterUser(roomNo);
        }

        try {
            String signalJson = mapper.writeValueAsString(raw);
            chatManager.forwardSignal(roomNo, signalJson, targetUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean allowSend(String type, GameRoom room, Job senderJob, Job receiverJob) {
        if (room == null || !"Y".equals(room.getIsGaming())) return true;
        if (senderJob == null || receiverJob == null) return true;
        
        String senderJobName = senderJob.getJobName();
        String receiverJobName = receiverJob.getJobName();
        
        // 마피아 채팅: 마피아 팀끼리만
        if ("mafia".equals(type)) {
            return senderJob.getJobClass() == 1 && receiverJob.getJobClass() == 1;
        }

        // 사망자 채팅: 사망자, 영매사, 네크로맨서만
        if ("death".equals(type)) {
            boolean isSenderDead = senderJob.getJobNo() == 0 || senderJobName.contains("Ghost");
            boolean isReceiverAllowed = (receiverJob.getJobNo() == 0 || receiverJobName.contains("Ghost") || "spiritualists".equals(receiverJobName));
            return isSenderDead && isReceiverAllowed;
        }

        // 연인 채팅: 연인끼리만
        if ("lovers".equals(type)) {
            boolean isSenderLover = "marriage_man".equals(senderJobName) || "marriage_woman".equals(senderJobName);
            boolean isReceiverLover = "marriage_man".equals(receiverJobName) || "marriage_woman".equals(receiverJobName);
            return isSenderLover && isReceiverLover;
        }
        
        // 일반 채팅: 모든 생존자
        return senderJob.getJobNo() != 0 && !senderJobName.contains("Ghost");
    }

    private int extractRoomNo(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("roomNo=")) {
            try { return Integer.parseInt(query.split("=")[1]); }
            catch (NumberFormatException e) { return -1; }
        }
        return -1;
    }
}