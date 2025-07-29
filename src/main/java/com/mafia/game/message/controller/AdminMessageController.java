package com.mafia.game.message.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.message.model.service.MessageService;
import com.mafia.game.message.model.vo.UserMessage;

@RestController
@RequestMapping("/api")
public class AdminMessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 관리자 권한 확인
     */
    private boolean isAdmin(Member loginUser) {
        // 실제 구현에서는 권한 체크 로직 추가
        return loginUser != null && loginUser.getUserName() != null;
    }

    /**
     * 전체 쪽지 목록 조회 (관리자용)
     */
    @GetMapping("/messages")
    public ResponseEntity<?> getMessageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

        Map<String, Object> response = new HashMap<>();

        // 관리자 권한 체크
        if (!isAdmin(loginUser)) {
            response.put("success", false);
            response.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            // 페이징 계산
            int offset = (page - 1) * size;

            // 전체 쪽지 조회 (관리자는 모든 쪽지 조회 가능)
            ArrayList<UserMessage> messages = messageService.getAllMessagesForAdmin(offset, size);
            
            // 전체 쪽지 개수
            int totalCount = messageService.getTotalMessagesCount();
            
            // 페이지 정보 계산
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            PageInfo pageInfo = new PageInfo();
            pageInfo.setCurrentPage(page);
            pageInfo.setListCount(totalCount);
            pageInfo.setBoardLimit(size);
            pageInfo.setPageLimit(5);
            pageInfo.setMaxPage(totalPages);
            
            response.put("success", true);
            response.put("messages", messages);
            response.put("pageInfo", pageInfo);
            response.put("totalCount", totalCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "쪽지 목록 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 쪽지 상세 조회 (관리자용)
     */
    @GetMapping("/messages/{privateMsgNo}")
    public ResponseEntity<?> getMessageDetail(
            @PathVariable int privateMsgNo,
            @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

        Map<String, Object> response = new HashMap<>();

        if (!isAdmin(loginUser)) {
            response.put("success", false);
            response.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            UserMessage message = messageService.getMessageDetail(privateMsgNo);
            
            if (message == null) {
                response.put("success", false);
                response.put("message", "존재하지 않는 쪽지입니다.");
                return ResponseEntity.status(404).body(response);
            }

            response.put("success", true);
            response.put("message", message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "쪽지 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 쪽지 검색/필터링 (관리자용)
     */
    @GetMapping("/messages/search")
    public ResponseEntity<?> searchMessages(
            @RequestParam(required = false) String senderUserName,
            @RequestParam(required = false) String receiverUserName,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String readYn,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

        Map<String, Object> response = new HashMap<>();

        if (!isAdmin(loginUser)) {
            response.put("success", false);
            response.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            // 검색 조건 맵 생성
            Map<String, Object> searchParams = new HashMap<>();
            if (senderUserName != null && !senderUserName.trim().isEmpty()) {
                searchParams.put("senderUserName", senderUserName.trim());
            }
            if (receiverUserName != null && !receiverUserName.trim().isEmpty()) {
                searchParams.put("receiverUserName", receiverUserName.trim());
            }
            if (title != null && !title.trim().isEmpty()) {
                searchParams.put("title", title.trim());
            }
            if (messageType != null && !messageType.trim().isEmpty()) {
                searchParams.put("messageType", messageType.trim());
            }
            if (readYn != null && !readYn.trim().isEmpty()) {
                searchParams.put("readYn", readYn.trim());
            }

            int offset = (page - 1) * size;
            searchParams.put("offset", offset);
            searchParams.put("size", size);

            // 검색 실행
            ArrayList<UserMessage> messages = messageService.searchMessagesForAdmin(searchParams);
            int totalCount = messageService.getSearchMessagesCount(searchParams);
            
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            PageInfo pageInfo = new PageInfo();
            pageInfo.setCurrentPage(page);
            pageInfo.setListCount(totalCount);
            pageInfo.setBoardLimit(size);
            pageInfo.setMaxPage(totalPages);

            response.put("success", true);
            response.put("messages", messages);
            response.put("pageInfo", pageInfo);
            response.put("totalCount", totalCount);
            response.put("searchParams", searchParams);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "검색에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 쪽지 강제 삭제 (관리자용)
     */
    @DeleteMapping("/messages/{privateMsgNo}")
    public ResponseEntity<?> forceDeleteMessage(
            @PathVariable int privateMsgNo,
            @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

        Map<String, Object> response = new HashMap<>();

        if (!isAdmin(loginUser)) {
            response.put("success", false);
            response.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            // 관리자는 완전 삭제 가능
            int result = messageService.forceDeleteMessage(privateMsgNo);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "쪽지가 완전 삭제되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "삭제할 쪽지가 없습니다.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 쪽지 수정 (관리자용)
     */
    @PutMapping("/messages/{privateMsgNo}")
    public ResponseEntity<?> updateMessage(
            @PathVariable int privateMsgNo,
            @RequestBody UserMessage updateMessage,
            @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

        Map<String, Object> response = new HashMap<>();

        if (!isAdmin(loginUser)) {
            response.put("success", false);
            response.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            // 기존 쪽지 확인
            UserMessage existingMessage = messageService.getMessageDetail(privateMsgNo);
            if (existingMessage == null) {
                response.put("success", false);
                response.put("message", "존재하지 않는 쪽지입니다.");
                return ResponseEntity.status(404).body(response);
            }

            // 수정할 내용 설정
            updateMessage.setPrivateMsgNo(privateMsgNo);
            
            int result = messageService.updateMessageByAdmin(updateMessage);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "쪽지가 수정되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "쪽지 수정에 실패했습니다.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "수정에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 일괄 삭제 (관리자용)
     */
    @PostMapping("/messages/bulk-delete")
    public ResponseEntity<?> bulkDeleteMessages(
            @RequestBody Map<String, Object> requestData,
            @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

        Map<String, Object> response = new HashMap<>();

        if (!isAdmin(loginUser)) {
            response.put("success", false);
            response.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            @SuppressWarnings("unchecked")
            ArrayList<Integer> messageIds = (ArrayList<Integer>) requestData.get("messageIds");
            
            if (messageIds == null || messageIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "삭제할 쪽지를 선택해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            int deletedCount = messageService.bulkDeleteMessages(messageIds);
            
            response.put("success", true);
            response.put("message", deletedCount + "개의 쪽지가 삭제되었습니다.");
            response.put("deletedCount", deletedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "일괄 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 사용자 쪽지 기능 차단 (관리자용)
     */
    @PostMapping("/messages/block-user")
    public ResponseEntity<?> blockUserMessage(
            @RequestBody Map<String, Object> requestData,
            @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

        Map<String, Object> response = new HashMap<>();

        if (!isAdmin(loginUser)) {
            response.put("success", false);
            response.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(response);
        }

        try {
            String targetUserName = (String) requestData.get("userName");
            Integer blockDays = (Integer) requestData.get("blockDays");
            String reason = (String) requestData.get("reason");

            if (targetUserName == null || targetUserName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "차단할 사용자를 지정해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            // 쪽지 차단 처리 (시스템 쪽지로 기록)
            int result = messageService.blockUserMessage(targetUserName.trim(), blockDays, reason, loginUser.getUserName());
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "사용자 쪽지 기능이 차단되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "차단 처리에 실패했습니다.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "차단 처리에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}