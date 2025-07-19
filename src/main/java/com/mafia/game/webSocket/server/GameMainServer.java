package com.mafia.game.webSocket.server;

import java.util.*;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.vo.Member;

public class GameMainServer extends TextWebSocketHandler {

    @Autowired
    private GameRoomManager roomManager;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Set<String> VOICE_TYPES = Set.of(
  		  "voiceHostStart","voiceHostStop","voiceReady",
  		  "voiceOffer","voiceAnswer","voiceCandidate",
  		  "voiceMute","voiceUnmute");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session);
        session.getAttributes().put("roomNo", roomNo);

        Member loginUser = (Member) session.getAttributes().get("loginUser");
        if (loginUser != null) {
            String userId = loginUser.getUserName();
            roomManager.addUserToRoom(roomNo, userId);  // 기존 로직 유지
            roomManager.bindUserSession(roomNo, userId, session); // NEW: Failover & WebRTC
        }
        roomManager.addSession(roomNo, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        // 먼저 raw 파싱 → type 확인
        Map<String,Object> raw = mapper.readValue(message.getPayload(), new TypeReference<Map<String,Object>>() {});
        String rawType = (String) raw.get("type");
        int roomNo = (int) session.getAttributes().get("roomNo");

     // ...
        if (isVoiceSignal(rawType)) {
            String target = (String) raw.get("target");
            String json = mapper.writeValueAsString(raw);

            if (target != null && !target.isBlank()) {
                // ✨✨✨ 수정된 핵심 로직 ✨✨✨
                // GameRoomManager의 신뢰성 높은 세션 검색 기능을 직접 사용합니다.
                WebSocketSession targetSession = roomManager.getSessionByUser(roomNo, target);

                if (targetSession != null && targetSession.isOpen()) {
                    targetSession.sendMessage(new TextMessage(json));
                } else {
                    // (선택적) 디버깅을 위해 대상이 없을 경우 로그를 남길 수 있습니다.
                    System.out.println("## WebRTC 음성 신호 타겟 유저를 찾지 못했습니다: " + target);
                }
                
            } else {
                // target이 없는 메시지(voiceReady, voiceHostStart 등)는 기존처럼 전체 방송합니다.
                for (WebSocketSession s : roomManager.getSessions(roomNo)) {
                    if (s.isOpen() && !s.getId().equals(session.getId())) {
                        s.sendMessage(new TextMessage(json));
                    }
                }
            }
            return; // 메시지 처리가 끝났으므로 여기서 종료
        }
        

        if (isRtcSignalType(rawType)) {
            handleRtcSignal(session, raw);
            return;
        }

        // 기존 게임 메시지 처리
        Message msg = mapper.convertValue(raw, Message.class);

        // DB 저장
        msg.setMsgNo(UUID.randomUUID().toString());
        msg.setChatDate(new Date());
        roomManager.sendMessage(msg);
        
        Member loginUser = (Member) session.getAttributes().get("loginUser");
        String userName = loginUser != null ? loginUser.getUserName() : null;

        String type = msg.getType();
        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", msg.getUserName()); // nickName
        payload.put("msg", msg.getMsg());
        payload.put("type", type);

        String json = mapper.writeValueAsString(payload);

        // 브로드캐스트 (기존 필터링)
        for (WebSocketSession s : roomManager.getSessions(roomNo)) {
            if (!s.isOpen()) continue;

            if ("ready".equals(type)) {
                roomManager.addReadyToRoom(roomNo, userName);
            } else if ("unReady".equals(type)) {
                roomManager.removeReady(roomNo, userName);
            } else if ("start".equals(type)) {
                roomManager.updateStart(roomNo);
                roomManager.addJobToSession(roomNo, s);
            }

            GameRoom room = roomManager.selectRoom(roomNo);
            Job job = (Job) s.getAttributes().get("job");

            if (allowSend(type, room, job)) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        int roomNo = (int) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");
        String nickName = (loginUser != null ? loginUser.getNickName() : "알수없음");

        Message msg = new Message(roomNo, UUID.randomUUID().toString(), "leave", nickName + "님이 퇴장하셨습니다.", nickName, new Date());
        roomManager.sendMessage(msg);

        roomManager.removeSession(roomNo, session, status);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", nickName);
        payload.put("msg", msg.getMsg());
        payload.put("type", msg.getType());

        String json = mapper.writeValueAsString(payload);

        for (WebSocketSession s : roomManager.getSessions(roomNo)) {
            if (s.isOpen() && !s.getId().equals(session.getId())) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }

    /* ---------- WebRTC 시그널 처리 ---------- */
    private boolean isRtcSignalType(String t) {
        if (t == null) return false;
        switch (t) {
            case "rtcHello":
            case "rtcOffer":
            case "rtcAnswer":
            case "rtcIce":
                return true;
            default:
                return false;
        }
    }

    private void handleRtcSignal(WebSocketSession session, Map<String,Object> raw) {
        int roomNo = (int) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");
        String fromUser = (loginUser != null ? loginUser.getUserName() : "unknown");

        raw.put("from", fromUser);

        String targetUser = (String) raw.getOrDefault("target", null);
        if (targetUser == null || targetUser.isBlank()) {
            targetUser = roomManager.getRoomMasterUser(roomNo); // default: host
        }

        try {
            String signalJson = mapper.writeValueAsString(raw);
            roomManager.forwardSignal(roomNo, signalJson, targetUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean allowSend(String type, GameRoom room, Job job) {
        if (room == null) return true;
        if (job == null) return true;
        return (!"Y".equals(room.getIsGaming())) ||
                (!"mafia".equals(type) && !"death".equals(type)) ||
                ("mafia".equals(type) && ("mafia".equals(job.getJobName()) || "mafiaGhost".equals(job.getJobName()))) ||
                ("death".equals(type) && ("death".equals(job.getJobName()) || "spiritualists".equals(job.getJobName())));
    }

    private int extractRoomNo(WebSocketSession session) {
        String query = session.getUri().getQuery(); // e.g. roomNo=3
        if (query != null && query.startsWith("roomNo=")) {
            return Integer.parseInt(query.split("=")[1]);
        }
        return -1;
    }

    private boolean isVoiceSignal(String type) {
    	return VOICE_TYPES.contains(type);
    }
}
