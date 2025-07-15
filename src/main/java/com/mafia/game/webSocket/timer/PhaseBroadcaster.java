package com.mafia.game.webSocket.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.webSocket.server.GameRoomManager;

import java.util.List;

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

        broadcastPhase(currentPhase, currentDuration);

        scheduler.schedule(() -> {
            phaseIndex = (phaseIndex + 1) % phases.length;
            schedulePhase();
        }, currentDuration, TimeUnit.SECONDS);
    }

    private void broadcastPhase(String phase, int duration) {
        try {
            String message = mapper.writeValueAsString(
                new PhaseMessage(phase, duration)
            );
            try {
            	if(phase.equals("DAY")) {
	                gameRoomManager.updateDayNo(roomNo);
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
