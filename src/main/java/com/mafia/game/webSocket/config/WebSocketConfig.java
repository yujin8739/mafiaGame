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

import com.mafia.game.webSocket.server.HomeChatServer;

import jakarta.websocket.server.ServerContainer;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{
	static {
        System.out.println("WebSocketConfig 클래스 로딩됨");
    }

    public WebSocketConfig() {
        System.out.println("WebSocketConfig 클래스 생성됨");
    }
	//각 웹소켓 서버 Bean으로 등록
	@Bean
	public WebSocketHandler basicServer() {
		System.out.println("basicServer() 빈 생성");
		return new HomeChatServer();
	}
	
	//요청 매핑주소와 웹소켓 서버 연결하는 핸들러 처리 
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		System.out.println("확인==================================");
		registry.addHandler(basicServer(), "/homeChat")
				.addInterceptors(new HttpSessionHandshakeInterceptor());
	}
	
	//보낼수 있는 최대 크기 조정
	@Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                context.addServletContainerInitializer((c, ctx) -> {
                    ServerContainer serverContainer = (ServerContainer) ctx.getAttribute("jakarta.websocket.server.ServerContainer");

                    if (serverContainer instanceof WsServerContainer wsContainer) {
                        wsContainer.setDefaultMaxTextMessageBufferSize(10240 * 10240); // 1MB
                        wsContainer.setDefaultMaxBinaryMessageBufferSize(10240 * 10240); // 1MB
                    }
                }, null);
            }
        };
    }
	
	

}
