package com.mafia.game.message.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mafia.game.member.model.vo.Member;
import com.mafia.game.message.model.service.MessageService;
import com.mafia.game.message.model.vo.UserMessage;

import lombok.extern.slf4j.Slf4j;


@Controller
@RequestMapping("/message")
@Slf4j
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 받은편지함 페이지
     */
    @GetMapping("/inbox")
    public String inbox(@SessionAttribute Member loginUser, Model model) {
        try {
            ArrayList<UserMessage> receivedMessages = messageService.getReceivedMessages(loginUser.getUserName());
            int unreadCount = messageService.getUnreadCount(loginUser.getUserName());
            
            model.addAttribute("messages", receivedMessages);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("messageType", "received");
            
            return "message/messageList";
        } catch (Exception e) {
            log.error("받은편지함 조회 중 오류 발생", e);
            model.addAttribute("msg", "편지함을 불러오는데 실패했습니다.");
            return "common/error";
        }
    }

    /**
     * 보낸편지함 페이지
     */
    @GetMapping("/outbox")
    public String outbox(@SessionAttribute Member loginUser, Model model) {
        try {
            ArrayList<UserMessage> sentMessages = messageService.getSentMessages(loginUser.getUserName());
            
            model.addAttribute("messages", sentMessages);
            model.addAttribute("messageType", "sent");
            
            return "message/messageList";
        } catch (Exception e) {
            log.error("보낸편지함 조회 중 오류 발생", e);
            model.addAttribute("msg", "편지함을 불러오는데 실패했습니다.");
            return "common/error";
        }
    }

    /**
     * 쪽지 상세보기
     */
    @GetMapping("/detail")
    public String messageDetail(@RequestParam int privateMsgNo,
                               @SessionAttribute Member loginUser,
                               Model model) {
        try {
            UserMessage message = messageService.getMessageDetail(privateMsgNo);
            
            if (message == null) {
                model.addAttribute("msg", "존재하지 않는 쪽지입니다.");
                return "common/error";
            }

            // 읽음 처리 (받는 사람만)
            if (message.getReceiverUserName().equals(loginUser.getUserName())) {
                messageService.markAsRead(privateMsgNo, loginUser.getUserName());
            }

            model.addAttribute("message", message);
            return "message/messageDetail";
            
        } catch (Exception e) {
            log.error("쪽지 상세보기 중 오류 발생", e);
            model.addAttribute("msg", "쪽지를 불러오는데 실패했습니다.");
            return "common/error";
        }
    }

    /**
     * 쪽지 작성 폼
     */
    @GetMapping("/write")
    public String writeForm(@RequestParam(required = false) String receiverUserName, Model model) {
        model.addAttribute("receiverUserName", receiverUserName);
        return "message/messageWrite";
    }

    /**
     * 쪽지 보내기
     */
    @PostMapping("/send")
    public String sendMessage(UserMessage message,
                             @SessionAttribute Member loginUser,
                             RedirectAttributes redirectAttributes) {
        try {
            message.setSenderUserName(loginUser.getUserName());
            
            int result = messageService.sendMessage(message);
            
            if (result > 0) {
                redirectAttributes.addFlashAttribute("msg", "쪽지를 성공적으로 보냈습니다.");
                return "redirect:/message/outbox";
            } else {
                redirectAttributes.addFlashAttribute("msg", "쪽지 전송에 실패했습니다.");
                return "redirect:/message/write";
            }
            
        } catch (Exception e) {
            log.error("쪽지 전송 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("msg", "쪽지 전송 중 오류가 발생했습니다.");
            return "redirect:/message/write";
        }
    }

    /**
     * 쪽지 삭제 (AJAX)
     */
    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> deleteMessage(@RequestParam int privateMsgNo,
                                           @RequestParam String deleteType,
                                           @SessionAttribute Member loginUser) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int result = 0;
            
            if ("sender".equals(deleteType)) {
                result = messageService.deleteBySender(privateMsgNo);
            } else if ("receiver".equals(deleteType)) {
                result = messageService.deleteByReceiver(privateMsgNo);
            }
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "쪽지가 삭제되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "쪽지 삭제에 실패했습니다.");
            }
            
        } catch (Exception e) {
            log.error("쪽지 삭제 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "서버 오류가 발생했습니다.");
        }
        
        return response;
    }

    /**
     * 안읽은 쪽지 개수 조회 (AJAX)
     */
    @GetMapping("/unreadCount")
    @ResponseBody
    public Map<String, Object> getUnreadCount(@SessionAttribute Member loginUser) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int unreadCount = messageService.getUnreadCount(loginUser.getUserName());
            response.put("success", true);
            response.put("unreadCount", unreadCount);
            
        } catch (Exception e) {
            log.error("안읽은 쪽지 개수 조회 중 오류 발생", e);
            response.put("success", false);
            response.put("unreadCount", 0);
        }
        
        return response;
    }
}