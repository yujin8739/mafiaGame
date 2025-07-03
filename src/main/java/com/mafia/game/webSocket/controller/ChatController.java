package com.mafia.game.webSocket.controller;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mafia.game.webSocket.model.service.ChatService;
import com.mafia.game.webSocket.model.vo.GameRoom;
import com.mafia.game.webSocket.model.vo.Message;

@Controller
@RequestMapping("/chat")
public class ChatController {

	@Autowired
    private ChatService chatService;

    @GetMapping("/rooms")
    public String listRooms(Model model) {
        model.addAttribute("rooms", chatService.getAllRooms());
        return "chat/roomList";
    }

    @GetMapping("/room/{roonNo}")
    public String enterRoom(@PathVariable int roonNo, Model model) {
        GameRoom room = chatService.getRoom(roonNo);
        List<Message> messages = chatService.getMessages(roonNo);
        model.addAttribute("room", room);
        model.addAttribute("messages", messages);
        return "chat/room";
    }

    @PostMapping("/room/{roonNo}/send")
    public String sendMessage(@PathVariable int roonNo, @ModelAttribute Message message) {
        message.setRoomNo(roonNo);
        message.setChatDate(new Date());
        message.setMsgNo(UUID.randomUUID().toString());
        chatService.sendMessage(message);
        return "redirect:/chat/room/" + roonNo;
    }

    @PostMapping("/room/create")
    public String createRoom(@ModelAttribute GameRoom room) {
        chatService.createRoom(room);
        return "redirect:/chat/rooms";
    }
}
