package com.mafia.game.webSocket.server;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.vo.Member;


public class GameMainServer extends TextWebSocketHandler {

    @Autowired
    private GameRoomManager roomManager;
	
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        int roomNo = extractRoomNo(session);
        session.getAttributes().put("roomNo", roomNo);

        // 로그인한 사용자 정보 추출
        Member loginUser = (Member) session.getAttributes().get("loginUser");

        if (loginUser != null) {
            String userId = loginUser.getUserName();

            // 👉 userList에 userId를 저장하는 서비스 호출 등 처리
            roomManager.addUserToRoom(roomNo, userId);
        }
        roomManager.addSession(roomNo, session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 메시지 파싱
        ObjectMapper mapper = new ObjectMapper();
        Message msg = mapper.readValue(message.getPayload(), Message.class);

        // DB 저장
        msg.setMsgNo(UUID.randomUUID().toString());
        msg.setChatDate(new Date());
        
        roomManager.sendMessage(msg);
        // 해당 방 세션 목록 가져오기
        int roomNo = msg.getRoomNo();
        
        Member loginUser = (Member) session.getAttributes().get("loginUser");
        String userName = loginUser.getUserName(); //이게 진짜 유저 id 나중에 시간나면 msg.userName은 닉네임으로 바꿔주기
        
        String type = msg.getType();
        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", msg.getUserName()); // nickName
        payload.put("msg", msg.getMsg());     // 메시지 본문
        payload.put("type", msg.getType());

        String json = mapper.writeValueAsString(payload);

        // 브로드캐스트
        for (WebSocketSession s : roomManager.getSessions(roomNo)) {
            if (s.isOpen()) {
                if(type != null && type.equals("ready")) {
                	roomManager.addReadyToRoom(roomNo, userName);
                } else if(type != null && type.equals("unReady")) {
                	roomManager.removeReady(roomNo, userName);
                } else if(type != null && type.equals("start")) {
                	roomManager.updateStart(roomNo);
                	roomManager.addJobToSession(roomNo, s);
                }
                GameRoom room = roomManager.selectRoom(roomNo);
                Job job = (Job) s.getAttributes().get("job");
                if(!room.getIsGaming().equals("Y") //게임중이 아니거나
                		|| (!msg.getType().equals("mafia") && !msg.getType().equals("death")) //죽은사람 채팅이거나 마피아 둘다 아니거나
                		|| (msg.getType().equals("mafia") && (job.getJobName().equals("mafia") || job.getJobName().equals("mafiaGhost"))) //직업조건에 맞거나
                		|| (msg.getType().equals("death") && (job.getJobName().equals("death")|| job.getJobName().equals("spiritualists")))) {
                	s.sendMessage(new TextMessage(json));
                }
            }
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        int roomNo = (int) session.getAttributes().get("roomNo");
        Member loginUser = (Member) session.getAttributes().get("loginUser");
        String nickName =loginUser.getNickName();
        
        Message msg = new Message(roomNo, UUID.randomUUID().toString(), "leave", nickName +"님이 퇴장하셨습니다.", nickName, new Date());
        roomManager.sendMessage(msg);
        
        roomManager.removeSession(roomNo, session, status);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", nickName);
        payload.put("msg", msg.getMsg());
        payload.put("type", msg.getType());
        
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(payload);
        
        for (WebSocketSession s : roomManager.getSessions(roomNo)) {
            if (s.isOpen() && !s.getId().equals(session.getId())) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }

    private int extractRoomNo(WebSocketSession session) {
        String query = session.getUri().getQuery(); // e.g., roomNo=3
        if (query != null && query.startsWith("roomNo=")) {
            return Integer.parseInt(query.split("=")[1]);
        }
        return -1;
    }
}
