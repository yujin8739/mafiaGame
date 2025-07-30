package com.mafia.game.webSocket.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.member.model.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy; // ✨ @Lazy 임포트
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class GameEventManager {

    private final GameEventServer eventServer;
    private final ChatService chatService;
    private final MemberService memberService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public GameEventManager(@Lazy GameEventServer eventServer, ChatService chatService, MemberService memberService) { // ✨ @Lazy 추가
        this.eventServer = eventServer;
        this.chatService = chatService;
        this.memberService = memberService;
    }

    public void broadcastMafiaKillEvent(int roomNo, String killedUserName) {
        String victimNickName = memberService.getMemberByUserName(killedUserName).getNickName();
        String eventContent = chatService.selectEvent(6, victimNickName);
        broadcastSystemEvent(roomNo, eventContent);
    }

    public void broadcastVoteResultEvent(int roomNo, String votedUserName, boolean isSurvived) {
        String targetName = memberService.getMemberByUserName(votedUserName).getNickName();
        String eventContent = isSurvived ? chatService.selectEvent(15, targetName) : chatService.selectEvent(9, targetName);
        broadcastSystemEvent(roomNo, eventContent);
    }

    public void broadcastGameEndEvent(int roomNo, String winner) {
        Map<String, Object> payload = Map.of(
                "type", "gameEnd",
                "winner", winner
        );
        try {
            eventServer.broadcastEvent(roomNo, mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    
    public void broadcastSystemEvent(int roomNo, String eventContent) {
        try {
            Message dbMsg = new Message(roomNo, UUID.randomUUID().toString(), "EVENT", eventContent, "시스템", new Date());
            chatService.sendMessage(dbMsg);

            Map<String, Object> payload = Map.of(
                    "type", "EVENT",
                    "userName", "시스템",
                    "msg", eventContent
            );
            
            eventServer.broadcastEvent(roomNo, mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            System.err.println("Failed to create event payload: " + e.getMessage());
            e.printStackTrace();
        }
    }
}