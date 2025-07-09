package com.mafia.game.job.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.game.job.model.service.JobService;
import com.mafia.game.job.model.vo.Player;

@Controller
@RequestMapping("/job")
public class JobController {

	@Autowired
	private JobService service;
	
	
	//플레이어 정보를 받아와 랜덤하게 직업 부여
	@PostMapping("/employment")
	public String jobEmployment(@RequestParam("roomNo") int roomNo,
								@RequestParam("userName") String userName,
								Model model) throws JsonMappingException, JsonProcessingException {
		//플레이어 명단(리스트)
		String player = service.playerList(roomNo);
		ObjectMapper objectMapper = new ObjectMapper();
		List<String> pList = objectMapper.readValue(player, new TypeReference<List<String>>() {});
        List<String> nList = new ArrayList<>();
        if (!pList.contains(userName)) {
        	pList.add(userName);
        }
		//플레이어 수
		for(int i = 0; i < pList.size(); i++) {
			nList.add(service.userNickName(pList.get(i)));
		}
		int pCount = nList.size();
		
		//플레이어 수에 따라 분기(8, 10, 12)
		//각 플레이어 수에 맞는 직업 테이블에서 필수 직업과 선택 직업 조회
		List<String> ejList = new ArrayList<>();
		List<String> ojList = new ArrayList<>();
		List<String> njList = new ArrayList<>();
		
		if(pCount < 13) {
			ejList = service.essentialJob8();
			ojList = service.optionalJob8();
			njList = service.neutralJob8();
		}
		//조회해온 직업 리스트 병합
		List<String> jobList = new ArrayList<>(ejList);
		if(!njList.isEmpty()) {
			Collections.shuffle(njList);
			jobList.add(njList.get(0));
		}
		Collections.shuffle(ojList);
		for(int i = 0; i < pCount - jobList.size(); i++) {
			jobList.add(ojList.get(i));
		}
		Collections.shuffle(jobList);
		String job = "";
		for(int i = 0; i < pList.size(); i++) {
			if(pList.get(i).equals(userName)) {
				job = jobList.get(i);
				break;
			}
		}
		List<Integer> jnList = new ArrayList<>();
		for(int i = 0; i < jobList.size(); i++) {
			jnList.add(service.jobNo(jobList.get(i)));
		}
		for(int i = 0; i < pCount; i++) {
			playerInfo(jnList.get(i), pList.get(i), roomNo);
		}
		ArrayList<Player> playerList = service.player(roomNo);
		for(Player p : playerList) {
			System.out.println(p);
		}
		model.addAttribute("player", playerList);
		model.addAttribute("nickName", nList);
		model.addAttribute("job", job);
		
		return "job/playerPanel :: playerPanelFragment";
		
	}
	
	public void playerInfo(int jobNo, String playerName, int roomNo) {
		service.playerInfo(jobNo, playerName, roomNo);
	}
	
}
