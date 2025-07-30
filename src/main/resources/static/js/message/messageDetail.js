// ✅ 쪽지 삭제 함수
function deleteMessage(privateMsgNo, deleteType) {
    if (!privateMsgNo || !deleteType) {
        alert('잘못된 요청입니다.');
        return;
    }

    const confirmDelete = confirm('이 쪽지를 삭제하시겠습니까?');

    if (confirmDelete) {
        const deleteBtn = event.target;
        const originalText = deleteBtn.textContent;
        deleteBtn.textContent = '삭제중...';
        deleteBtn.disabled = true;

        $.ajax({
            url: '/message/delete',
            type: 'POST',
            data: {
                privateMsgNo: privateMsgNo,
                deleteType: deleteType
            },
            success: function (response) {
                if (response && response.success) {
                    alert(response.message || '쪽지가 삭제되었습니다.');
                    if (deleteType === 'sender') {
                        location.href = '/message/outbox';
                    } else {
                        location.href = '/message/inbox';
                    }
                } else {
                    alert(response.message || '쪽지 삭제에 실패했습니다.');
                }
            },
            error: function (xhr, status, error) {
                console.error('삭제 요청 실패:', { xhr, status, error });

                let errorMessage = '서버 오류가 발생했습니다.';
                if (xhr.status === 403) {
                    errorMessage = '권한이 없습니다.';
                } else if (xhr.status === 404) {
                    errorMessage = '삭제할 쪽지를 찾을 수 없습니다.';
                } else if (xhr.status === 500) {
                    errorMessage = '서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
                }

                alert(errorMessage);
            },
            complete: function () {
                deleteBtn.textContent = originalText;
                deleteBtn.disabled = false;
            }
        });
    }
}

// ✅ 페이지 로드 시 실행
$(document).ready(function () {
    console.log('쪽지 상세보기 페이지 로드 완료');

    if (typeof currentUser !== 'undefined' &&
        typeof receiverUserName !== 'undefined' &&
        typeof readYn !== 'undefined') {

        if (currentUser === receiverUserName && readYn === 'N') {
            console.log('새 메시지 읽음 처리됨');
        }
    }
});


