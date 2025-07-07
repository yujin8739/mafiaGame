package com.mafia.game.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.vo.GameRoom;

@Controller
public class MainController {

    @Autowired
    private GameRoomService gameRoomService; // 추가
    
    @GetMapping("/")
    public String main(Model model) {
        // 방 목록 데이터 추가
        List<GameRoom> rooms = gameRoomService.getAllRooms();
        
        // 인원수 계산 (GameRoomController와 동일한 로직)
        ObjectMapper mapper = new ObjectMapper();
        for (GameRoom room : rooms) {
            int userCount = 0;
            if (room.getUserList() != null && !room.getUserList().isEmpty() && !room.getUserList().equals("[]")) {
                try {
                    List<String> users = mapper.readValue(room.getUserList(), new TypeReference<List<String>>() {});
                    userCount = users.size();
                } catch (Exception e) {
                    userCount = 0;
                }
            }
            room.setCurrentUserCount(userCount);
        }        
        model.addAttribute("rooms", rooms);
        return "index";
    }
}