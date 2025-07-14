package com.mafia.game.game.controller;

import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.game.model.vo.Vote;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/room")
public class GameRoomController {

    @Autowired
    private GameRoomService gameRoomService;
    
	@Autowired
    private ChatService chatService;
	
	@Autowired
	private MemberService memberService;
    
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
        System.out.println(room.getPassword());
        if (result > 0) {
            redirectAttributes.addFlashAttribute("msg", "방이 성공적으로 생성되었습니다!");
            System.out.println(room.getPassword());
            if(room.getPassword()==null || room.getPassword().isEmpty()) {
            	return "redirect:/room/"+room.getRoomNo()+"/"+"0000";
            } else {
            	return "redirect:/room/"+room.getRoomNo()+"/"+room.getPassword();	
            }
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

        model.addAttribute("room", room);
        return "game/gameRoom";
    }
    
    @GetMapping("/loadMessage")
    @ResponseBody
    public List<Message> loadMessage(@RequestParam int roomNo,
    	    						 @RequestParam int page,
    	    						 @RequestParam int size,
    	    						 @RequestParam String job) {
    	String type = "chat";
    	System.out.println("직업========================"+job);
    	switch(job) {
    		case "ghost":case "mafiaGhost":case "spiritualists": type = "death"; break;
    		case "mafia": type = "mafia"; break;
    	}
    	
    	if(type.equals("chat")) {
    		int offset = (page - 1) * size;
    		RowBounds rowBounds = new RowBounds(offset, size); 
    		return chatService.getMessages(roomNo, rowBounds);		
    	} else {
    		int offset = (page - 1) * size;
    		RowBounds rowBounds = new RowBounds(offset, size); 
    		return chatService.getMessages(roomNo, type, rowBounds);		
    	}
    }
    
    @GetMapping("/readyCount")
    @ResponseBody
    public int getReadyCount(@RequestParam int roomNo) {
    	String readyUsers = gameRoomService.getReadyCount(roomNo);
    	List<String> users = new ArrayList<>();
        try {
            users = new ObjectMapper().readValue(readyUsers, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace(); // JSON 파싱 실패 시 빈 리스트 유지
        }
    	return users.size();
    }
    
    @GetMapping("/roomReloadToUserLoad")
    @ResponseBody
    public GameRoom roomReloadToUserLoad(@RequestParam int roomNo) {
    	return gameRoomService.selectRoom(roomNo);
    }
    
    @GetMapping("/reloadRoom")
    @ResponseBody
    public GameRoom reloadRoom(@RequestParam int roomNo, int dayNo) {
    	gameRoomService.updateDayNo(roomNo, dayNo);
    	return gameRoomService.selectRoom(roomNo);
    }
    
    @GetMapping("/userNickList")
    @ResponseBody
    public List<String> userNickList (@RequestParam String userList){
    	return memberService.getUserNickList(userList);
    }
    
    @GetMapping("/getJob")
    @ResponseBody
    public Job getJob(@RequestParam int roomNo, HttpSession session,
    				RedirectAttributes redirectAttributes) {
        Member loginUser = (Member) session.getAttribute("loginUser");
        if (loginUser == null) {
            redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
            return null;
        }
        String userName = loginUser.getUserName();
        System.out.println(">> 로그인 유저: " + userName);
        Map<String, Object> result = gameRoomService.getRoomJob(roomNo,userName);
        String userListJson = clobToString((Clob) result.get("USERLIST"));
        String jobJson = (String) result.get("JOB");

        ObjectMapper mapper = new ObjectMapper();
        try {
        	if(userListJson != null &&jobJson != null) {
        		System.out.println(">> userListJson: " + userListJson);
        		 System.out.println(">> jobJson: " + jobJson);
	        	List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {});
				List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {});
				
				int index = userList.indexOf(userName);
				int myJob = jobList.get(index);
								
				return gameRoomService.getJobDetail(myJob);
        	} else {
        		return null;
        	}
			
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
    
    @GetMapping("/userDeathList")
    @ResponseBody
    public List<String> getDeathList (@RequestParam int roomNo) {
    	return gameRoomService.getDeathList(roomNo);
    }
    
    @GetMapping("/voteUser")
    @ResponseBody
    public Vote voteUser (@RequestParam int roomNo, @RequestParam int dayNo, @RequestParam String targetName) {
        
    	Vote vote = gameRoomService.selectVote(roomNo,dayNo);
        
        if (vote == null) { 
        	vote = new Vote(roomNo,dayNo,"[]");
        }
        
        List<String> votes = new ArrayList<>();
		
        try {
        	votes = new ObjectMapper().readValue(vote.getVote(), new TypeReference<List<String>>() {});
        	votes.add(targetName);
        	
        	String updatedList = new ObjectMapper().writeValueAsString(votes);
        	gameRoomService.updateVote(roomNo, dayNo, updatedList);
        	
        	return gameRoomService.selectVote(roomNo,dayNo);
        } catch (Exception e) {
            e.printStackTrace(); // JSON 파싱 실패 시 빈 리스트 유지
        }
        return null;
    }
    
    public static String clobToString(Clob clob) {
    	if (clob == null) return null;
    	
    	try (Reader reader = clob.getCharacterStream();
    			StringWriter writer = new StringWriter()) {
    		char[] buffer = new char[2048];
    		int length;
    		while ((length = reader.read(buffer)) != -1) {
    			writer.write(buffer, 0, length);
    		}
    		return writer.toString();
    	} catch (Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    }

}