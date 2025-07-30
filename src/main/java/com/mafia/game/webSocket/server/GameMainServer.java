//package com.mafia.game.webSocket.server;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mafia.game.game.model.vo.GameRoom;
//import com.mafia.game.game.model.vo.Message;
//import com.mafia.game.job.model.vo.Job;
//import com.mafia.game.member.model.vo.Member;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class GameMainServer extends TextWebSocketHandler {
//
//    @Autowired
//    private GameRoomManager roomManager;
//    
//    private final ExecutorService executor = Executors.newCachedThreadPool();
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        int roomNo = extractRoomNo(session);
//        session.getAttributes().put("roomNo", roomNo);
//
//        Member loginUser = (Member) session.getAttributes().get("loginUser");
//        if (loginUser != null) {
//            String userId = loginUser.getUserName();
//            roomManager.addUserToRoom(roomNo, userId);
//            roomManager.bindUserSession(roomNo, userId, session);
//        }
//        roomManager.addSession(roomNo, session);
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        Map<String,Object> raw = mapper.readValue(message.getPayload(), new TypeReference<Map<String,Object>>() {});
//        String rawType = (String) raw.get("type");
//        int roomNo = (int) session.getAttributes().get("roomNo");
//
//        if (isRtcSignalType(rawType)) {
//            handleRtcSignal(session, raw);
//            return;
//        }
//
//        Member loginUser = (Member) session.getAttributes().get("loginUser");
//        String userName = loginUser != null ? loginUser.getUserName() : null;
//        String type = (String) raw.get("type");
//
//        // 게임 상태 변경 로직 (중복 호출에 유의)
//        if ("ready".equals(type)) {
//            roomManager.addReadyToRoom(roomNo, userName);
//            // 준비/해제 같은 상태 변경도 EventServer를 통해 알릴 수 있음
//            // roomManager.broadcastSystemEvent(roomNo, userName + "님이 준비했습니다.");
//        } else if ("unReady".equals(type)) {
//            roomManager.removeReady(roomNo, userName);
//        } else if ("start".equals(type)) {
//            try {
//                roomManager.updateStart(roomNo);
//                // 게임 시작 이벤트
//                // roomManager.broadcastSystemEvent(roomNo, "게임이 시작되었습니다.");
//            } catch (IllegalArgumentException e) {
//                Map<String, Object> errorPayload = new HashMap<>();
//                errorPayload.put("type", "error");
//                errorPayload.put("msg", e.getMessage());
//                errorPayload.put("userName", "system");
//                String errorJson = mapper.writeValueAsString(errorPayload);
//                if (session.isOpen()) {
//                    session.sendMessage(new TextMessage(errorJson));
//                }
//                return;
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                return;
//            }
//        }
//        
//        // 순수 채팅 메시지만 브로드캐스팅
//        if("chat".equals(type) || "mafia".equals(type) || "death".equals(type)) {
//            // 채팅은 DB에 저장
//            Message msg = mapper.convertValue(raw, Message.class);
//            msg.setMsgNo(UUID.randomUUID().toString());
//            msg.setChatDate(new Date());
//            roomManager.sendMessage(msg);
//
//            String json = mapper.writeValueAsString(raw);
//            for (WebSocketSession s : roomManager.getSessions(roomNo)) {
//                if (!s.isOpen()) continue;
//                
//                GameRoom room = roomManager.selectRoom(roomNo);
//                Job job = (Job) s.getAttributes().get("job");
//
//                if (allowSend(type, room, job)) {
//                    executor.submit(()->{
//                        try {
//                            s.sendMessage(new TextMessage(json));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    });
//                }
//            }
//        }
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        int roomNo = (int) session.getAttributes().get("roomNo");
//        roomManager.removeSession(roomNo, session, status);
//    }
//
//    /* ---------- WebRTC 시그널 처리 (텍스트 채팅용) ---------- */
//    private boolean isRtcSignalType(String t) {
//        if (t == null) return false;
//        switch (t) {
//            case "rtcHello":
//            case "rtcOffer":
//            case "rtcAnswer":
//            case "rtcIce":
//                return true;
//            default:
//                return false;
//        }
//    }
//
//    private void handleRtcSignal(WebSocketSession session, Map<String,Object> raw) {
//        int roomNo = (int) session.getAttributes().get("roomNo");
//        Member loginUser = (Member) session.getAttributes().get("loginUser");
//        String fromUser = (loginUser != null ? loginUser.getUserName() : "unknown");
//
//        raw.put("from", fromUser);
//
//        String targetUser = (String) raw.getOrDefault("target", null);
//        if (targetUser == null || targetUser.isBlank()) {
//            targetUser = roomManager.getRoomMasterUser(roomNo);
//        }
//
//        try {
//            String signalJson = mapper.writeValueAsString(raw);
//            roomManager.forwardSignal(roomNo, signalJson, targetUser);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private boolean allowSend(String type, GameRoom room, Job job) {
//        if (room == null) return true;
//        if (job == null) return true;
//        return (!"Y".equals(room.getIsGaming())) ||
//                (!"mafia".equals(type) && !"death".equals(type)) ||
//                ("mafia".equals(type) && ("mafia".equals(job.getJobName()) || "mafiaGhost".equals(job.getJobName()))) ||
//                ("death".equals(type) && ("death".equals(job.getJobName()) || "spiritualists".equals(job.getJobName())));
//    }
//
//    private int extractRoomNo(WebSocketSession session) {
//        String query = session.getUri().getQuery();
//        if (query != null && query.startsWith("roomNo=")) {
//            try {
//                return Integer.parseInt(query.split("=")[1]);
//            } catch (NumberFormatException e) {
//                return -1;
//            }
//        }
//        return -1;
//    }
//}