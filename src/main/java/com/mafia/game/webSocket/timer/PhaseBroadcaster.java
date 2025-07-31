package com.mafia.game.webSocket.timer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.webSocket.server.GameRoomManager;

public class PhaseBroadcaster {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private List<WebSocketSession> sessions; // [수정] final을 제거하여 갱신 가능하도록 변경
    private final ObjectMapper mapper = new ObjectMapper();
    private final GameRoomManager gameRoomManager;
    private final int roomNo;

    private int phaseIndex = -1;
    private int dayNo = 0;
    private final String[] phases = { "NIGHT", "DAY", "VOTE" };
    private final int[] durations = { 60, 60, 30 };

    // [추가] 현재 페이즈의 남은 시간을 추적하기 위한 필드
    private long phaseEndTime;
    private ScheduledFuture<?> nextPhaseFuture;

    public PhaseBroadcaster(List<WebSocketSession> sessions, int roomNo, GameRoomManager gameRoomManager) {
        this.sessions = sessions;
        this.roomNo = roomNo;
        this.gameRoomManager = gameRoomManager;
    }

    public void startPhases() {
        executePhaseTransition();
    }

    private void executePhaseTransition() {
        try {
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
        } catch (Exception e) {
            System.out.println("CRITICAL ERROR during game logic processing in room " + roomNo + ". Forcing next phase."+ e);
        }

        phaseIndex = (phaseIndex + 1) % phases.length;
        if (phaseIndex == 0) {
            dayNo++;
            gameRoomManager.updateDayNo(roomNo);
        }

        String nextPhase = phases[phaseIndex];
        int nextDuration = durations[phaseIndex];

        // [추가] 다음 페이즈가 끝나는 정확한 시간을 기록
        this.phaseEndTime = Instant.now().getEpochSecond() + nextDuration;

        broadcastPhaseInfo(nextPhase, nextDuration, dayNo);
        nextPhaseFuture = scheduler.schedule(this::executePhaseTransition, nextDuration, TimeUnit.SECONDS);
    }

    private void endGameAndNotify(String winner) {
        gameRoomManager.endGame(roomNo, winner);
        stop();
    }

    private void broadcastPhaseInfo(String phase, int duration, int currentDay) {
        try {
            GameRoom currentRoom = gameRoomManager.selectRoom(roomNo);
            Map<String, Job> currentUserJobs = gameRoomManager.getUserJobs(roomNo);

            PhaseMessage phaseMessage = new PhaseMessage(phase, duration, currentDay, currentRoom, currentUserJobs);
            String message = mapper.writeValueAsString(phaseMessage);
            broadcast(message);
        } catch (Exception e) {
            System.err.println("페이즈 정보 브로드캐스팅 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        // [중요] 항상 최신 세션 목록을 사용합니다.
        List<WebSocketSession> currentSessions = this.sessions; 
        currentSessions.stream()
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

    public void updateSessions(List<WebSocketSession> newSessions) {
        this.sessions = newSessions;
        System.out.println("Room " + roomNo + " broadcaster sessions updated. New count: " + newSessions.size());
    }

    public String getCurrentPhase() {
        return (phaseIndex >= 0 && phaseIndex < phases.length) ? phases[phaseIndex] : "WAITING";
    }

    public int getRemainingTime() {
        long now = Instant.now().getEpochSecond();
        int remaining = (int) (this.phaseEndTime - now);
        return Math.max(0, remaining); // 남은 시간이 음수가 되지 않도록 보장
    }

    public int getDayNo() {
        return this.dayNo;
    }

    private static class PhaseMessage {
        public String type = "phase";
        public String phase;
        public int remaining;
        public int dayNo;
        public GameRoom room;
        public Map<String, Job> userJobs;

        public PhaseMessage(String phase, int remaining, int dayNo, GameRoom room, Map<String, Job> userJobs) {
            this.phase = phase;
            this.remaining = remaining;
            this.dayNo = dayNo;
            this.room = room;
            this.userJobs = userJobs;
        }
    }
}