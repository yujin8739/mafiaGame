// /js/game/sockets.js (전체 코드)

import { state } from './state.js';
import * as gameLogic from './gameLogic.js';
import * as ui from './ui.js';

export let roomSocket, chatSocket, eventSocket;
let heartbeatInterval = null;
let reconnectAttempts = 0;

function createWebSocket(path, onMessageCallback) {
	const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
	const socket = new WebSocket(`${protocol}${location.host}${path}?roomNo=${state.roomNo}`);

	socket.onmessage = event => onMessageCallback(JSON.parse(event.data));
	socket.onopen = () => console.log(`✅ WebSocket Connected: ${path}`);
	socket.onerror = (e) => console.error(`❌ WebSocket Error: ${path}`, e);

	socket.onclose = (event) => {
		console.warn(`⚠️ WebSocket Closed: ${path}. Code: ${event.code}`);
		if (event.code === 1000 || event.code >= 4000) {
			if (heartbeatInterval) clearInterval(heartbeatInterval);
			return;
		}

		reconnectAttempts++;
		const delay = Math.min(30000, Math.pow(2, reconnectAttempts) * 1000 + 3000);

		console.log(`Abnormal closure. Trying to reconnect all sockets in ${delay / 1000} seconds...`);
		setTimeout(() => {
			console.log("Reconnecting all sockets...");
			connectAllSockets();
		}, delay);
	};
	return socket;
}

export function connectAllSockets() {
	if (heartbeatInterval) clearInterval(heartbeatInterval);
	if (roomSocket) roomSocket.close(1000);
	if (chatSocket) chatSocket.close(1000);
	if (eventSocket) eventSocket.close(1000);

	roomSocket = createWebSocket('/chat/gameRoom', handleRoomMessage);
	chatSocket = createWebSocket('/chat/gameChat', handleChatMessage);
	eventSocket = createWebSocket('/chat/gameEvent', handleEventMessage);

	heartbeatInterval = setInterval(() => {
		if (roomSocket && roomSocket.readyState === WebSocket.OPEN) {
			roomSocket.send(JSON.stringify({ type: 'ping' }));
			state.lastActivityTime = Date.now();
		}
	}, 25000);
}

export function roomSocket_send(obj) { if (roomSocket && roomSocket.readyState === WebSocket.OPEN) roomSocket.send(JSON.stringify(obj)); }
export function chatSocket_send(obj) { if (chatSocket && chatSocket.readyState === WebSocket.OPEN) chatSocket.send(JSON.stringify(obj)); }

function handleRoomMessage(msg) {
	switch (msg.type) {
		case 'phase':
			state.dayNo = msg.dayNo;
			gameLogic.setPhase(msg.phase, msg.remaining);
			break;
		case 'jobInfo':
			state.job = msg.job;
			state.startJob = msg.startJob;
			alert(`당신의 직업은 [${state.job.jobVisible}] 입니다.`);
			state.isGaming = true;
			ui.updateLobbyButtons();
			break;
		case 'abilityResult':
			alert(msg.msg);
			break;
		case 'error':
			alert(msg.msg);
			break;
	}
}

function handleChatMessage(msg) {
	if (msg.type === 'load') {
		if (msg.messages.length === 0) {
			state.isLastChatPage = true;
			state.isChatLoading = false;
			return;
		}

		const scrollBefore = ui.elements.chatArea.scrollHeight - ui.elements.chatArea.scrollTop;

		msg.messages.forEach(messageData => {
			ui.displayMessage(messageData, true);
		});

		const scrollAfter = ui.elements.chatArea.scrollHeight;
		ui.elements.chatArea.scrollTop = scrollAfter - scrollBefore;

		state.isChatLoading = false;
	} else {
		ui.displayMessage(msg);
	}
}

function handleEventMessage(msg) {
	ui.displayMessage(msg);

	if (msg.type === 'CONNECTION_STATE_CHANGED') {
		ui.updateUserConnectionStatus(msg.userName, msg.status);
	}

	if (['enter', 'leave', 'EVENT', 'READY_STATE_CHANGED'].includes(msg.type)) {
		gameLogic.reloadRoomAndUsers();
	}

	if (msg.type === 'gameEnd') {
		gameLogic.handleGameEnd(msg.winner);
	}
}