package com.mafia.game.game.controller;

import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;
import com.mafia.game.game.model.service.ChatService;
import com.mafia.game.game.model.service.GameRoomService;
import com.mafia.game.game.model.service.GameRoomServiceImpl;
import com.mafia.game.game.model.service.RoomHintService;
import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Kill;
import com.mafia.game.game.model.vo.Message;
import com.mafia.game.game.model.vo.RoomHint;
import com.mafia.game.job.model.vo.Job;
import com.mafia.game.member.model.service.MemberService;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.webSocket.server.GameRoomManager;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/room")
public class GameRoomController {

	private final GameRoomServiceImpl gameRoomServiceImpl;

	@Autowired
	private GameRoomService gameRoomService;

	@Autowired
	private ChatService chatService;

	@Autowired
	private MemberService memberService;

	@Autowired
	private RoomHintService roomHintService;
	
	@Autowired
    private GameRoomManager gameRoomManager;

	private final ObjectMapper objectMapper = new ObjectMapper();

	GameRoomController(GameRoomServiceImpl gameRoomServiceImpl) {
		this.gameRoomServiceImpl = gameRoomServiceImpl;
	}

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
			if (room.getPassword() == null || room.getPassword().isEmpty()) {
				return "redirect:/room/" + room.getRoomNo() + "/" + "0000";
			} else {
				return "redirect:/room/" + room.getRoomNo() + "/" + room.getPassword();
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
					List<String> users = objectMapper.readValue(room.getUserList(), new TypeReference<List<String>>() {
					});
					userCount = users.size();
				} catch (Exception e) {
					System.err.println("JSON 파싱 실패 - 방번호: " + room.getRoomNo() + ", userList: " + room.getUserList()
							+ ", 에러: " + e.getMessage());
					userCount = 0;
				}
			}
			room.setCurrentUserCount(userCount);
		}

		model.addAttribute("rooms", rooms);
		return "chat/roomList";
	}

	/**
	 * 페이징된 방 목록을 JSON으로 반환하는 API (DB에서 직접 페이징 처리)
	 * 
	 * @param page 페이지 번호 (기본값: 1)
	 * @param size 페이지 크기 (기본값: 5)
	 * @return JSON 형태의 방 목록과 페이징 정보
	 */
	@GetMapping("/api/list")
	@ResponseBody
	public Map<String, Object> getRoomListAPI(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {

		// 1. 전체 방 개수 조회
		int totalRooms = gameRoomService.getTotalRoomCount();

		// 2. 페이징 계산
		int totalPages = (int) Math.ceil((double) totalRooms / size);
		int offset = (page - 1) * size;

		// 3. 페이징된 방 목록 조회 (DB에서 직접 처리)
		List<GameRoom> rooms = gameRoomService.getRoomsPaged(offset, size);

		// 4. 각 방의 현재 인원수 계산 및 상태 설정
		for (GameRoom room : rooms) {
			int userCount = calculateUserCount(room.getUserList());
			room.setCurrentUserCount(userCount);

			// 게임 상태 설정 (대기중/게임중)
			if (room.getIsGaming() != null && room.getIsGaming().equals("Y")) {
				room.setIsGaming("게임중");
			} else {
				room.setIsGaming("대기중");
			}
		}

		// 5. 결과 반환
		Map<String, Object> result = new HashMap<>();
		result.put("rooms", rooms);
		result.put("currentPage", page);
		result.put("totalPages", totalPages);
		result.put("totalRooms", totalRooms);
		result.put("pageSize", size);

		return result;
	}

	/**
	 * 검색/필터링 방목록 조회
	 */
	@GetMapping("/api/search")
	@ResponseBody
	public Map<String, Object> searchRoomsAPI(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "5") int size, @RequestParam(defaultValue = "전체") String type,
			@RequestParam(defaultValue = "전체") String status, @RequestParam(defaultValue = "") String keyword) {

		Map<String, Object> searchParams = new HashMap<>();

		// 방 종류 변환
		if (!"전체".equals(type)) {
			searchParams.put("type", type); // "일반", "친선" 그대로 사용
		}

		// 방 상태 변환
		if (!"전체".equals(status)) {
			if ("게임중".equals(status)) {
				searchParams.put("isGaming", "Y");
			} else if ("대기중".equals(status)) {
				searchParams.put("isGaming", "N");
			}
		}

		// 검색어 처리
		if (!keyword.trim().isEmpty()) {
			searchParams.put("keyword", keyword.trim());
		}

		// 2. 페이징 계산
		int offset = (page - 1) * size;
		searchParams.put("offset", offset);
		searchParams.put("limit", size);

		// 3. 필터링된 방 개수 조회
		int totalRooms = gameRoomService.getFilteredRoomCount(searchParams);
		int totalPages = (int) Math.ceil((double) totalRooms / size);

		// 4. 필터링된 방 목록 조회
		List<GameRoom> rooms = gameRoomService.searchRooms(searchParams);

		// 5. 각 방의 현재 인원수 계산 및 상태를 다시 한글로 변환
		for (GameRoom room : rooms) {
			int userCount = calculateUserCount(room.getUserList());
			room.setCurrentUserCount(userCount);

			// DB 값을 사용자 친화적인 한글로 변환
			if (room.getIsGaming() != null && room.getIsGaming().equals("Y")) {
				room.setIsGaming("게임중");
			} else {
				room.setIsGaming("대기중");
			}
		}

		// 6. 결과 반환
		Map<String, Object> result = new HashMap<>();
		result.put("rooms", rooms);
		result.put("currentPage", page);
		result.put("totalPages", totalPages);
		result.put("totalRooms", totalRooms);
		result.put("pageSize", size);
		result.put("filters", Map.of("type", type, // 원본 한글 값
				"status", status, // 원본 한글 값
				"keyword", keyword // 원본 검색어
		));

		return result;
	}

	/**
	 * 유저 리스트 JSON에서 실제 인원수를 계산하는 헬퍼 메소드
	 */
	private int calculateUserCount(String userListJson) {
		if (userListJson == null || userListJson.isEmpty() || userListJson.equals("[]")) {
			return 0;
		}

		try {
			List<String> users = objectMapper.readValue(userListJson, new TypeReference<List<String>>() {
			});
			return users.size();
		} catch (Exception e) {
			System.err.println("JSON 파싱 실패: " + userListJson);
			return 0;
		}
	}

	@GetMapping("/{roomNo}/{password}")
	public String enterRoom(@PathVariable int roomNo, @PathVariable String password, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {

		GameRoom room = gameRoomService.selectRoom(roomNo);

		if (room == null) {
			redirectAttributes.addFlashAttribute("msg", "해당 게임방이 존재 하지 않습니다.");
			return "redirect:/";
		}

		// 비밀번호가 틀리면 홈으로 이동
		if (room.getPassword() != null && !room.getPassword().trim().isEmpty() // ← 🔧 trim() 추가
				&& !room.getPassword().trim().equals(password.trim())) { // ← 🔧 양쪽 trim()
			redirectAttributes.addFlashAttribute("msg", "게임방의 비밀번호가 틀렸습니다.");
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
				users = objectMapper.readValue(room.getUserList(), new TypeReference<List<String>>() {
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("msg", "방 정보를 불러오는데 실패했습니다.");
			return "redirect:/";
		}

		// 인원 초과 여부 체크 (이미 들어간 사람은 제외)
		if (!users.contains(userName) && users.size() >= room.getHeadCount()) {
			redirectAttributes.addFlashAttribute("msg", "방 인원이 가득 찼습니다.");
			return "redirect:/";
		}

		model.addAttribute("room", room);
		return "game/gameRoom";
	}

	@GetMapping("/loadMessage")
	@ResponseBody
	public List<Message> loadMessage(@RequestParam int roomNo, @RequestParam int page, @RequestParam int size,
			@RequestParam String job) {
		String type = "chat";
		switch (job) {
		case "ghost":
		case "mafiaGhost":
		case "spiritualists":
			type = "death";
			break;
		case "mafia":
			type = "mafia";
			break;
		}

		if (type.equals("chat")) {
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
			users = new ObjectMapper().readValue(readyUsers, new TypeReference<List<String>>() {
			});
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
	public GameRoom reloadRoom(@RequestParam int roomNo) {
		return gameRoomService.selectRoom(roomNo);
	}

	@GetMapping("/userNickList")
	@ResponseBody
	public List<String> userNickList(@RequestParam String userList) {
		return memberService.getUserNickList(userList);
	}

	@GetMapping("/getJob")
	@ResponseBody
	public Map<String, Job> getJob(@RequestParam int roomNo, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Member loginUser = (Member) session.getAttribute("loginUser");
		if (loginUser == null) {
			redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
			return null;
		}
		String userName = loginUser.getUserName();
		System.out.println(">> 로그인 유저: " + userName);
		Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
		String userListJson = clobToString((Clob) result.get("USERLIST"));
		String jobJson = (String) result.get("JOB");
		String startJobJson = (String) result.get("STARTJOB");

		ObjectMapper mapper = new ObjectMapper();
		try {
			if (userListJson != null && jobJson != null) {
				List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {
				});
				List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {
				});
				List<Integer> startList = mapper.readValue(startJobJson, new TypeReference<List<Integer>>() {
				});

				int index = userList.indexOf(userName);
				int myJob = jobList.get(index);
				int myStartJob = startList.get(index);
				Map<String, Job> resultMap = new HashMap<>();

				resultMap.put("myJob", gameRoomService.getJobDetail(myJob));
				resultMap.put("myStartJob", gameRoomService.getJobDetail(myStartJob));
				return resultMap;
			} else {
				return null;
			}

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	@GetMapping("/userDeathList")
	@ResponseBody
	public List<String> getDeathList(@RequestParam int roomNo) {
		return gameRoomService.getDeathList(roomNo);
	}

	@GetMapping("/voteResult")
	@ResponseBody
	public Kill voteUser(@RequestParam int roomNo, @RequestParam int dayNo, @RequestParam String targetName) {
		Kill kill = gameRoomService.selectKill(roomNo, dayNo);

		if (kill == null) {
			return createNewKill(roomNo, dayNo, targetName, null, null);
		} else {
			return updateKill(kill, roomNo, dayNo, targetName, null, null);
		}
	}

	@GetMapping("/healResult")
	@ResponseBody
	public Kill healUser(@RequestParam int roomNo, @RequestParam int dayNo, @RequestParam String targetName) {
		Kill kill = gameRoomService.selectKill(roomNo, dayNo);

		if (kill == null) {
			return createNewKill(roomNo, dayNo, null, targetName, null);
		} else {
			return updateKill(kill, roomNo, dayNo, null, targetName, null);
		}
	}

	@GetMapping("policeCheck")
	@ResponseBody
	public Job policeCheck(@RequestParam int roomNo, @RequestParam int dayNo, @RequestParam String targetName) {
		Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
		String userListJson = clobToString((Clob) result.get("USERLIST"));
		String jobJson = (String) result.get("JOB");
		int targetJob = 0;
		ObjectMapper mapper = new ObjectMapper();
		try {
			if (userListJson == null || jobJson == null) {
				return null;
			}

			List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {
			});
			List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {
			});

			int index = userList.indexOf(targetName);
			targetJob = jobList.get(index);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return gameRoomService.getJobDetail(targetJob);
	}

	@GetMapping("/spyCheck")
	@ResponseBody
	public boolean spyCheck(@RequestParam int roomNo, @RequestParam int dayNo, @RequestParam String targetName,
			HttpSession session) {
		Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
		String userListJson = clobToString((Clob) result.get("USERLIST"));
		String jobJson = (String) result.get("JOB");
		int targetJob = 0;
		ObjectMapper mapper = new ObjectMapper();
		try {
			if (userListJson == null || jobJson == null) {
				return false;
			}

			List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {
			});
			List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {
			});

			int index = userList.indexOf(targetName);
			targetJob = jobList.get(index);
			if (targetJob == 1 || targetJob == 99999) {
				// 현재 로그인한 사용자 정보
				Member loginUser = (Member) session.getAttribute("loginUser");

				int myIndex = userList.indexOf(loginUser.getUserName());

				String updatedJobJson = null;
				if (myIndex != -1 && myIndex < jobList.size()) {
					jobList.set(myIndex, 1);
					updatedJobJson = mapper.writeValueAsString(jobList);
					gameRoomService.updateJob(roomNo, updatedJobJson);
				}
				return true;
			} else {
				return false;
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return false;
	}

	@GetMapping("/robberJob")
	@ResponseBody
	public Job robberJob(@RequestParam int roomNo, @RequestParam String targetName, HttpSession session) {
		Map<String, Object> result = gameRoomService.getRoomJob(roomNo);
		String userListJson = clobToString((Clob) result.get("USERLIST"));
		String jobJson = (String) result.get("STARTJOB");
		int targetJob = 0;
		ObjectMapper mapper = new ObjectMapper();
		try {
			if (userListJson == null || jobJson == null) {
				return gameRoomService.getJobDetail(8);
			}

			List<String> userList = mapper.readValue(userListJson, new TypeReference<List<String>>() {
			});
			List<Integer> jobList = mapper.readValue(jobJson, new TypeReference<List<Integer>>() {
			});

			int index = userList.indexOf(targetName);
			targetJob = jobList.get(index);

			// 현재 로그인한 사용자 정보
			Member loginUser = (Member) session.getAttribute("loginUser");

			int myIndex = userList.indexOf(loginUser.getUserName());

			String updatedJobJson = null;
			if (myIndex != -1 && myIndex < jobList.size()) {
				jobList.set(myIndex, targetJob);
				updatedJobJson = mapper.writeValueAsString(jobList);
				gameRoomService.updateJob(roomNo, updatedJobJson);
				return gameRoomService.getJobDetail(targetJob);
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return gameRoomService.getJobDetail(8);
	}

	@GetMapping("/mafiaKillResult")
	@ResponseBody
	public Kill killUser(@RequestParam int roomNo, @RequestParam int dayNo, @RequestParam String targetName) {
		Kill kill = gameRoomService.selectKill(roomNo, dayNo);

		if (kill == null) {
			return createNewKill(roomNo, dayNo, null, null, targetName);
		} else {
			return updateKill(kill, roomNo, dayNo, null, null, targetName);
		}
	}

	public Kill createNewKill(int roomNo, int dayNo, String targetVote, String tagetDoctor, String targetMafia) {
		try {
			List<String> votes = new ObjectMapper().readValue("[]", new TypeReference<List<String>>() {
			});
			List<String> mafias = new ObjectMapper().readValue("[]", new TypeReference<List<String>>() {
			});
			List<String> doctors = new ObjectMapper().readValue("[]", new TypeReference<List<String>>() {
			});

			if (targetVote != null && !targetVote.equals("")) {
				votes.add(targetVote);
			}

			if (targetMafia != null && !targetMafia.equals("")) {
				mafias.add(targetMafia);
			}

			if (tagetDoctor != null && !tagetDoctor.equals("")) {
				doctors.add(tagetDoctor);
			}

			String updatedVotes = new ObjectMapper().writeValueAsString(votes);
			String updateDoctors = new ObjectMapper().writeValueAsString(doctors);
			String updatedMafias = new ObjectMapper().writeValueAsString(mafias);

			Kill kill = new Kill(roomNo, dayNo, updatedVotes, updatedMafias, updateDoctors);

			gameRoomService.insertKill(kill);
		} catch (Exception e) {
			e.printStackTrace(); // JSON 파싱 실패 시 빈 리스트 유지
		}

		return gameRoomService.selectKill(roomNo, dayNo);
	}

	public Kill updateKill(Kill kill, int roomNo, int dayNo, String targetVote, String tagetDoctor,
			String targetMafia) {
		try {
			List<String> votes = new ObjectMapper().readValue(kill.getVote(), new TypeReference<List<String>>() {
			});
			List<String> mafias = new ObjectMapper().readValue(kill.getKillUser(), new TypeReference<List<String>>() {
			});
			List<String> doctors = new ObjectMapper().readValue(kill.getHealUser(), new TypeReference<List<String>>() {
			});

			if (targetVote != null && !targetVote.equals("")) {
				votes.add(targetVote);
			}

			if (targetMafia != null && !targetMafia.equals("")) {
				mafias.add(targetMafia);
			}

			if (tagetDoctor != null && !tagetDoctor.equals("")) {
				doctors.add(tagetDoctor);
			}

			String updatedVotes = new ObjectMapper().writeValueAsString(votes);
			String updateDoctors = new ObjectMapper().writeValueAsString(doctors);
			String updatedMafias = new ObjectMapper().writeValueAsString(mafias);

			kill.setVote(updatedVotes);
			kill.setKillUser(updatedMafias);
			kill.setHealUser(updateDoctors);

			gameRoomService.updateKill(kill);

		} catch (Exception e) {
			e.printStackTrace(); // JSON 파싱 실패 시 빈 리스트 유지
		}
		return gameRoomService.selectKill(roomNo, dayNo);
	}

	@GetMapping("/getRoomHint")
	@ResponseBody
	public List<RoomHint> getRoomHint(@RequestParam int roomNo) {
		return roomHintService.selectRoomHintList(roomNo);
	}

	public static String clobToString(Clob clob) {
		if (clob == null)
			return null;

		try (Reader reader = clob.getCharacterStream(); StringWriter writer = new StringWriter()) {
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

	@Controller
	@RequestMapping("/score")
	public class ScoreController {

		@GetMapping("/scorepoint")
		public String scorepoint() {
			return "score/scorepoint"; // → templates/score/scorepoint.html 로 연결됨
		}

		@GetMapping("/recodeList")
		@ResponseBody
		public Map<String,Object> getRecodeList(@RequestParam String userName, int currentPage) {
			int pageLimit = 10;
			int boardLimit = 10;
			int totalCount = gameRoomService.getTotalRecodeCount(userName);

			PageInfo pi = Pagination.getPageInfo(totalCount, currentPage, pageLimit, boardLimit);
			//List<Map<String,Object>>
			Map<String,Object> resultMap = new HashMap<>();
			resultMap.put("recodeList", gameRoomService.getRecodeList(pi, userName));
			resultMap.put("maxPage", pi.getMaxPage());
			return resultMap;
		}
	}

	// 전적 저장을 위한 메소드 추가 by 이수한
	@GetMapping("/saveGameResult")
	@ResponseBody
	public int saveGameResult(String userName, int jobNo, String type, int startJobNo) {

		Map<String, Object> gameResultMap = new HashMap<>();
		gameResultMap.put("resultNo", UUID.randomUUID().toString());
		gameResultMap.put("userName", userName);
		gameResultMap.put("jobNo", jobNo);

		Job finalJob = gameRoomService.getJobDetail(jobNo); // 최종 직업 정보 가져오기
		int jobClass = finalJob.getJobClass();

		switch (jobClass) {
		case 1:
			gameResultMap.put("team", "마피아팀");
			break;
		case 2:
			gameResultMap.put("team", "시민팀");
			break;
		case 3:
			gameResultMap.put("team", "중립팀");
			break;
		}

		gameResultMap.put("teamResult", checkWin(jobClass, type));
		gameResultMap.put("date", new Date());
		return gameRoomService.insertGameResult(gameResultMap);
	}

	@PostMapping("/leave")
	@ResponseBody // 이 어노테이션이 있어야 View를 찾지 않고 API로 동작합니다.
	public void handleBeaconLeave(@RequestBody Map<String, Integer> payload, HttpSession session) {
		Member loginUser = (Member) session.getAttribute("loginUser");
		Integer roomNo = payload.get("roomNo");

		if (loginUser != null && roomNo != null) {
			// 이전에 만들었던 "즉시 퇴장" 메소드를 그대로 호출합니다.
			gameRoomManager.leaveRoomImmediately(roomNo, loginUser.getUserName());
		}
	}

	private String checkWin(int jobClass, String type) {
		String result = null;
		switch (jobClass) {
		case 1:
			result = type.equals("MAFIA_WIN") ? "승리" : "패배";
			break;
		case 2:
			result = type.equals("CITIZEN_WIN") ? "승리" : "패배";
			break;
		case 3:
			result = type.equals("NEUTRALITY_WIN") ? "승리" : "패배";
			break;
		}
		return result;
	}

}