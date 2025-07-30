// /js/game/main.js
import { state } from './state.js';
import * as sockets from './sockets.js';
import * as ui from './ui.js';
import * as gameLogic from './gameLogic.js';
import { api } from './api.js';

document.addEventListener('DOMContentLoaded', () => {
    if (!state.initialRoom || !state.initialRoom.roomNo) {
        alert('방이 삭제되었습니다. 다른 게임에 접속해주세요');
        window.location.href = '/';
        return;
    }
    const userListArray = state.initialRoom.userList ? JSON.parse(state.initialRoom.userList) : [];
    if (state.isGaming && !userListArray.includes(state.userName)) {
        alert('이미 게임이 시작되었습니다. 다른 게임에 접속해주세요');
        window.location.href = '/';
        return;
    }

    ui.cacheElements();
    sockets.connectAllSockets();
    if (typeof initVoiceChat === 'function') {
        initVoiceChat({ userName: state.userName, roomNo: state.roomNo });
    }
    bindEventHandlers();
    loadInitialData();
    ui.updateLobbyButtons();
});

function bindEventHandlers() {
    ui.elements.startBtn.addEventListener('click', () => {
        const readyInfo = ui.elements.readyCount.textContent.split(':')[1].trim().split('/');
        const readyUserCount = parseInt(readyInfo[0], 10);
        const totalUserCount = parseInt(readyInfo[1], 10);
        if (state.isHost && readyUserCount === totalUserCount) {
            sockets.roomSocket_send({ type: 'start' });
        } else {
            alert('모든 플레이어가 준비되어야 시작할 수 있습니다.');
        }
    });

    ui.elements.readyBtn.addEventListener('click', gameLogic.toggleReady);
    ui.elements.sendBtn.addEventListener('click', gameLogic.sendChatMessage);
    ui.elements.chatInput.addEventListener('keydown', e => {
        if (e.key === 'Enter' && !e.isComposing) {
            e.preventDefault(); 
            gameLogic.sendChatMessage();
        }
    });
    ui.elements.togglePhoneBtn.addEventListener("click", ui.togglePhoneModal);
    ui.elements.closePhoneBtn.addEventListener("click", ui.togglePhoneModal);
    ui.elements.hackBtn.addEventListener('click', () => {
        if(confirm("힌트를 교란시키겠습니까?")) {
            sockets.roomSocket_send({ type: 'useAbility', targetName: null });
        }
    });

    ui.elements.chatArea.addEventListener("scroll", () => {
        if (ui.elements.chatArea.scrollTop === 0 && !state.isChatLoading && !state.isLastChatPage) {
            state.isChatLoading = true;
            state.chatPage++;
            sockets.chatSocket_send({ type: 'loadMore', page: state.chatPage });
        }
    });
}

async function loadInitialData() {
    const initialUserListJSON = state.initialRoom ? state.initialRoom.userList : '[]';
    try {
        const nicks = await api.getUserNickList(initialUserListJSON);
        state.userNickList = nicks;
        const deaths = await api.getUserDeathList();
        ui.loadUserPanel(nicks, deaths);
        ui.updateReadyCount(JSON.parse(state.initialRoom.readyUser || '[]').length);
    } catch (e) {
        console.error("초기 유저 정보 로딩 실패", e);
    }
    gameLogic.loadHintList();
}