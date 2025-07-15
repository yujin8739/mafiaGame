package com.mafia.game.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("introduce")
public class IntroductionController {
	
	
	@GetMapping("/list")
	public String introduceList() {
	    return "board/introduction";
	}

	@GetMapping("/roles")
    public String roles(@RequestParam(defaultValue = "mafia") String team) {

        // team 값에 따라 프래그먼트 경로 설정
        switch (team) {
            case "citizen":
                return "introduceFragment/teamCitizen :: roleList";
            case "neutral":
                return "introduceFragment/teamNeutral :: roleList";
            case "mafia":
            default:
                return "introduceFragment/teamMafia :: roleList";
        }
    }
	
	@GetMapping("/teamFragment")
	public String teamFragment(@RequestParam(defaultValue = "mafia") String team, Model model) {
		model.addAttribute("selectedTeam", team);
	    return "introduceFragment/team :: team";
	}
	
	@GetMapping("/roleDetail")
	public String roleDetail(@RequestParam("role") String roleName, Model model) {

	    String description = "";
	    String imagePath = "";

	    switch (roleName) {
	        case "마피아":
	            description = "마피아는 밤마다 시민을 제거할 수 있는 능력을 가진 직업입니다.";
	            imagePath = "/images/roles/mafia.png";
	            break;
	        case "스파이":
	            description = "스파이는 마피아의 동향을 파악하며 정보를 수집하는 역할을 합니다.";
	            imagePath = "/images/roles/spy.png";
	            break;
	        case "의사":
	            description = "의사는 밤 중에 치명상을 입은 사람을 치료할 수 있습니다.";
	            imagePath = "/images/roles/doctor.png";
	            break;
	        // 필요한 경우 다른 직업 추가
	        default:
	            description = "알 수 없는 직업입니다.";
	            imagePath = "/images/사망이미지.png";
	    }

	    // 모델에 데이터 담기
	    model.addAttribute("description", description);
	    model.addAttribute("imagePath", imagePath);
	    model.addAttribute("roleName", roleName); // 직업명도 표시 가능

	    return "introduceFragment/roleDetail :: roleDetail";
	}
	
}
