package com.mafia.game.webSocket.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.mafia.game.webSocket.model.vo.GameRoom;

/**
 * 게임룸을 관리하는 내부 클래스
 */
@Component
class GameRoomManager {

	//현존하는 게임방의 객체 (vo)
    private final Map<Integer, GameRoom> gameRoomMap = new ConcurrentHashMap<>();
    //방의 세션
    private final Map<Integer, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public void addSession(int roomNo, WebSocketSession session) {
        roomSessions.putIfAbsent(roomNo, new CopyOnWriteArrayList<>());
        roomSessions.get(roomNo).add(session);
    }

    public void removeSession(int roomNo, WebSocketSession session) {
        List<WebSocketSession> sessions = roomSessions.get(roomNo);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomNo);
            }
        }
    }

    public List<WebSocketSession> getSessions(int roomNo) {
        return roomSessions.getOrDefault(roomNo, Collections.emptyList());
    }

    public void createRoom(GameRoom room) {
        gameRoomMap.put(room.getRoomNo(), room);
    }

    public GameRoom getRoom(int roomNo) {
        return gameRoomMap.get(roomNo);
    }
}