// 전송 전에 입력값을 숨은 input에 저장
document.querySelector('form').addEventListener('submit', function() {
	document.getElementById('detailInput').value = quill.root.innerHTML;
});

document.getElementById('deleteFileBtn')?.addEventListener('click', function() {
	// 현재 파일 정보 숨김
	const flag = confirm('등록한 첨부파일을 삭제하시겠습니까?');
	if (flag) {
		const fileInfo = document.getElementById('current-file-info');
		if (fileInfo) fileInfo.remove();

		// deleteFile hidden input 추가
		const deleteInput = document.createElement('input');
		deleteInput.type = 'hidden';
		deleteInput.name = 'deleteFile';
		deleteInput.value = 'true';

		document.querySelector('form').appendChild(deleteInput);
	}
});



$(function() {
	const typeName = typeNameOfBoard;

	const roleToTeamMap = {
		"마피아": "mafia",
		"스파이": "mafia",
		"경찰": "citizen",
		"의사": "citizen",
		"정치인": "citizen",
		"연인": "citizen",
		"군인": "citizen",
		"영매사": "citizen",
		"훼방꾼": "third",
		"도굴꾼": "third",
		"네크로맨서": "third"
	};

	// 카테고리 초기값 설정
	const isJob = typeName in roleToTeamMap;

	if (isJob) {
		// 1. 상위 카테고리 select 박스에서 "직업" 선택
		$("#category").val("직업");

		// 2. 팀-직업 선택 영역 보이기
		$("#team-role-section").show();

		// 3. 역할 select에 name 부여
		$("#role-select").attr("name", "jobTypeName");

		const teamKey = roleToTeamMap[typeName];
		$("#team-select").val(teamKey).trigger("change");

		// 4. 직업 select에서 해당 직업 선택 (role-select는 change 이벤트 후 렌더됨)
		setTimeout(() => {
			$("#role-select").val(typeName);
			$("#role-group").show(); // 보장
		}, 50);

	} else {
		// 자유, 플레이 등의 카테고리라면 단순 설정
		$("#category").val(typeName);
		$("#team-role-section").hide();
		$("#role-select").removeAttr("name");
	}
});
