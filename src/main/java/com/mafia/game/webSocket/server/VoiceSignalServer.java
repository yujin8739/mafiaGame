package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.member.model.vo.Member;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceSignalServer extends TextWebSocketHandler {

	private static final Map<String, Map<Integer, Map<String, WebSocketSession>>> allChannels = new ConcurrentHashMap<>();
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final ObjectMapper mapper = new ObjectMapper();
	private final String channelType;

	public VoiceSignalServer(String channelType) {
		this.channelType = channelType;
		allChannels.putIfAbsent(channelType, new ConcurrentHashMap<>());
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		int roomNo = extractRoomNo(session);
		Member loginUser = (Member) session.getAttributes().get("loginUser");
		if (loginUser == null || roomNo == -1) {
			session.close();
			return;
		}
		String userName = loginUser.getUserName();
		session.getAttributes().put("roomNo", roomNo);
		session.getAttributes().put("userName", userName);
		Map<Integer, Map<String, WebSocketSession>> channelSessions = allChannels.get(channelType);
		channelSessions.computeIfAbsent(roomNo, k -> new ConcurrentHashMap<>()).put(userName, session);
//		System.out.println("[VoiceServer-" + channelType + "] User '" + userName + "' connected to room " + roomNo);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		Map<String, Object> raw = mapper.readValue(message.getPayload(), new TypeReference<>() {
		});
		int roomNo = (int) session.getAttributes().get("roomNo");
		String fromUser = (String) session.getAttributes().get("userName");
		raw.put("from", fromUser);
		String json = mapper.writeValueAsString(raw);
		String target = (String) raw.get("target");
		Map<String, WebSocketSession> currentRoomSessions = allChannels.get(channelType).get(roomNo);
		if (currentRoomSessions == null)
			return;
		if (target != null && !target.isBlank()) {
			WebSocketSession targetSession = currentRoomSessions.get(target);
			if (targetSession != null && targetSession.isOpen())
				sendAsync(targetSession, json);
		} else {
			for (WebSocketSession s : currentRoomSessions.values()) {
				if (s.isOpen() && !s.getId().equals(session.getId()))
					sendAsync(s, json);
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		Integer roomNo = (Integer) session.getAttributes().get("roomNo");
		String userName = (String) session.getAttributes().get("userName");
		if (roomNo == null || userName == null)
			return;
		Map<Integer, Map<String, WebSocketSession>> channelSessions = allChannels.get(channelType);
		Map<String, WebSocketSession> currentRoomSessions = channelSessions.get(roomNo);
		if (currentRoomSessions != null) {
			currentRoomSessions.remove(userName);
			if (currentRoomSessions.isEmpty()) {
				channelSessions.remove(roomNo);
			}
		}
		//System.out.println("[VoiceServer-" + channelType + "] User '" + userName + "' disconnected from room " + roomNo);
	}

	private void sendAsync(WebSocketSession session, String payload) {
		executor.submit(() -> {
			try {
				if (session.isOpen())
					session.sendMessage(new TextMessage(payload));
			} catch (IOException e) {
				/* session closed */ }
		});
	}

	private int extractRoomNo(WebSocketSession session) {
		String query = session.getUri().getQuery();
		if (query != null && query.startsWith("roomNo=")) {
			try {
				return Integer.parseInt(query.split("=")[1]);
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}
}