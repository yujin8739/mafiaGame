package com.mafia.game.job.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mafia.game.job.model.service.JobService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/job")
public class JobController {

	@Autowired
	private JobService service;
	
	
	//플레이어 정보를 받아와 랜덤하게 직업 부여
	@ResponseBody
	@RequestMapping(value = "/employment",
					produces = "text/html;charset=UTF-8")
	public String jobEmployment(HttpSession session) {
		//플레이어 명단(리스트)
		List<String> pList = new ArrayList<>();
		pList.add("player1");
		pList.add("player2");
		pList.add("player3");
		pList.add("player4");
		pList.add("player5");
		pList.add("player6");
		//플레이어 수
		int pCount = pList.size();
		
		//플레이어 수에 따라 분기(6, 8, 10)
		//각 플레이어 수에 맞는 직업 테이블에서 필수 직업과 선택 직업 조회
		if(pCount < 8) {
			List<Integer> ejList = service.essentialJob6();
			List<Integer> ojList = service.optionalJob6();
		}else if(pCount < 10) {
			//8인 이상 게임 직업 테이블에서 직업 목록 조회
		}
		
		
		return "직업 랜덤 부여 성공";
	}
	
	
	
	
	
	
}
