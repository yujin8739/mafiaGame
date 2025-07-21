package com.mafia.game.webSocket.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.Message;
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
    private final GameRoomManager gameRoomManager;
    private final int roomNo;

    private int phaseIndex = -1;
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

            // ⭐️⭐️⭐️⭐️⭐️ 여기가 핵심 수정 부분입니다 ⭐️⭐️⭐️⭐️⭐️
            // 'DAY'가 아닌 'NIGHT'가 끝났을 때 마피아 킬을 처리하도록 수정했습니다.
            if ("NIGHT".equals(previousPhase)) {
                gameRoomManager.mafiaKill(roomNo);
            } else if ("VOTE".equals(previousPhase)) {
                gameRoomManager.voteKill(roomNo);
            }
        }

        String winner = gameRoomManager.checkWinner(roomNo);

        if (!"CONTINUE".equals(winner)) {
            endGameAndNotify(winner);
            return;
        } else {
            phaseIndex = (phaseIndex + 1) % phases.length;
            String nextPhase = phases[phaseIndex];
            int nextDuration = durations[phaseIndex];

            broadcastPhaseInfo(nextPhase, nextDuration);
            scheduler.schedule(this::executePhaseTransition, nextDuration, TimeUnit.SECONDS);
        }
    }

    private void endGameAndNotify(String winner) {
        try {
            Message msg = new Message(roomNo, UUID.randomUUID().toString(), winner, "게임이 종료되었습니다.", "시스템", new Date());

            Map<String, Object> payload = new HashMap<>();
            payload.put("userName", msg.getUserName());
            payload.put("msg", msg.getMsg());
            payload.put("type", winner);

            String finalMessage = mapper.writeValueAsString(payload);
            broadcast(finalMessage);

            gameRoomManager.updateStop(roomNo);
            stop();

        } catch (Exception e) {
            System.err.println("게임 종료 메시지 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastPhaseInfo(String phase, int duration) {
        try {
            PhaseMessage phaseMessage = new PhaseMessage(phase, duration);
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

        public PhaseMessage(String phase, int remaining) {
            this.phase = phase;
            this.remaining = remaining;
        }
    }
}