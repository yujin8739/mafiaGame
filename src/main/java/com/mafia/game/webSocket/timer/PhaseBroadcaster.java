package com.mafia.game.webSocket.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.webSocket.server.GameRoomManager;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhaseBroadcaster {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<WebSocketSession> sessions;
    private final ObjectMapper mapper = new ObjectMapper();

    private int phaseIndex = 0;
    private final String[] phases = { "NIGHT", "DAY", "VOTE" };
    private final int[] durations = { 60, 60, 30 }; // 초 단위
    private int roomNo;
    private GameRoomManager gameRoomManager;

    public PhaseBroadcaster(List<WebSocketSession> sessions, int roomNo, GameRoomManager gameRoomManager) {
        this.sessions = sessions;
        this.roomNo = roomNo;
        this.gameRoomManager = gameRoomManager;
    }

    public void startPhases() {
        schedulePhase();
    }

    private void schedulePhase() {
        int currentDuration = durations[phaseIndex];
        String currentPhase = phases[phaseIndex];
        String winner = gameRoomManager.checkWinner(roomNo);
    	if (winner.equals("MAFIA_WIN") || winner.equals("CITIZEN_WIN") || winner.equals("NEUTRALITY_WIN")) {
    		currentPhase = winner;
    		currentDuration = 0;
    		broadcastPhase(currentPhase, currentDuration);
    		gameRoomManager.updateStop(roomNo);
    	} else {
    		broadcastPhase(currentPhase, currentDuration);
    		scheduler.schedule(() -> {
    			phaseIndex = (phaseIndex + 1) % phases.length;
    			schedulePhase();
    		}, currentDuration, TimeUnit.SECONDS);
    	}
    	
    }

    private void broadcastPhase(String phase, int duration) {
        try {
            String message = mapper.writeValueAsString(
                new PhaseMessage(phase, duration)
            );
            try {
            	if(duration == 0 && 
            	(phase.equals("MAFIA_WIN") 
            	|| phase.equals("CITIZEN_WIN") 
            	|| phase.equals("NEUTRALITY_WIN"))) {
            		Message msg = new Message(roomNo, UUID.randomUUID().toString(), phase, "게임이 종료되었습니다.", "시스템", new Date());
            		
            		gameRoomManager.sendMessage(msg);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("userName", msg.getUserName()); // nickName
                    payload.put("msg", msg.getMsg());     // 메시지 본문
                    payload.put("type", msg.getType());

                    message = mapper.writeValueAsString(payload);
            	} else {
	            	if(phase.equals("DAY")) {
	            		gameRoomManager.mafiaKill(roomNo);
	            	} else if (phase.equals("NIGHT")) {
	            		gameRoomManager.voteKill(roomNo);
	            	}
            	}
            } catch (Exception dbEx) {
                System.err.println("[DB 오류] broadcastPhase 중 DB 접근 실패: " + dbEx.getMessage());
                dbEx.printStackTrace();
            }
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private static class PhaseMessage {
        public String type = "phase";
        public String phase;
        public int remaining;

        public PhaseMessage(String phase, int remaining) {
            this.phase = phase;
            this.remaining = remaining;
        }
    }
}
