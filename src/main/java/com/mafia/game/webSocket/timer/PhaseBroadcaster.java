package com.mafia.game.webSocket.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class PhaseBroadcaster {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<WebSocketSession> sessions;
    private final ObjectMapper mapper = new ObjectMapper();

    private int phaseIndex = 0;
    private final String[] phases = { "NIGHT", "DAY", "VOTE" };
    private final int[] durations = { 60, 60, 30 }; // 초 단위

    public PhaseBroadcaster(List<WebSocketSession> sessions) {
        this.sessions = sessions;
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
