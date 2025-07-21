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
	public String friendList(@SessionAttribute(name = "loginUser", required = false) Member loginUser, Model model) {

		// 로그인 체크
		if (loginUser == null) {
			return "redirect:/login/view";
		}

		// 친구 목록 가져오기
		ArrayList<FriendList> friendList = friendService.getFriendList(loginUser.getUserName());

		// 받은 친구 요청 가져오기
		ArrayList<FriendRelation> friendRequests = friendService.getPendingRequests(loginUser.getUserName());

		// 받은 게임 초대 가져오기
		ArrayList<GameInvite> gameInvites = friendService.getPendingGameInvites(loginUser.getUserName());

		model.addAttribute("friendList", friendList);
		model.addAttribute("friendRequests", friendRequests);
		model.addAttribute("gameInvites", gameInvites);
		model.addAttribute("currentUser", loginUser);

		return "friend/friendList";
	}

	/**
	 * 친구 검색하기 (모달용)
	 */
	@PostMapping("/search")
	@ResponseBody
	public Map<String, Object> searchFriend(@RequestParam String searchKeyword,
			@SessionAttribute(name = "loginUser", required = false) Member loginUser) {

		Map<String, Object> result = new HashMap<>();

		// 로그인 체크
		if (loginUser == null) {
			result.put("success", false);
			result.put("message", "로그인이 필요합니다.");
			return result;
		}

		// 사용자 검색
		Member foundUser = friendService.searchUser(searchKeyword);

		if (foundUser == null) {
			result.put("success", false);
			result.put("message", "사용자를 찾을 수 없습니다.");
		} else {
			result.put("success", true);
			result.put("user", foundUser);
		}

		return result;
	}

	/**
	 * 친구 요청 보내기
	 */
	@PostMapping("/request")
	@ResponseBody
	public Map<String, Object> sendFriendRequest(@RequestParam String targetUserName,
	                                           @SessionAttribute(name = "loginUser", required = false) Member loginUser) {
	    
	    Map<String, Object> result = new HashMap<>();
	    
	    if (loginUser == null) {
	        result.put("success", false);
	        result.put("message", "로그인이 필요합니다.");
	        return result;
	    }
	    
	    if (loginUser.getUserName().equals(targetUserName)) {
	        result.put("success", false);
	        result.put("message", "자기 자신에게는 친구 요청을 보낼 수 없습니다.");
	        return result;
	    }
	    
	    try {
	        // 기존 친구 관계/요청 여부 체크
	        boolean alreadyExists = friendService.checkExistingRelation(loginUser.getUserName(), targetUserName);
	        if (alreadyExists) {
	            result.put("success", false);
	            result.put("message", "이미 친구 요청을 보냈거나 친구 관계입니다.");
	            return result;
	        }
	        
	        FriendRelation friendRequest = new FriendRelation();
	        friendRequest.setRequesterName(loginUser.getUserName());
	        friendRequest.setReceiverName(targetUserName);
	        friendRequest.setReceiverStatus("PENDING");
	        
	        int success = friendService.sendFriendRequest(friendRequest);
	        
	        if (success > 0) {
	            result.put("success", true);
	            result.put("message", "친구 요청을 보냈습니다.");
	        } else {
	            result.put("success", false);
	            result.put("message", "친구 요청에 실패했습니다.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("message", "이미 친구 요청을 보냈거나 친구 관계입니다.");
	    }
	    
	    return result;
	}

	/**
	 * 친구 요청 수락
	 */
	@PostMapping("/accept")
	@ResponseBody
	public Map<String, Object> acceptFriendRequest(@RequestParam int relationNo,
			@SessionAttribute(name = "loginUser", required = false) Member loginUser) {

		Map<String, Object> result = new HashMap<>();

		// 로그인 체크
		if (loginUser == null) {
			result.put("success", false);
			result.put("message", "로그인이 필요합니다.");
			return result;
		}

		try {
			// 친구 요청 수락
			int success = friendService.acceptFriendRequest(relationNo, loginUser.getUserName());

			if (success > 0) {
				result.put("success", true);
				result.put("message", "친구 요청을 수락했습니다.");
			} else {
				result.put("success", false);
				result.put("message", "친구 요청 수락에 실패했습니다. (권한이 없거나 이미 처리된 요청입니다)");
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "오류가 발생했습니다: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 친구 요청 거절
	 */
	@PostMapping("/reject")
	@ResponseBody
	public Map<String, Object> rejectFriendRequest(@RequestParam int relationNo,
			@SessionAttribute(name = "loginUser", required = false) Member loginUser) {

		Map<String, Object> result = new HashMap<>();

		// 로그인 체크
		if (loginUser == null) {
			result.put("success", false);
			result.put("message", "로그인이 필요합니다.");
			return result;
		}

		// 친구 요청 거절
		int success = friendService.rejectFriendRequest(relationNo, loginUser.getUserName());

		if (success > 0) {
			result.put("success", true);
			result.put("message", "친구 요청을 거절했습니다.");
		} else {
			result.put("success", false);
			result.put("message", "친구 요청 거절에 실패했습니다.");
		}

		return result;
	}

	/**
	 * 친구 삭제
	 */
	@PostMapping("/delete")
	@ResponseBody
	public Map<String, Object> deleteFriend(@RequestParam String friendUserName,
			@SessionAttribute(name = "loginUser", required = false) Member loginUser) {

		Map<String, Object> result = new HashMap<>();

		// 로그인 체크
		if (loginUser == null) {
			result.put("success", false);
			result.put("message", "로그인이 필요합니다.");
			return result;
		}

		// 친구 삭제
		int success = friendService.deleteFriend(loginUser.getUserName(), friendUserName);

		if (success > 0) {
			result.put("success", true);
			result.put("message", "친구를 삭제했습니다.");
		} else {
			result.put("success", false);
			result.put("message", "친구 삭제에 실패했습니다.");
		}

		return result;
	}

	/**
	 * 게임방 초대 보내기
	 */
	@PostMapping("/invite")
	@ResponseBody
	public Map<String, Object> inviteToGame(@RequestParam String inviteUserName, @RequestParam int roomNo,
	        @SessionAttribute(name = "loginUser", required = false) Member loginUser) {

	    Map<String, Object> result = new HashMap<>();

	    // 로그인 체크
	    if (loginUser == null) {
	        result.put("success", false);
	        result.put("message", "로그인이 필요합니다.");
	        return result;
	    }

	    try {
	        // 1. 친구 여부 확인
	        boolean isFriend = friendService.checkFriendship(loginUser.getUserName(), inviteUserName);
	        if (!isFriend) {
	            result.put("success", false);
	            result.put("message", "친구만 초대할 수 있습니다.");
	            return result;
	        }

	        // 2. 중복 초대 방지
	        boolean alreadyInvited = friendService.checkExistingGameInvite(inviteUserName, roomNo);
	        if (alreadyInvited) {
	            result.put("success", false);
	            result.put("message", "이미 해당 방에 초대를 보냈습니다.");
	            return result;
	        }

	        // 3. 게임방 검증 (방 상태, 인원, 권한 한번에 체크)
	        String validationMessage = friendService.validateGameRoom(roomNo, loginUser.getUserName());
	        if (validationMessage != null) {
	            result.put("success", false);
	            result.put("message", validationMessage);
	            return result;
	        }

	        // 모든 검증 통과 후 게임 초대 생성
	        GameInvite gameInvite = new GameInvite();
	        gameInvite.setSenderName(loginUser.getUserName());
	        gameInvite.setReceiverName(inviteUserName);
	        gameInvite.setRoomNo(roomNo);
	        gameInvite.setInviteStatus("PENDING");

	        int success = friendService.sendGameInvite(gameInvite);

	        if (success > 0) {
	            result.put("success", true);
	            result.put("message", "게임 초대를 보냈습니다.");
	        } else {
	            result.put("success", false);
	            result.put("message", "게임 초대에 실패했습니다.");
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("message", "게임 초대 중 오류가 발생했습니다.");
	    }

	    return result;
	}

	/**
	 * 게임 초대 응답 (수락/거절)
	 */
	@PostMapping("/invite/respond")
	@ResponseBody
	public Map<String, Object> respondGameInvite(@RequestParam int inviteNo, @RequestParam String status,
			@SessionAttribute(name = "loginUser", required = false) Member loginUser) {

		Map<String, Object> result = new HashMap<>();

		// 로그인 체크
		if (loginUser == null) {
			result.put("success", false);
			result.put("message", "로그인이 필요합니다.");
			return result;
		}

		// 게임 초대 응답
		int success = friendService.respondGameInvite(inviteNo, status, loginUser.getUserName());

		if (success > 0) {
			String message = "ACCEPTED".equals(status) ? "게임 초대를 수락했습니다." : "게임 초대를 거절했습니다.";
			result.put("success", true);
			result.put("message", message);
		} else {
			result.put("success", false);
			result.put("message", "게임 초대 응답에 실패했습니다.");
		}

		return result;
	}

	/**
	 * 알림 조회 (친구 요청 + 게임 초대 + 친구 목록 포함)
	 */
	@GetMapping("/notifications")
	@ResponseBody
	public Map<String, Object> getNotifications(
			@SessionAttribute(name = "loginUser", required = false) Member loginUser) {

		Map<String, Object> result = new HashMap<>();

		// 로그인 체크
		if (loginUser == null) {
			result.put("success", false);
			result.put("friendList", new ArrayList<>());
			result.put("friendRequests", new ArrayList<>());
			result.put("gameInvites", new ArrayList<>());
			return result;
		}

		// 친구 목록
		ArrayList<FriendList> friendList = friendService.getFriendList(loginUser.getUserName());

		// 친구 요청 알림
		ArrayList<FriendRelation> friendRequests = friendService.getPendingRequests(loginUser.getUserName());

		// 게임 초대 알림
		ArrayList<GameInvite> gameInvites = friendService.getPendingGameInvites(loginUser.getUserName());

		result.put("success", true);
		result.put("friendList", friendList);
		result.put("friendRequests", friendRequests);
		result.put("gameInvites", gameInvites);

		return result;
	}
}