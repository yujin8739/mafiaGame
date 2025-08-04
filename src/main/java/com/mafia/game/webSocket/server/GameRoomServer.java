package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.member.model.vo.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameRoomServer extends TextWebSocketHandler {

    @Autowired
    private GameRoomManager roomManager;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session);
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (loginUser == null || roomNo == -1) {
            session.close(new CloseStatus(4001, "Invalid session for room server"));
            return;
        }

        session.getAttributes().put("roomNo", roomNo);
        
        roomManager.addSession(roomNo, session, loginUser.getUserName());
//        System.out.println("[GameRoomServer] User '" + loginUser.getUserName() + "' connected to room " + roomNo);
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 모든 속성 접근 전에 Null 체크를 수행하여 안정성 확보
        Integer roomNo = (Integer) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (roomNo == null || loginUser == null) {
            System.err.println("Session attributes are missing. Closing session.");
            session.close(new CloseStatus(4002, "Session attributes missing"));
            return;
        }
        
        String userName = loginUser.getUserName();
        Map<String, Object> raw = mapper.readValue(message.getPayload(), new TypeReference<>() {});
        String type = (String) raw.get("type");
        
        if (type == null) return;

        switch (type) {
        	case "requestSync":
	            roomManager.sendFullGameState(roomNo, userName);
	            break;
	        case "ping":
	            roomManager.recordHeartbeat(userName);
	            break;
            case "ready":
                roomManager.addReadyToRoom(roomNo, userName);
                break;
            case "unReady":
                roomManager.removeReady(roomNo, userName);
                break;
            case "start":
                try {
                    roomManager.updateStart(roomNo);
                } catch (IllegalArgumentException e) {
                    sendError(session, e.getMessage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sendError(session, "게임 시작 중 알 수 없는 오류가 발생했습니다.");
                }
                break;
            case "vote":
                String votedName = (String) raw.get("targetName");
                if (votedName != null) roomManager.castVote(roomNo, userName, votedName);
                break;
            case "useAbility":
                String targetName = (String) raw.get("targetName");
                roomManager.useAbility(roomNo, userName, targetName);
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer roomNo = (Integer) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (roomNo == null || loginUser == null) return;

        roomManager.removeSession(roomNo, session, loginUser.getUserName());
//        System.out.println("[GameRoomServer] User '" + loginUser.getUserName() + "' disconnected from room " + roomNo);
    }
    
    private void sendError(WebSocketSession session, String message) throws IOException {
        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("type", "error");
        errorPayload.put("msg", message);
        errorPayload.put("userName", "system");
        String errorJson = mapper.writeValueAsString(errorPayload);
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(errorJson));
        }
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