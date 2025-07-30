package com.mafia.game.webSocket.config;

import org.apache.catalina.Context;
import org.apache.tomcat.websocket.server.WsServerContainer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

// 새로 분리된 핸들러들을 import 합니다.
import com.mafia.game.webSocket.server.GameRoomServer;
import com.mafia.game.webSocket.server.GameChatServer;
import com.mafia.game.webSocket.server.GameEventServer;
import com.mafia.game.webSocket.server.VoiceSignalServer;
import com.mafia.game.webSocket.server.HomeChatServer;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{
	
	@Bean
	public WebSocketHandler homeChatServer() {
		return new HomeChatServer();
	}

    @Bean
    public WebSocketHandler gameRoomServer() {
        return new GameRoomServer();
    }

    @Bean
    public WebSocketHandler gameChatServer() {
        return new GameChatServer();
    }

    @Bean
    public WebSocketHandler gameEventServer() {
        return new GameEventServer();
    }

    @Bean
    public WebSocketHandler voiceSignalServer() {
        return new VoiceSignalServer();
    }
	
	/**
	 * 요청 URL과 WebSocket 핸들러를 매핑하고, 인터셉터를 설정합니다.
	 */
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		
		// 1. 홈 채팅 핸들러
		registry.addHandler(homeChatServer(), "/chat/homeChat")
				.addInterceptors(new HttpSessionHandshakeInterceptor());

		// 2. 게임 상태 핸들러 등록
		registry.addHandler(gameRoomServer(), "/room/game")
				.addInterceptors(new HttpSessionHandshakeInterceptor());
        
        // 3. 게임 채팅 핸들러 등록
		registry.addHandler(gameChatServer(), "/chat/game")
				.addInterceptors(new HttpSessionHandshakeInterceptor());

        // 4. 이벤트 핸들러 등록
		registry.addHandler(gameEventServer(), "/chat/gameEvent")
				.addInterceptors(new HttpSessionHandshakeInterceptor());

        // 5. 음성 시그널링 핸들러 등록 (기존 "/chat/gameMainVoice" -> "/signal/voice"로 변경)
		registry.addHandler(voiceSignalServer(), "/chat/gameMainVoice")
				.addInterceptors(new HttpSessionHandshakeInterceptor());
	}
	
	/**
	 * WebSocket 메시지 버퍼 크기를 설정합니다. (기존 코드 유지)
	 */
	@Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                context.addServletContainerInitializer((c, ctx) -> {
                    // jakarta.websocket.server.ServerContainer는 Tomcat 10 이상부터 사용됩니다.
                    // 만약 Tomcat 9 이하라면 "javax.websocket.server.ServerContainer"를 사용해야 할 수 있습니다.
                    Object serverContainerAttr = ctx.getAttribute("jakarta.websocket.server.ServerContainer");
                    if (serverContainerAttr == null) {
                         serverContainerAttr = ctx.getAttribute("javax.websocket.server.ServerContainer");
                    }

                    if (serverContainerAttr instanceof WsServerContainer wsContainer) {
                        wsContainer.setDefaultMaxTextMessageBufferSize(1024 * 1024 * 5); // 5MB
                        wsContainer.setDefaultMaxBinaryMessageBufferSize(1024 * 1024 * 5); // 5MB
                    }
                }, null);
            }
        };
    }	

}