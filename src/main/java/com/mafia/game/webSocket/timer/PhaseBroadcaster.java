package com.mafia.game.webSocket.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.webSocket.server.GameRoomManager;

import java.util.List;

public class PhaseBroadcaster {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<WebSocketSession> sessions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GameRoomManager gameRoomManager;
    private final int roomNo;

    private int phaseIndex = -1;
    private int dayNo = 0;
    private final String[] phases = { "NIGHT", "DAY", "VOTE" };
    private final int[] durations = { 60, 60, 30 };

    public PhaseBroadcaster(List<WebSocketSession> sessions, int roomNo, GameRoomManager gameRoomManager) {
        this.sessions = sessions;
        this.roomNo = roomNo;
        this.gameRoomManager = gameRoomManager;
    }

    public void startPhases() {
        executePhaseTransition();
    }

    private void executePhaseTransition() {
        if (phaseIndex != -1) {
            String previousPhase = phases[phaseIndex];
            if ("NIGHT".equals(previousPhase)) {
                gameRoomManager.processNightActions(roomNo);
            } else if ("VOTE".equals(previousPhase)) {
                gameRoomManager.processVote(roomNo);
            }
        }

        String winner = gameRoomManager.checkWinner(roomNo, phaseIndex);
        if (!"CONTINUE".equals(winner)) {
            endGameAndNotify(winner);
            return;
        }

        phaseIndex = (phaseIndex + 1) % phases.length;
        if (phaseIndex == 0) { // 밤이 시작될 때 dayNo 증가
            dayNo++;
            gameRoomManager.updateDayNo(roomNo);
        }

        String nextPhase = phases[phaseIndex];
        int nextDuration = durations[phaseIndex];

        broadcastPhaseInfo(nextPhase, nextDuration, dayNo);
        scheduler.schedule(this::executePhaseTransition, nextDuration, TimeUnit.SECONDS);
    }

    private void endGameAndNotify(String winner) {
        gameRoomManager.endGame(roomNo, winner);
        stop();
    }

    private void broadcastPhaseInfo(String phase, int duration, int currentDay) {
        try {
            PhaseMessage phaseMessage = new PhaseMessage(phase, duration, currentDay);
            String message = mapper.writeValueAsString(phaseMessage);
            broadcast(message);
        } catch (Exception e) {
            System.err.println("페이즈 정보 브로드캐스팅 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        sessions.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        System.err.println("세션 " + session.getId() + "에 메시지 전송 실패: " + e.getMessage());
                    }
                });
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private static class PhaseMessage {
        public String type = "phase";
        public String phase;
        public int remaining;
        public int dayNo;

        public PhaseMessage(String phase, int remaining, int dayNo) {
            this.phase = phase;
            this.remaining = remaining;
            this.dayNo = dayNo;
        }
    }
}