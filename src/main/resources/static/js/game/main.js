import './state.js'; // state를 전역으로 먼저 설정
import * as sockets from './sockets.js';
import * as ui from './ui.js';
import * as gameLogic from './gameLogic.js';
import { api } from './api.js';
import { initVoiceChat } from './voiceChat.js';

document.addEventListener('DOMContentLoaded', () => {
	const state = window.MAFIA_GAME_STATE;
	if (!state.initialRoom || !state.initialRoom.roomNo) {
		alert('방이 삭제되었습니다. 다른 게임에 접속해주세요');
		return (window.location.href = '/');
	}
	const userListArray = state.initialRoom.userList ? JSON.parse(state.initialRoom.userList) : [];
	if (state.isGaming && !userListArray.includes(state.userName)) {
		alert('이미 게임이 시작되었습니다. 다른 게임에 접속해주세요');
		return (window.location.href = '/');
	}
	ui.cacheElements();
	sockets.connectAllSockets();
	initVoiceChat({ userName: state.userName, roomNo: state.roomNo });
	bindEventHandlers();
	loadInitialData();
});

function bindEventHandlers() {
	const state = window.MAFIA_GAME_STATE;
	ui.elements.startBtn.addEventListener('click', () => {
		const [readyCount, totalCount] = ui.elements.readyCount.textContent.split(':')[1].trim().split('/');
		if (state.isHost && readyCount === totalCount && parseInt(totalCount) > 0) {
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
		if (confirm("힌트를 교란시키겠습니까?")) {
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

window.addEventListener('beforeunload', () => {
	const state = window.MAFIA_GAME_STATE;
	if (state.roomNo > 0) {
		navigator.sendBeacon('/room/leave', new Blob([JSON.stringify({ roomNo: state.roomNo })], { type: 'application/json; charset=UTF-8' }));
	}
});

async function loadInitialData() {
	const state = window.MAFIA_GAME_STATE;
	const initialUserListJSON = state.initialRoom?.userList || '[]';
	const userList = JSON.parse(initialUserListJSON);
	try {
		const nicks = await api.getUserNickList(initialUserListJSON);
		state.userNickList = nicks;
		const deaths = state.isGaming && state.initialUserJobs
			? userList.map(un => state.initialUserJobs[un]?.jobName.toLowerCase().toLowerCase().includes('ghost') ? 'dead' : 'alive')
			: Array(nicks.length).fill('alive');
		ui.loadUserPanel(nicks, deaths);
		const readyUsers = state.initialRoom.readyUser ? JSON.parse(state.initialRoom.readyUser) : [];
		ui.updateReadyCount(readyUsers.length, userList.length);
		ui.updateLobbyButtons();
		ui.updateChatInputState(state.currentPhase);
	} catch (e) {
		console.error("초기 유저 정보 로딩 실패", e);
	}
	if (state.isGaming) gameLogic.loadHintList();
}