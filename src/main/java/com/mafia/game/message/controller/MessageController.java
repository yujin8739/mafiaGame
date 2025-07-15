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

@Controller
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;


    /**
     * 로그인 상태 확인
     */
    private boolean isLoggedIn(Member loginUser) {
        return loginUser != null && loginUser.getUserName() != null && !loginUser.getUserName().trim().isEmpty();
    }

    /**
     * messageType 변환 (한글 → DB 값)
     */
    private String convertMessageType(String messageType) {
        if (messageType == null || messageType.trim().isEmpty()) {
            return "PERSONAL"; // 기본값
        }
        
        switch (messageType.trim()) {
            case "답장": return "REPLY";
            case "친구요청": return "FRIEND_REQUEST";
            case "거래": return "TRADE";
            case "게임초대": return "GAME_INVITE";
            case "시스템": return "SYSTEM";
            default: return "PERSONAL";
        }
    }

    /**
     * messageType 변환 (DB 값 → 한글)
     */
    private String convertMessageTypeToKorean(String dbMessageType) {
        if (dbMessageType == null) {
            return "일반";
        }
        
        switch (dbMessageType) {
            case "REPLY": return "답장";
            case "FRIEND_REQUEST": return "친구요청";
            case "TRADE": return "거래";
            case "GAME_INVITE": return "게임초대";
            case "SYSTEM": return "시스템";
            default: return "일반";
        }
    }


    /**
     * 받은편지함
     */
    @GetMapping("/inbox")
    public String inbox(@SessionAttribute(name = "loginUser", required = false) Member loginUser,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        
        if (!isLoggedIn(loginUser)) {
            return "redirect:/login/view";
        }
        
        try {
            String userName = loginUser.getUserName();
            
            // 페이지네이션 설정
            int pageSize = 5;
            int offset = (page - 1) * pageSize;
            
            // 페이지별 메시지 조회
            ArrayList<UserMessage> messages = messageService.getReceivedMessagesWithPaging(userName, offset, pageSize);
            
            // 전체 메시지 개수 조회
            int totalMessages = messageService.getTotalReceivedMessagesCount(userName);
            int totalPages = (int) Math.ceil((double) totalMessages / pageSize);
            
            // 안읽은 메시지 개수
            int unreadCount = messageService.getUnreadCount(userName);
            
            // messageType을 한글로 변환
            for (UserMessage message : messages) {
                message.setMessageType(convertMessageTypeToKorean(message.getMessageType()));
            }
            
            model.addAttribute("messages", messages);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("messageType", "received");
            model.addAttribute("currentUser", loginUser);
            
            // 페이지네이션 정보 추가
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalMessages", totalMessages);
            model.addAttribute("hasNext", page < totalPages);
            model.addAttribute("hasPrevious", page > 1);
            
            return "message/messageList";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("msg", "편지함을 불러오는데 실패했습니다.");
            return "common/error";
        }
    }

    /**
     * 보낸편지함
     */
    @GetMapping("/outbox")
    public String outbox(@SessionAttribute(name = "loginUser", required = false) Member loginUser,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        
        if (!isLoggedIn(loginUser)) {
            return "redirect:/login/view";
        }
        
        try {
            String userName = loginUser.getUserName();
            
            // 페이지네이션 설정
            int pageSize = 5;
            int offset = (page - 1) * pageSize;
            
            // 페이지별 메시지 조회
            ArrayList<UserMessage> messages = messageService.getSentMessagesWithPaging(userName, offset, pageSize);
            
            // 전체 메시지 개수 조회
            int totalMessages = messageService.getTotalSentMessagesCount(userName);
            int totalPages = (int) Math.ceil((double) totalMessages / pageSize);
            
            // messageType을 한글로 변환
            for (UserMessage message : messages) {
                message.setMessageType(convertMessageTypeToKorean(message.getMessageType()));
            }
            
            model.addAttribute("messages", messages);
            model.addAttribute("messageType", "sent");
            model.addAttribute("currentUser", loginUser);
            
            // 페이지네이션 정보 추가
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalMessages", totalMessages);
            model.addAttribute("hasNext", page < totalPages);
            model.addAttribute("hasPrevious", page > 1);
            
            return "message/messageList";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("msg", "편지함을 불러오는데 실패했습니다.");
            return "common/error";
        }
    }

    /**
     * 쪽지 상세보기
     */
    @GetMapping("/detail")
    public String detail(@RequestParam int privateMsgNo,
                        @SessionAttribute(name = "loginUser", required = false) Member loginUser,
                        Model model) {
        
        if (!isLoggedIn(loginUser)) {
            return "redirect:/login/view";
        }
        
        try {
            UserMessage message = messageService.getMessageDetail(privateMsgNo);
            
            if (message == null) {
                model.addAttribute("msg", "존재하지 않는 쪽지입니다.");
                return "common/error";
            }
            
            String userName = loginUser.getUserName();
            
            // 권한 확인 (보낸사람 또는 받은사람인지)
            if (!message.getSenderUserName().equals(userName) && 
                !message.getReceiverUserName().equals(userName)) {
                model.addAttribute("msg", "권한이 없습니다.");
                return "common/error";
            }
            
            // 읽음 처리 (받는 사람만)
            if (message.getReceiverUserName().equals(userName) && message.getReadYn() == 'N') {
                messageService.markAsRead(privateMsgNo, userName);
                // 다시 조회해서 읽음 상태 업데이트
                message = messageService.getMessageDetail(privateMsgNo);
            }
            
            // messageType을 한글로 변환
            message.setMessageType(convertMessageTypeToKorean(message.getMessageType()));
            
            model.addAttribute("message", message);
            model.addAttribute("currentUser", loginUser);
            
            return "message/messageDetail";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("msg", "쪽지를 불러오는데 실패했습니다.");
            return "common/error";
        }
    }

    /**
     * 쪽지 쓰기 폼
     */
    @GetMapping("/write")
    public String writeForm(@RequestParam(required = false) String receiverUserName,
                           @SessionAttribute(name = "loginUser", required = false) Member loginUser,
                           Model model) {
        
        if (!isLoggedIn(loginUser)) {
            return "redirect:/login/view";
        }
        
        model.addAttribute("receiverUserName", receiverUserName);
        model.addAttribute("senderUserName", loginUser.getUserName());
        model.addAttribute("isReply", false);
        model.addAttribute("currentUser", loginUser);
        
        return "message/messageWrite";
    }

    /**
     * 답장 폼
     */
    @GetMapping("/reply")
    public String replyForm(@RequestParam int privateMsgNo,
                           @SessionAttribute(name = "loginUser", required = false) Member loginUser,
                           Model model) {
        
        if (!isLoggedIn(loginUser)) {
            return "redirect:/login/view";
        }
        
        try {
            UserMessage originalMessage = messageService.getMessageDetail(privateMsgNo);
            
            if (originalMessage == null) {
                model.addAttribute("msg", "존재하지 않는 쪽지입니다.");
                return "common/error";
            }
            
            String userName = loginUser.getUserName();
            
            // 받은 사람만 답장 가능
            if (!originalMessage.getReceiverUserName().equals(userName)) {
                model.addAttribute("msg", "답장 권한이 없습니다.");
                return "common/error";
            }
            
            // messageType을 한글로 변환
            originalMessage.setMessageType(convertMessageTypeToKorean(originalMessage.getMessageType()));
            
            model.addAttribute("receiverUserName", originalMessage.getSenderUserName());
            model.addAttribute("replyTitle", "RE: " + originalMessage.getTitle());
            model.addAttribute("originalMessage", originalMessage);
            model.addAttribute("isReply", true);
            model.addAttribute("senderUserName", userName);
            model.addAttribute("currentUser", loginUser);
            
            return "message/messageWrite";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("msg", "답장 폼을 불러오는데 실패했습니다.");
            return "common/error";
        }
    }


    /**
     * 쪽지 보내기
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam String receiverUserName,
                             @RequestParam String title,
                             @RequestParam String content,
                             @RequestParam(required = false, defaultValue = "") String messageType,
                             @RequestParam(required = false, defaultValue = "0") int parentPrivateMsgNo,
                             @SessionAttribute(name = "loginUser", required = false) Member loginUser,
                             RedirectAttributes redirectAttributes) {
        
        if (!isLoggedIn(loginUser)) {
            redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
            return "redirect:/login/view";
        }
        
        // 입력값 검증
        if (receiverUserName == null || receiverUserName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("msg", "받는 사람을 입력해주세요.");
            return "redirect:/message/write";
        }
        
        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("msg", "제목을 입력해주세요.");
            return "redirect:/message/write";
        }
        
        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("msg", "내용을 입력해주세요.");
            return "redirect:/message/write";
        }
        
        // 길이 제한 체크
        if (title.trim().length() > 200) {
            redirectAttributes.addFlashAttribute("msg", "제목은 200자를 초과할 수 없습니다.");
            return "redirect:/message/write";
        }
        
        // 자기 자신에게 보내는지 체크
        if (receiverUserName.trim().equals(loginUser.getUserName())) {
            redirectAttributes.addFlashAttribute("msg", "자기 자신에게는 쪽지를 보낼 수 없습니다.");
            return "redirect:/message/write";
        }
        
        try {
            // UserMessage 객체 생성
            UserMessage message = new UserMessage();
            message.setSenderUserName(loginUser.getUserName());
            message.setReceiverUserName(receiverUserName.trim());
            message.setTitle(title.trim());
            message.setContent(content.trim());
            message.setMessageType("PERSONAL");
            
            // DB 제약조건에 맞는 messageType 설정
            String dbMessageType = convertMessageType(messageType);
            message.setMessageType(dbMessageType);
            
            // parentPrivateMsgNo 설정 (0이면 설정하지 않음)
            if (parentPrivateMsgNo > 0) {
                message.setParentPrivateMsgNo(parentPrivateMsgNo);
            }
            
            // 쪽지 전송
            int result = messageService.sendMessage(message);
            
            if (result > 0) {
                redirectAttributes.addFlashAttribute("successMsg", "쪽지를 성공적으로 보냈습니다.");
                return "redirect:/message/outbox";
            } else {
                redirectAttributes.addFlashAttribute("msg", "쪽지 전송에 실패했습니다.");
                return "redirect:/message/write";
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("msg", "서버 오류: " + e.getMessage());
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
                                           @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!isLoggedIn(loginUser)) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        // 삭제 타입 유효성 검사
        if (!"sender".equals(deleteType) && !"receiver".equals(deleteType)) {
            response.put("success", false);
            response.put("message", "잘못된 삭제 타입입니다.");
            return response;
        }
        
        try {
            // 권한 확인을 위해 쪽지 정보 조회
            UserMessage message = messageService.getMessageDetail(privateMsgNo);
            if (message == null) {
                response.put("success", false);
                response.put("message", "존재하지 않는 쪽지입니다.");
                return response;
            }
            
            String userName = loginUser.getUserName();
            int result = 0;
            
            if ("sender".equals(deleteType)) {
                if (!message.getSenderUserName().equals(userName)) {
                    response.put("success", false);
                    response.put("message", "삭제 권한이 없습니다.");
                    return response;
                }
                result = messageService.deleteBySender(privateMsgNo);
            } else { // "receiver"
                if (!message.getReceiverUserName().equals(userName)) {
                    response.put("success", false);
                    response.put("message", "삭제 권한이 없습니다.");
                    return response;
                }
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
            e.printStackTrace();
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
    public Map<String, Object> getUnreadCount(@SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!isLoggedIn(loginUser)) {
            response.put("success", false);
            response.put("unreadCount", 0);
            return response;
        }
        
        try {
            int unreadCount = messageService.getUnreadCount(loginUser.getUserName());
            response.put("success", true);
            response.put("unreadCount", unreadCount);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("unreadCount", 0);
        }
        
        return response;
    }
}