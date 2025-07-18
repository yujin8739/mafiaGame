package com.mafia.game.friend.controller;

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

import com.mafia.game.friend.model.service.FriendService;
import com.mafia.game.friend.model.vo.FriendList;
import com.mafia.game.friend.model.vo.FriendRelation;
import com.mafia.game.friend.model.vo.GameInvite;
import com.mafia.game.member.model.vo.Member;

@Controller
@RequestMapping("/friend")
public class FriendController {
    
    @Autowired
    private FriendService friendService;
    
    /**
     * 친구 목록 페이지
     */
    @GetMapping("/list")
    public String friendList(@SessionAttribute(name = "loginUser", required = false) Member loginUser, 
                           Model model) {
        
        if (loginUser == null) {
            return "redirect:/login/view";
        }
        
        try {
            // 친구 목록 조회
            ArrayList<FriendList> friendList = friendService.getFriendList(loginUser.getUserName());
            
            // 친구 요청 목록 조회 (받은 요청)
            ArrayList<FriendRelation> pendingRequests = friendService.getPendingRequests(loginUser.getUserName());
            
            model.addAttribute("friendList", friendList);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("currentUser", loginUser);
            
            return "friend/friendList";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("msg", "친구 목록을 불러오는데 실패했습니다.");
            return "common/error";
        }
    }
    
    /**
     * 친구 검색 (AJAX)
     */
    @PostMapping("/search")
    @ResponseBody
    public Map<String, Object> searchFriend(@RequestParam String searchKeyword,
                                          @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        try {
            // 사용자 검색 (아이디 또는 닉네임)
            Member searchResult = friendService.searchUser(searchKeyword);
            
            if (searchResult == null) {
                response.put("success", false);
                response.put("message", "존재하지 않는 사용자입니다.");
            } else if (searchResult.getUserName().equals(loginUser.getUserName())) {
                response.put("success", false);
                response.put("message", "자기 자신은 친구로 추가할 수 없습니다.");
            } else {
                // 이미 친구인지 확인
                boolean isAlreadyFriend = friendService.isAlreadyFriend(loginUser.getUserName(), searchResult.getUserName());
                
                if (isAlreadyFriend) {
                    response.put("success", false);
                    response.put("message", "이미 친구입니다.");
                } else {
                    response.put("success", true);
                    response.put("user", searchResult);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "검색 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 친구 요청 보내기 (AJAX) - 수정됨
     */
    @PostMapping("/request")
    @ResponseBody
    public Map<String, Object> sendFriendRequest(@RequestParam String friendUserName,
                                               @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        try {
            // 중복 요청 확인
            boolean hasRequest = friendService.hasRequestBetween(loginUser.getUserName(), friendUserName);
            
            if (hasRequest) {
                response.put("success", false);
                response.put("message", "이미 친구 요청을 보냈거나 받은 상대입니다.");
            } else {
                // 친구 요청 생성 - 수정된 부분
                FriendRelation friendRequest = new FriendRelation();
                friendRequest.setRequesterName(loginUser.getUserName());
                friendRequest.setReceiverName(friendUserName);
                friendRequest.setReceiverStatus("PENDING");
                
                int result = friendService.sendFriendRequest(friendRequest);
                
                if (result > 0) {
                    response.put("success", true);
                    response.put("message", "친구 요청을 보냈습니다.");
                } else {
                    response.put("success", false);
                    response.put("message", "친구 요청 전송에 실패했습니다.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "친구 요청 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 친구 요청 수락 (AJAX)
     */
    @PostMapping("/accept")
    @ResponseBody
    public Map<String, Object> acceptFriendRequest(@RequestParam int relationNo,
                                                 @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        try {
            int result = friendService.acceptFriendRequest(relationNo, loginUser.getUserName());
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "친구 요청을 수락했습니다.");
            } else {
                response.put("success", false);
                response.put("message", "친구 요청 수락에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "친구 요청 처리 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 친구 요청 거절 (AJAX)
     */
    @PostMapping("/reject")
    @ResponseBody
    public Map<String, Object> rejectFriendRequest(@RequestParam int relationNo,
                                                 @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        try {
            int result = friendService.rejectFriendRequest(relationNo, loginUser.getUserName());
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "친구 요청을 거절했습니다.");
            } else {
                response.put("success", false);
                response.put("message", "친구 요청 거절에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "친구 요청 처리 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 친구 삭제 (AJAX)
     */
    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> deleteFriend(@RequestParam String friendUserName,
                                          @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        try {
            int result = friendService.deleteFriend(loginUser.getUserName(), friendUserName);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "친구를 삭제했습니다.");
            } else {
                response.put("success", false);
                response.put("message", "친구 삭제에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "친구 삭제 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 게임방 초대 (AJAX) - 수정됨
     */
    @PostMapping("/invite")
    @ResponseBody
    public Map<String, Object> inviteToGame(@RequestParam String friendUserName,
                                          @RequestParam int roomNo, // String에서 int로 변경
                                          @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        try {
            // 게임 초대 생성
            GameInvite gameInvite = new GameInvite();
            gameInvite.setSenderName(loginUser.getUserName());
            gameInvite.setReceiverName(friendUserName);
            gameInvite.setRoomNo(roomNo); // 이미 int 타입이므로 바로 설정
            gameInvite.setInviteStatus("PENDING");
            
            int result = friendService.sendGameInvite(gameInvite);
            
            if (result > 0) {
                response.put("success", true);
                response.put("message", "게임 초대를 보냈습니다.");
            } else {
                response.put("success", false);
                response.put("message", "게임 초대 전송에 실패했습니다.");
            }
            
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "잘못된 방 번호입니다.");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "게임 초대 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 게임 초대 응답 (AJAX) - 새로 추가
     */
    @PostMapping("/invite/respond")
    @ResponseBody
    public Map<String, Object> respondGameInvite(@RequestParam int inviteNo,
                                               @RequestParam String status,
                                               @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }
        
        try {
            int result = friendService.respondGameInvite(inviteNo, status, loginUser.getUserName());
            
            if (result > 0) {
                String message = "ACCEPTED".equals(status) ? "게임 초대를 수락했습니다." : "게임 초대를 거절했습니다.";
                response.put("success", true);
                response.put("message", message);
            } else {
                response.put("success", false);
                response.put("message", "게임 초대 응답에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "게임 초대 응답 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 알림 조회 (AJAX)
     */
    @GetMapping("/notifications")
    @ResponseBody
    public Map<String, Object> getNotifications(@SessionAttribute(name = "loginUser", required = false) Member loginUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("notifications", new ArrayList<>());
            return response;
        }
        
        try {
            // 친구 요청 알림
            ArrayList<FriendRelation> friendRequests = friendService.getPendingRequests(loginUser.getUserName());
            
            // 게임 초대 알림
            ArrayList<GameInvite> gameInvites = friendService.getPendingGameInvites(loginUser.getUserName());
            
            response.put("success", true);
            response.put("friendRequests", friendRequests);
            response.put("gameInvites", gameInvites);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("friendRequests", new ArrayList<>());
            response.put("gameInvites", new ArrayList<>());
        }
        
        return response;
    }
}