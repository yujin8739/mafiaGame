// friendsidebar.js

function openFriendSidebar() {
    $('#sidebarOverlay').addClass('active');
    $('#friendSidebar').addClass('active');
    $('body').css('overflow', 'hidden');
    loadFriendData();
}

function closeFriendSidebar() {
    $('#sidebarOverlay').removeClass('active');
    $('#friendSidebar').removeClass('active');
    $('body').css('overflow', 'auto');
    hideSearchResult();
}

function switchTab(tabName) {
    $('.tab-btn').removeClass('active');
    $(`[data-tab="${tabName}"]`).addClass('active');

    $('.tab-content').removeClass('active');
    $(`#${tabName}Tab`).addClass('active');
}

function hideSearchResult() {
    $('#searchResult').removeClass('show');
}

function searchFriend() {
    const searchKeyword = $('#friendSearchInput').val().trim();

    if (!searchKeyword) {
        alert('검색어를 입력해주세요.');
        return;
    }

    $.ajax({
        url: '/friend/search',
        type: 'POST',
        data: { searchKeyword: searchKeyword },
        success: function (response) {
            if (response.success) {
                showSearchResult(response.user);
            } else {
                alert(response.message);
                hideSearchResult();
            }
        },
        error: function () {
            alert('검색 중 오류가 발생했습니다.');
        }
    });
}

function showSearchResult(user) {
    $('#searchedUserName').text(user.userName);
    $('#searchedUserNickname').text(user.nickName);
    $('#searchResult').addClass('show');
}

function sendFriendRequest() {
    const targetUser = $('#searchedUserName').text();

    $.ajax({
        url: '/friend/request',
        type: 'POST',
        data: { targetUserName: targetUser },
        success: function (response) {
            alert(response.message);
            if (response.success) {
                hideSearchResult();
                $('#friendSearchInput').val('');
            }
        },
        error: function () {
            alert('친구 요청 중 오류가 발생했습니다.');
        }
    });
}

function loadFriendData() {
    $.ajax({
        url: '/friend/notifications',
        type: 'GET',
        success: function (response) {
            if (response.success) {
                updateFriendList(response.friendList || []);
                updateRequestList(response.friendRequests || []);
                updateInviteList(response.gameInvites || []);
            }
        },
        error: function () {
            console.log('친구 데이터 로드 중 오류가 발생했습니다.');
        }
    });
}

function updateFriendList(friendList) {
    $('#friendList').empty();
    $('#friendCount').text(friendList.length);

    if (friendList.length === 0) {
        $('#friendsEmpty').show();
        return;
    }

    $('#friendsEmpty').hide();
    friendList.forEach(function (friend) {
        const friendItem = createFriendItem(friend);
        $('#friendList').append(friendItem);
    });
}

function updateRequestList(friendRequests) {
    $('#requestList').empty();
    $('#requestCount').text(friendRequests.length);

    if (friendRequests.length === 0) {
        $('#requestsEmpty').show();
        return;
    }

    $('#requestsEmpty').hide();
    friendRequests.forEach(function (request) {
        const requestItem = createRequestItem(request);
        $('#requestList').append(requestItem);
    });
}

function updateInviteList(gameInvites) {
    $('#inviteList').empty();
    $('#inviteCount').text(gameInvites.length);

    if (gameInvites.length === 0) {
        $('#invitesEmpty').show();
        return;
    }

    $('#invitesEmpty').hide();
    gameInvites.forEach(function (invite) {
        const inviteItem = createInviteItem(invite);
        $('#inviteList').append(inviteItem);
    });
}

function createFriendItem(friend) {
    return $(`
        <li class="friend-item">
            <div class="friend-avatar">
                👤
                <div class="online-status offline"></div>
            </div>
            <div class="friend-info">
                <div class="friend-name">${friend.friendNickName || friend.friendUserName}</div>
                <div class="friend-status">오프라인</div>
            </div>
            <div class="friend-actions">
                <button class="action-btn message-btn" onclick="sendMessage('${friend.friendUserName}')">쪽지</button>
                <button class="action-btn invite-btn" onclick="inviteToGame('${friend.friendUserName}')">초대</button>
                <button class="action-btn remove-btn" onclick="removeFriend('${friend.friendUserName}')">삭제</button>
            </div>
        </li>
    `);
}

function createRequestItem(request) {
    return $(`
        <li class="friend-item">
            <div class="friend-avatar">👤</div>
            <div class="friend-info">
                <div class="friend-name">${request.requesterNickName || request.requesterName}</div>
                <div class="friend-status">친구 요청을 보냈습니다</div>
            </div>
            <div class="friend-actions">
                <button class="action-btn accept-btn" onclick="acceptFriendRequest(${request.relationNo})">수락</button>
                <button class="action-btn reject-btn" onclick="rejectFriendRequest(${request.relationNo})">거절</button>
            </div>
        </li>
    `);
}

function createInviteItem(invite) {
    return $(`
        <li class="friend-item">
            <div class="friend-avatar">🎮</div>
            <div class="friend-info">
                <div class="friend-name">${invite.senderNickName || invite.senderName}</div>
                <div class="friend-status">${invite.roomName}에 초대했습니다</div>
            </div>
            <div class="friend-actions">
                <button class="action-btn accept-btn" onclick="acceptGameInvite(${invite.inviteNo})">수락</button>
                <button class="action-btn reject-btn" onclick="rejectGameInvite(${invite.inviteNo})">거절</button>
            </div>
        </li>
    `);
}

function acceptFriendRequest(relationNo) {
    $.post('/friend/accept', { relationNo }, function (response) {
        alert(response.message);
        if (response.success) loadFriendData();
    });
}

function rejectFriendRequest(relationNo) {
    $.post('/friend/reject', { relationNo }, function (response) {
        alert(response.message);
        if (response.success) loadFriendData();
    });
}

function removeFriend(friendUserName) {
    if (!confirm('정말로 이 친구를 삭제하시겠습니까?')) return;

    $.post('/friend/delete', { friendUserName }, function (response) {
        alert(response.message);
        if (response.success) loadFriendData();
    });
}

function acceptGameInvite(inviteNo) {
    $.post('/friend/invite/respond', { inviteNo, status: 'ACCEPTED' }, function (response) {
        alert(response.message);
        if (response.success) loadFriendData();
    });
}

function rejectGameInvite(inviteNo) {
    $.post('/friend/invite/respond', { inviteNo, status: 'REJECTED' }, function (response) {
        alert(response.message);
        if (response.success) loadFriendData();
    });
}

function sendMessage(userName) {
    window.location.href = '/message/write';
}

function inviteToGame(userName) {
    alert('현재 기능 구현중 쫌만 기다리삼.');
}

// 이벤트 바인딩
$(document).ready(function () {
    $(document).on('click', '#closeSidebarBtn, #sidebarOverlay', closeFriendSidebar);
    $(document).on('click', '.tab-btn', function () {
        const tabName = $(this).data('tab');
        switchTab(tabName);
    });

    $(document).on('keydown', function (e) {
        if (e.which === 27) closeFriendSidebar();
    });

    $(document).on('click', '#searchFriendBtn', searchFriend);
    $(document).on('keypress', '#friendSearchInput', function (e) {
        if (e.which === 13) searchFriend();
    });

    $(document).on('click', '#sendRequestBtn', sendFriendRequest);
});
