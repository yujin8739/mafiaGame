
// 쪽지 상세보기로 이동
function goToDetail(privateMsgNo) {
	if (privateMsgNo) {
		location.href = '/message/detail?privateMsgNo=' + privateMsgNo;
	}
}

// 쪽지 삭제 함수
function deleteMessage(privateMsgNo, deleteType) {
	if (!privateMsgNo || !deleteType) {
		alert('오류가 발생했습니다.');
		return;
	}

	const confirmDelete = confirm('이 쪽지를 삭제하시겠습니까?');

	if (confirmDelete) {
		$.ajax({
			url: '/message/delete',
			type: 'POST',
			data: {
				privateMsgNo: privateMsgNo,
				deleteType: deleteType
			},
			success: function(response) {
				if (response.success) {
					alert(response.message);
					location.reload(); // 페이지 새로고침
				} else {
					alert(response.message);
				}
			},
			error: function() {
				alert('서버 오류가 발생했습니다.');
			}
		});
	}
}

// 안읽은 메시지 개수 업데이트 (주기적으로)
function updateUnreadCount() {
	$.ajax({
		url: '/message/unreadCount',
		type: 'GET',
		success: function(response) {
			if (response.success && response.unreadCount > 0) {
				const badge = document.querySelector('.unread-badge');
				if (badge) {
					badge.textContent = response.unreadCount;
				}
			}
		},
		error: function() {
			console.log('안읽은 메시지 개수 조회 실패');
		}
	});
}

// 페이지 로드 시 실행
$(document).ready(function() {
	console.log('쪽지함 페이지 로드 완료');

	// 5분마다 안읽은 메시지 개수 업데이트
	setInterval(updateUnreadCount, 300000);
});