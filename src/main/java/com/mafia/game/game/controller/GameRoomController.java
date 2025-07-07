package com.mafia.game.game.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody; // ← 🆕 추가된 import
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/room")
public class GameRoomController {

    @Autowired
    private GameRoomService gameRoomService;
    
	@Autowired
    private ChatService chatService;
    
    private final ObjectMapper objectMapper = new ObjectMapper(); 

    @GetMapping("/createRoom")
    public String createRoomForm() {
        return "game/createRoom";
    }

    @PostMapping("/create")
    public String createRoom(@ModelAttribute GameRoom room, Model model, RedirectAttributes redirectAttributes) {
        
        if (room.getRoomName() == null || room.getRoomName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("msg", "방 이름을 입력해주세요.");
            return "redirect:/room/createRoom";
        }
        
        if (room.getHeadCount() < 6 || room.getHeadCount() > 15) {
            redirectAttributes.addFlashAttribute("msg", "인원수는 6~15명 사이여야 합니다.");
            return "redirect:/room/createRoom";
        }
        
        int result = gameRoomService.createRoom(room);
        
        if (result > 0) {
            redirectAttributes.addFlashAttribute("msg", "방이 성공적으로 생성되었습니다!");
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("msg", "방 생성에 실패했습니다.");
            return "redirect:/room/createRoom";
        }
    }
    
    @GetMapping("/listRoom")
    public String listRooms(Model model) {
        List<GameRoom> rooms = gameRoomService.getAllRooms();
        
        if (rooms == null) {
            rooms = new ArrayList<>();
        }

        for (GameRoom room : rooms) {
            int userCount = 0;
            if (room.getUserList() != null && !room.getUserList().isEmpty() && !room.getUserList().equals("[]")) {
                try {
                    List<String> users = objectMapper.readValue(room.getUserList(), new TypeReference<List<String>>() {});
                    userCount = users.size();
                } catch (Exception e) {
                    System.err.println("JSON 파싱 실패 - 방번호: " + room.getRoomNo() + 
                                      ", userList: " + room.getUserList() + 
                                      ", 에러: " + e.getMessage());
                    userCount = 0;
                }
            }
            room.setCurrentUserCount(userCount); // ← 🎯 setSetCurrentUserCount에서 변경
        }
        
        model.addAttribute("rooms", rooms);
        return "chat/roomList";
    }
    
    @GetMapping("/{roomNo}/{password}")
    public String enterRoom(@PathVariable int roomNo, 
    						@PathVariable String password, 
							Model model, HttpSession session,
							RedirectAttributes redirectAttributes) {
    	
        GameRoom room = gameRoomService.selectRoom(roomNo);
        
        if (room == null) {
        	redirectAttributes.addFlashAttribute("msg","해당 게임방이 존재 하지 않습니다.");
            return "redirect:/";
        }
        

        //비밀번호가 틀리면 홈으로 이동
        if(room.getPassword() != null 
        		&& !room.getPassword().trim().isEmpty() // ← 🔧 trim() 추가
        		&& !room.getPassword().trim().equals(password.trim())) { // ← 🔧 양쪽 trim()
        	 redirectAttributes.addFlashAttribute("msg","게임방의 비밀번호가 틀렸습니다.");
            return "redirect:/";
        }

        // 현재 로그인한 사용자 정보
        Member loginUser = (Member) session.getAttribute("loginUser");
        if (loginUser == null) {
            redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
            return "redirect:/login/view";
        }
        String userName = loginUser.getUserName();

        // userList 파싱
        List<String> users = new ArrayList<>();
        try {
            if (room.getUserList() != null && !room.getUserList().isEmpty()) {
                users = objectMapper.readValue(room.getUserList(), new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("msg","방 정보를 불러오는데 실패했습니다.");
            return "redirect:/";
        }

        // 인원 초과 여부 체크 (이미 들어간 사람은 제외)
        if (!users.contains(userName) && users.size() >= room.getHeadCount()) {
        	redirectAttributes.addFlashAttribute("msg","방 인원이 가득 찼습니다.");
            return "redirect:/";
        }

        // 메시지 로딩
        List<Message> messages = chatService.getMessages(roomNo);

        model.addAttribute("room", room);
        model.addAttribute("messages", messages);
        return "game/gameRoom";
    }

}