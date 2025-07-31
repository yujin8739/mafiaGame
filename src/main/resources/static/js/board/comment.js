function loadComments() {
	var loginUserName = userName;

	$.ajax({
		url: '/board/getReplyList',
		data: {
			boardNo: boardNo
		},
		success: function(replyList) {
			$('#bestCommentList').empty();
			$('#commentList').empty();

			// 좋아요 3개 이상 댓글들 중 BEST 선정
			const bestReplies = [];
			const normalReplies = [];

			for (var i = 0; i < replyList.length; i++) {
				if (replyList[i].likeCount >= 3) {
					bestReplies.push(replyList[i]);
				} else {
					normalReplies.push(replyList[i]);
				}
			}

			// BEST 댓글 정렬 (좋아요 높은 순)
			for (var i = 0; i < bestReplies.length - 1; i++) {
				for (var j = i + 1; j < bestReplies.length; j++) {
					if (bestReplies[i].likeCount < bestReplies[j].likeCount) {
						var temp = bestReplies[i];
						bestReplies[i] = bestReplies[j];
						bestReplies[j] = temp;
					}
				}
			}

			// 최대 2개만 BEST 댓글로
			var bestToShow = bestReplies.slice(0, 2);
			var restBest = bestReplies.slice(2);
			normalReplies.push(...restBest);

			// BEST 댓글 출력
			bestToShow.forEach(function(reply) {
				const html = generateReplyHtml(reply, true);
				$('#bestCommentList').append(html);
			});

			// 일반 댓글 출력
			normalReplies.forEach(function(reply) {
				const html = generateReplyHtml(reply, false);
				$('#commentList').append(html);
			});

			// 삭제 버튼 제어
			var replyDivs = $('.comment-item.position-relative');
			replyDivs.each(function() {
				if ($(this).children("#replyWriter").text() !== loginUserName) {
					$(this).find(".delete-btn").remove();
				}
			});
		}
	});
}

function generateReplyHtml(reply, isBest) {
	return `
				    <div class="comment-item position-relative ${isBest ? 'best-comment' : ''}">
				      <div class="comment-actions">
				        <span class="like-wrapper">
				          <i class="fas fa-heart like-btn" title="좋아요"></i>
				          <span class="comment-like-badge">${reply.likeCount}</span>
				        </span>
				        <i class="fas fa-flag report-btn" title="신고"></i>
				        <i class="fas fa-trash delete-btn" title="삭제"></i>
				        <div style="display: none;" id="replyNo">${reply.replyNo}</div>
				      </div>

				      <div style="display: flex; align-items: center;">
				        <img src="${reply.profileUrl}" alt="Badge" class="badge-image" />
				        ${isBest ? `<span class="best-badge">BEST</span>` : ''}
				        <span class="comment-author">${reply.nickName}</span>
				      </div>

				      <div class="comment-date">${reply.createDate}</div>
				      <div class="comment-text">${reply.replyContent}</div>

				      ${reply.changeName ? `<img src="/godDaddy_uploadImage/replyImage/${reply.changeName}" style="max-width: 100px; margin-top: 10px;" />` : ''}

				      <div style="display: none;" id="replyWriter">${reply.userName}</div>
				    </div>
				  `;
}

$(function() {
	loadComments();

	$('#commentImage').on('change', function(e) {
		const file = e.target.files[0];
		if (file) {
			const reader = new FileReader();
			reader.onload = function(event) {
				$('#previewImage').attr('src', event.target.result).show();
			};
			reader.readAsDataURL(file);
		} else {
			$('#previewImage').hide();
		}
	});


	// 댓글 등록 버튼 클릭
	$('#submitComment').click(function() {
		const content = $('#commentContent').val();
		const imageFile = $('#commentImage')[0].files[0];

		const formData = new FormData();
		formData.append('replyContent', content);
		formData.append('boardNo',boardNo);
		formData.append('userName',userName);
		if (imageFile) formData.append('image', imageFile);
		if (content === '' && !imageFile) {
			alert('내용을 입력해주세요');
			return;
		}

		$.ajax({
			url: '/board/uploadReply',
			type: 'POST',
			data: formData,
			processData: false,
			contentType: false,
			success: function(result) {
				if (result > 0) {
					$('#commentContent').val('');
					$('#commentImage').val('');
					$('#previewImage').hide();
					loadComments(); // 다시 불러오기

				} else {
					alert("댓글이 정상적으로 등록되지 않았습니다");
				}

			},
			error: function() {
				alert("서버 오류가 발생하였습니다. 잠시 후 다시 시도해주세요");
			}

		});
	});

	$('.comments-section').on('click', '.like-btn', function() {
		// TODO: Ajax 호출 등으로 좋아요 처리
		const $count = $(this).next(); //count적는 span 태그
		const count = parseInt($count.text());
		$.ajax({
			url: "/board/likeReply",
			data: {
				replyNo: $(this).parent().siblings("#replyNo").text()
			},
			success: function(result) {
				if (result > 0) { //댓글에 좋아요가 잘 등록된 경우
					$count.text(count + 1);
				} else if (result === -1) {
					$count.text(count - 1);
				} else {
					alert("해당 댓글에 '좋아요'가 등록되지 않았습니다");
				}
				loadComments();
			},
			error: function() {
				alert("서버 오류가 발생했습니다. 잠시후 다시 시도해주세요");
			}
		});

	});

	$('.comments-section').on('click', '.report-btn', function() {
		const confirmReport = confirm('이 댓글을 신고하시겠습니까?');
		if (confirmReport) {
			alert('신고 처리됨');
			// TODO: Ajax 호출로 신고 처리
		}
	});

	$('.comments-section').on('click', '.delete-btn', function(e) {

		const confirmDelete = confirm('이 댓글을 삭제하시겠습니까?');

		if (confirmDelete) {

			// TODO: Ajax 호출로 댓글 삭제 처리
			$.ajax({
				url: "/board/deleteReply",
				method: "post",
				data: {
					replyNo: $(e.target).next().text()
				},
				success: function(result) {
					if (result > 0) {
						alert('댓글이 삭제 처리되었습니다');
						loadComments();

					} else {
						alert('댓글이 정상적으로 삭제되지 않았습니다')
					}
				},
				error: function() {
					alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요');
				}

			});


		}
	});

});