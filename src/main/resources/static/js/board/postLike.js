$(function() {
	// 게시글 좋아요/싫어요 버튼 클릭 이벤트
	$('#btnLike').click(function() {
		// TODO: Ajax 호출로 좋아요 증가 처리 후 카운트 갱신
		$.ajax({
			url: "/board/like",
			data: {
				boardNo: boardNo,
				type: 'L'
			},
			success: function(result) {
				let count = parseInt($('#likeCount').text());
				if (result > 0) { //제대로 '좋아요'가 등록이 되었다면
					$('#likeCount').text(count + 1);
				} else if (result === -1) {
					$('#likeCount').text(count - 1);
				} else if (result === -100) {
					let dislikeCount = parseInt($('#dislikeCount').text());
					$('#dislikeCount').text(dislikeCount - 1);
					$('#likeCount').text(count + 1);
				} else {
					alert("해당 게시물에 '좋아요'가 등록되지 않았습니다");
				}
			},
			error: function() {
				alert("서버 오류가 발생했습니다. 잠시후 다시 시도해주세요");
			}

		});
	});

	$('#btnDislike').click(function() {
		// TODO: Ajax 호출로 싫어요 증가 처리 후 카운트 갱신
		$.ajax({
			url: "/board/like",
			data: {
				boardNo: boardNo,
				type: 'D'
			},
			success: function(result) {
				let count = parseInt($('#dislikeCount').text());
				if (result > 0) { //제대로 '좋아요'가 등록이 되었다면
					$('#dislikeCount').text(count + 1);
				} else if (result === -1) {
					$('#dislikeCount').text(count - 1);
				} else if (result === -100) {
					let likeCount = parseInt($('#likeCount').text());
					$('#likeCount').text(likeCount - 1);
					$('#dislikeCount').text(count + 1);
				} else {
					alert("해당 게시물에 '싫어요'가 등록되지 않았습니다");
				}
			},
			error: function() {
				alert("서버 오류가 발생했습니다. 잠시후 다시 시도해주세요");
			}

		});

	});

});
