package com.mafia.game.webSocket.customSocket;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

public class GameSocket {
    private final WebSocketSession session;
	private String userName;
	private String userJob;

    public GameSocket(WebSocketSession session, String userName, String userJob) {
        this.session = session;
        this.userName = userName;
        this.userJob = userJob;
    }

    public WebSocketSession getSession() {
        return session;
    }
}
