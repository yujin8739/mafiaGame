const rolesByTeam = {
	mafia: [
		{ text: "마피아", value: "마피아" },
		{ text: "스파이", value: "스파이" }
	],
	citizen: [
		{ text: "경찰", value: "경찰" },
		{ text: "의사", value: "의사" },
		{ text: "정치인", value: "정치인" },
		{ text: "연인", value: "연인" },
		{ text: "군인", value: "군인" },
		{ text: "영매사", value: "영매사" },
	],
	third: [
		{ text: "훼방꾼", value: "훼방꾼" },
		{ text: "도굴꾼", value: "도굴꾼" },
		{ text: "네크로맨서", value: "네크로맨서" }
	]
};

$(function() {
	const $team = $("#team-select");
	const $role = $("#role-select");

	// 카테고리 변경 감지
	$("#category").on("change", function() {
		const categoryVal = $(this).val();

		if (categoryVal === "직업") {
			$("#team-role-section").show();
			// name 속성 동적으로 추가
			$role.attr("name", "jobTypeName");
		} else {
			$("#team-role-section").hide();
			$("#role-group").hide();
			// name 속성 제거 (submit 안되도록)
			$role.removeAttr("name");
		}
	});

	// 팀 선택 시 직업 옵션 설정
	$team.on("change", function() {
		const selectedTeam = $(this).val();

		if (!rolesByTeam[selectedTeam]) {
			$("#role-group").hide();
			$role.empty();
			return;
		}

		$role.empty();
		$.each(rolesByTeam[selectedTeam], function(index, role) {
			$role.append($("<option>", {
				value: role.value,
				text: role.text
			}));
		});

		$("#role-group").show();
	});

	// 최종 submit 유효성 체크
	$("#loungeBoardForm").submit(function(e) {
		const category = $("#category").val();

		if (category === "직업") {
			if (!$team.val()) {
				alert("팀을 선택해주세요.");
				$team.focus();
				e.preventDefault();
				return;
			}

			if (!$role.val()) {
				alert("직업을 선택해주세요.");
				$role.focus();
				e.preventDefault();
				return;
			}
		}
	});
});