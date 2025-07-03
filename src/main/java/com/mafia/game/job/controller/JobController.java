package com.mafia.game.job.controller;

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
		//직업 테이블에서
		
		
		
		return "직업 랜덤 부여 성공";
	}
	
}
