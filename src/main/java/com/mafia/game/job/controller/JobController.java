package com.mafia.game.job.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mafia.game.job.model.service.JobService;

@Controller
@RequestMapping("/job")
public class JobController {

	@Autowired
	private JobService service;
	
	
	//플레이어 정보를 받아와 랜덤하게 직업 부여
	@ResponseBody
	@PostMapping("/employment")
	public String jobEmployment(@RequestBody Map<String, String> playerList, Model model) {
		//플레이어 명단(리스트)
		List<String> pList = new ArrayList<>(playerList.values());
        
		//플레이어 수
		int pCount = pList.size();
		System.out.println(pList.toString());
		System.out.println(pCount);
		
		//플레이어 수에 따라 분기(6, 8, 10)
		//각 플레이어 수에 맞는 직업 테이블에서 필수 직업과 선택 직업 조회
		List<Integer> ejList = new ArrayList<>();
		List<Integer> ojList = new ArrayList<>();
		List<Integer> njList = new ArrayList<>();
		if(pCount < 8) {
			ejList = service.essentialJob6();
			ojList = service.optionalJob6();
			njList = service.neutralJob6();
		}else if(pCount < 10) {
			//8인 이상 게임 직업 테이블에서 직업 목록 조회
//			ejList = service.essentialJob8();
//			ojList = service.optionalJob8();
//			njList = service.neutralJob8();
		}else {
			//10인
//			ejList = service.essentialJob10();
//			ojList = service.optionalJob10();
//			njList = service.neutralJob10();
		}
		//조회해온 직업 리스트 병합
		List<Integer> jobList = new ArrayList<>(ejList);
		if(!njList.isEmpty()) {
			Collections.shuffle(njList);
			jobList.add(njList.get(0));
		}
		Collections.shuffle(ojList);
		for(int i = 0; i < pCount - jobList.size(); i++) {
			jobList.add(ojList.get(i));
		}
		Collections.shuffle(jobList);
		System.out.println((String)jobList.toString());
		return "job/playerPanel :: playerPanelFragment";
	}
	
	
	
	
	
	
}
