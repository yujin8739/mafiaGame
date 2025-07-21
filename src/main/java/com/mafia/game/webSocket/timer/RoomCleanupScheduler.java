package com.mafia.game.webSocket.timer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.webSocket.server.GameRoomManager;

public class RoomCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoomCleanupScheduler.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Integer, ScheduledFuture<?>> cleanupTasks = new ConcurrentHashMap<>();

    private final GameRoomService gameRoomService;
    private final GameRoomManager gameRoomManager;
    private final long cleanupDelayMs;

    public RoomCleanupScheduler(GameRoomService gameRoomService, GameRoomManager gameRoomManager, long cleanupDelayMs) {
        this.gameRoomService = gameRoomService;
        this.gameRoomManager = gameRoomManager;
        this.cleanupDelayMs = cleanupDelayMs;
    }

    public void scheduleIfEmpty(int roomNo) {
        cancel(roomNo);
        ScheduledFuture<?> f = scheduler.schedule(() -> performCleanup(roomNo), cleanupDelayMs, TimeUnit.MILLISECONDS);
        cleanupTasks.put(roomNo, f);
        log.info("[RoomCleanup] room {} empty, scheduled deletion in {}ms", roomNo, cleanupDelayMs);
    }

    public void cancel(int roomNo) {
        ScheduledFuture<?> f = cleanupTasks.remove(roomNo);
        if (f != null) {
            f.cancel(false);
            log.info("[RoomCleanup] room {} cleanup canceled", roomNo);
        }
    }

    private void performCleanup(int roomNo) {
        try {
            GameRoom room = gameRoomService.selectRoom(roomNo);
            if (room == null) return;

            List<String> userList = gameRoomManager.parseJsonList(room.getUserList());
            if (userList == null || userList.isEmpty()) {
                log.info("[RoomCleanup] Deleting empty room {}", roomNo);
                gameRoomManager.stopPhaseBroadcast(roomNo);
                gameRoomService.deleteRoom(roomNo);
                gameRoomManager.removeRoomCaches(roomNo);
            }
        } catch (Exception e) {
            log.error("[RoomCleanup] Error cleaning room {}", roomNo, e);
        } finally {
            cleanupTasks.remove(roomNo);
        }
    }
}
