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

    @GetMapping("/createRoom")
    public String createRoomForm() {
        return "game/createRoom";
    }

    @PostMapping("/create")
    public String createRoom(@ModelAttribute GameRoom room, Model model) {
        int result = gameRoomService.createRoom(room);

        return "redirect:/chat/room/"+room.getRoomNo();
    }
    
    @GetMapping("/listRoom")
    public String listRooms(Model model) {
        model.addAttribute("rooms", gameRoomService.getAllRooms());
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
        
        //방에 비밀번호가 존재할때 비밀번호 확인 
        //비밀번호가 틀리면 홈으로 이동
        if(room.getPassword() != null 
        		&& !room.getPassword().isEmpty()
        		&& !room.getPassword().equals(password)) {
        	 redirectAttributes.addFlashAttribute("msg","게임방의 비밀번호가 틀렸습니다.");
            return "redirect:/";
        }

        // 현재 로그인한 사용자 정보
        Member loginUser = (Member) session.getAttribute("loginUser");
        String userName = loginUser.getUserName();

        // userList 파싱
        List<String> users = new ArrayList<>();
        try {
            if (room.getUserList() != null && !room.getUserList().isEmpty()) {
                users = new ObjectMapper().readValue(room.getUserList(), new TypeReference<List<String>>() {});
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