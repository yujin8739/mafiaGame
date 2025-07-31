// /js/game/main.js
import { state } from './state.js';
import * as sockets from './sockets.js';
import * as ui from './ui.js';
import * as gameLogic from './gameLogic.js';
import { api } from './api.js';

document.addEventListener('DOMContentLoaded', () => {
	// 초기 상태 점검
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

	// 초기화 순서
	ui.cacheElements();
	sockets.connectAllSockets();
	if (typeof initVoiceChat === 'function') {
		initVoiceChat({ userName: state.userName, roomNo: state.roomNo });
	}

	bindEventHandlers();
	loadInitialData();
	ui.updateLobbyButtons();

	// 절전 모드 등에서 복귀했을 때를 감지하는 이벤트 리스너
	document.addEventListener('visibilitychange', () => {
		// 탭이 다시 화면에 보일 때 (활성화될 때)
		if (document.visibilityState === 'visible') {
			const idleTime = Date.now() - state.lastActivityTime;
			console.log(`Page became visible. Idle time: ${idleTime / 1000} seconds.`);

			// 마지막 활동으로부터 40초 이상 지났다면, 서버로부터 연결이 끊겼다고 간주합니다.
			// (서버의 하트비트 타임아웃 45초보다 약간 짧게 설정하여 클라이언트가 먼저 인지하도록 함)
			if (idleTime > 40000) {
				if (state.isGaming) {
					// 게임 중일 때는, 일단 방 정보를 새로고침하여 내 상태를 확인합니다.
					console.log("Re-checking room status after long idle time...");
					gameLogic.reloadRoomAndUsers().then(() => {
						// 새로고침 후, userList에 내가 여전히 포함되어 있는지 확인합니다.
						const userList = state.room.userList ? JSON.parse(state.room.userList) : [];
						if (!userList.includes(state.userName)) {
							alert('장시간 비활성화로 인해 게임에서 제외되었습니다. 홈 화면으로 이동합니다.');
							window.location.href = '/';
						}
					});
				} else {
					// 로비 대기 중일 때는, 그냥 홈으로 보냅니다.
					alert('장시간 비활성화로 인해 서버와 연결이 끊어졌습니다. 홈 화면으로 이동합니다.');
					window.location.href = '/';
				}
			}
		}
	});
});

function bindEventHandlers() {
	ui.elements.startBtn.addEventListener('click', () => {
		const readyInfo = ui.elements.readyCount.textContent.split(':')[1].trim().split('/');
		const readyUserCount = parseInt(readyInfo[0]);
		const totalUserCount = parseInt(readyInfo[1]);
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
		if (confirm("힌트를 교란시키겠습니까?")) {
			sockets.roomSocket_send({ type: 'useAbility', targetName: null });
		}
	});
	ui.elements.chatArea.addEventListener("scroll", () => {
		if (ui.elements.chatArea.scrollTop === 0 && !state.isChatLoading && !state.isLastChatPage) {
			state.isChatLoading = true;
			state.chatPage++; // 다음 페이지 요청
			sockets.chatSocket_send({ type: 'loadMore', page: state.chatPage });
		}
	});
}

window.addEventListener('beforeunload', () => {
	// state.roomNo가 유효한 경우에만 (즉, 게임 방에 있는 경우에만) 신호를 보냅니다.
	if (state.roomNo && state.roomNo > 0) {
		// Beacon API는 JSON을 직접 보낼 수 없으므로 Blob 객체로 변환합니다.
		const data = JSON.stringify({ roomNo: state.roomNo });
		const blob = new Blob([data], { type: 'application/json; charset=UTF-8' });

		// 이 API는 브라우저가 페이지를 닫는 중에도 데이터 전송을 보장합니다.
		// 서버의 '/room/leave' 엔드포인트로 POST 요청을 보냅니다.
		navigator.sendBeacon('/room/leave', blob);
	}
});

async function loadInitialData() {
	const initialUserListJSON = state.initialRoom ? state.initialRoom.userList : '[]';
	try {
		const nicks = await api.getUserNickList(initialUserListJSON);
		state.userNickList = nicks;
		const deaths = await api.getUserDeathList();
		ui.loadUserPanel(nicks, deaths);
	} catch (e) {
		console.error("초기 유저 정보 로딩 실패", e);
	}
	gameLogic.loadHintList();
}