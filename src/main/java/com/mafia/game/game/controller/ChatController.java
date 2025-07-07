package com.mafia.game.game.controller;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.vo.Message;



@Controller
@RequestMapping("/chat")
public class ChatController {

	@Autowired
    private ChatService chatService;
	
    @PostMapping("/room/{roomNo}/send")
    public String sendMessage(@PathVariable int roomNo, @ModelAttribute Message message) {
        message.setRoomNo(roomNo);
        message.setChatDate(new Date());
        message.setMsgNo(UUID.randomUUID().toString());
        chatService.sendMessage(message);
        return "redirect:/chat/room/" + roomNo;
    }
    
    @GetMapping("/selectEvent")
    @ResponseBody
    public String selectEvent(@RequestParam int eventNo, 
    						  @RequestParam String userName) {
    	return chatService.selectEvent(eventNo, userName);
    }
}
