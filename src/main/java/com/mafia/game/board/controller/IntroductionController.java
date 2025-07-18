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
	            description = "어둠 속에 숨어 도시를 조종하는 범죄 조직의 일원.<br>"
	            		+ "매 밤마다 한 명의 플레이어를 사살할 수 있습니다.<br>"
	            		+ "같은 마피아 팀원들과 은밀하게 협력해 시민들을 제거해 나가세요.";
	            imagePath = "/images/roles/mafia.png";
	            break;
	        case "스파이":
	            description = "겉으론 시민 진영에 속해 있는 듯하지만, 실제 정체는 이중 첩자.<br>"
	            		+ "밤마다 마피아와 접선하여 정보를 주고받을 수 있습니다.<br>"
	            		+ "자신의 정체를 숨긴 채 시민들의 정보를 마피아에게 전달하며 이득을 챙깁니다.";
	            imagePath = "/images/roles/spy.png";
	            break;
	        case "의사":
	            description = "한때 도시의 명망 높은 종합병원에서 생명을 지키던 의사였지만,<br>"
	            		+ "끝없는 사건과 죽음 속에서 스스로를 잃어가고 있는 인물입니다.<br>"
	            		+ "밤마다 플레이어 한 명을 지정해 마피아의 공격으로부터 보호할 수 있습니다.<br>"
	            		+ "보호받은 대상은 해당 밤에 사망하지 않습니다.";
	            imagePath = "/images/roles/doctor.png";
	            break;
	        case "경찰":
	        	description = "질서가 무너진 도시의 마지막 정의.<br>"
	        			+ "연이은 실종과 살인사건 속에서도 끝까지 수사를 포기하지 않는 베테랑 경찰입니다.<br>"
	            		+ "밤마다 플레이어 한 명을 선택해 해당 플레이어의 직업을 확인할 수 있습니다.";
	            imagePath = "/images/roles/police.png";
	            break;
	        case "훼방꾼":
	        	description = "조커 가면 뒤에 감춰진 정체불명의 인물.<br>"
	        			+ "그는 마피아도 시민도 아닌, 오직 혼돈 그 자체를 추구합니다.<br>"
	        			+ "자신이 세상의 ‘진정한 심판자’라 믿으며, 게임판을 뒤흔들 기회를 노리고 있습니다.<br>"
	        			+ "첫 낮 투표에서 자신이 처형될 경우 단독 승리하게 됩니다.";
	            imagePath = "/images/roles/saboteur.png";
	            break;
	        case "도굴꾼":
	        	description = "죽음을 기회로 삼는 그림자 같은 존재.<br>"
	        			+ "첫 사망자의 직업과 팀을 그대로 복사해 그 역할을 이어갑니다.";
	            imagePath = "/images/roles/graverobber.png";
	            break;
	        case "연인":
	        	description = "어느 날 우연처럼 시작된 사랑은 서로에게 없어서는 안 될 운명으로 깊어졌습니다.<br>"
	        			+ "연인은 둘이 한 팀으로 행동하며, 밤에 마피아의 타깃이 상대 연인일 경우 자신이 대신 죽음을 맞이합니다.";
	            imagePath = "/images/roles/lover.png";
	            break;
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
