$(document).ready(function () {
    updateCharCounter();

    $('#title').on('input', updateCharCounter);
    $('#content').on('input', updateCharCounter);

    loadDraft();

    console.log('쪽지 쓰기 페이지 로드 완료');
});

// 글자 수 카운터
function updateCharCounter() {
    const titleLength = $('#title').val().length;
    const contentLength = $('#content').val().length;

    $('#titleCounter').text(titleLength);
    $('#contentCounter').text(contentLength);

    $('#titleCounter').css('color', titleLength > 90 ? '#ff3b3b' : '#888');
    $('#contentCounter').css('color', contentLength > 900 ? '#ff3b3b' : '#888');
}

// 폼 유효성 검사
function validateForm() {
    const receiverUserName = $('#receiverUserName').val().trim();
    const title = $('#title').val().trim();
    const content = $('#content').val().trim();

    if (!receiverUserName) {
        alert('받는 사람을 입력해주세요.');
        $('#receiverUserName').focus();
        return false;
    }
    if (!title) {
        alert('제목을 입력해주세요.');
        $('#title').focus();
        return false;
    }
    if (!content) {
        alert('내용을 입력해주세요.');
        $('#content').focus();
        return false;
    }
    if (title.length > 100) {
        alert('제목은 100자를 초과할 수 없습니다.');
        $('#title').focus();
        return false;
    }
    if (content.length > 1000) {
        alert('내용은 1000자를 초과할 수 없습니다.');
        $('#content').focus();
        return false;
    }

    const currentUser = document.body.dataset.currentUser || '';
    if (receiverUserName === currentUser) {
        alert('자기 자신에게는 쪽지를 보낼 수 없습니다.');
        $('#receiverUserName').focus();
        return false;
    }

    return confirm('쪽지를 보내시겠습니까?');
}

// 임시저장
function saveDraft() {
    const draftData = {
        receiverUserName: $('#receiverUserName').val(),
        title: $('#title').val(),
        content: $('#content').val(),
        messageType: $('#messageType').val(),
        timestamp: new Date().toISOString()
    };

    localStorage.setItem('messageDraft', JSON.stringify(draftData));
    alert('임시저장되었습니다.');
}

// 임시저장 불러오기
function loadDraft() {
    const draft = localStorage.getItem('messageDraft');
    if (draft) {
        try {
            const draftData = JSON.parse(draft);

            const draftTime = new Date(draftData.timestamp);
            const now = new Date();
            const hoursDiff = (now - draftTime) / (1000 * 60 * 60);

            if (hoursDiff > 24) {
                localStorage.removeItem('messageDraft');
                return;
            }

            const isReply = document.body.dataset.isReply === 'true';
            if (!isReply && !$('#receiverUserName').val() && !$('#title').val() && !$('#content').val()) {
                if (confirm('임시저장된 내용이 있습니다. 불러오시겠습니까?')) {
                    $('#receiverUserName').val(draftData.receiverUserName || '');
                    $('#title').val(draftData.title || '');
                    $('#content').val(draftData.content || '');
                    $('#messageType').val(draftData.messageType || '');
                    updateCharCounter();
                }
            }
        } catch (e) {
            console.error('임시저장 데이터 로드 실패:', e);
            localStorage.removeItem('messageDraft');
        }
    }
}

// 폼 초기화
function resetForm() {
    if (confirm('작성 중인 내용이 모두 삭제됩니다. 계속하시겠습니까?')) {
        const isReply = document.body.dataset.isReply === 'true';
        if (!isReply) {
            $('#receiverUserName').val('');
            $('#title').val('');
        }
        $('#content').val('');
        $('#messageType').val('');
        updateCharCounter();

        localStorage.removeItem('messageDraft');
    }
}

// 페이지 떠날 때 자동 임시저장
window.addEventListener('beforeunload', function () {
    const hasContent = $('#content').val().trim() || $('#title').val().trim();
    if (hasContent) {
        saveDraft();
    }
});
