$(function() {
	const typeClass = typeClassOfFilterMap;
	const decodedUrl = decodeURIComponent(window.location.href);
	const isJobTabActive = decodedUrl.includes("typeName=직업");

	// 직업 탭이면 팀 탭 보여주기
	if (isJobTabActive || typeClass == 3) {
		$("#teamTabs").show();
		// 현재 활성화된 팀에 맞춰 역할탭 보여주기
		if ($("#roleTabs-mafia a.active").length) {
			$("#team-mafia").addClass("active");
			$("#roleTabs-mafia").show();
		} else if ($("#roleTabs-citizen a.active").length) {
			$("#team-citizen").addClass("active");
			$("#roleTabs-citizen").show();
		} else if ($("#roleTabs-third a.active").length) {
			$("#team-third").addClass("active");
			$("#roleTabs-third").show();
		} else {
			// 기본은 전체 팀, 역할탭 숨김
			$("#teamTabs a[data-team='all']").addClass("active");
			$(".role-tabs").hide();
		}
	} else {
		$("#teamTabs").hide();
		$(".role-tabs").hide();
	}

	// 서브탭 클릭 (전체, 자유, 플레이, 직업)
	$(".sub-tabs a").on("click", function() {
		// 여기선 클릭 후 페이지가 이동하므로, fade 효과는 사실 의미가 없음
		// 하지만 이동 안 할 경우를 대비해 작성

		// 필터맵에 의존하지 말고, URL로 판단하거나 typeClass 재조회 권장
		if (typeClass == 3) {
			$("#teamTabs").fadeIn(200);
		} else {
			$("#teamTabs").fadeOut(200);
			$(".role-tabs").hide();
		}
	});

	// 팀 탭 클릭 (전체, 팀 마피아, 팀 시민, 팀 제3세력)
	$(".team-tab").on("click", function(e) {
		// 이동 막지 않고 탭 스타일만 변경 (필요시 e.preventDefault() 추가)
		$(".team-tab").removeClass("active");
		$(this).addClass("active");

		const team = $(this).data("team");

		$(".role-tabs").hide();

		if (team === "mafia") {
			$("#roleTabs-mafia").fadeIn(200);
		} else if (team === "citizen") {
			$("#roleTabs-citizen").fadeIn(200);
		} else if (team === "third") {
			$("#roleTabs-third").fadeIn(200);
		}
		// team === 'all'인 경우 역할탭 숨김
	});

});