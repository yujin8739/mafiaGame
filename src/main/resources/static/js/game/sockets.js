import * as gameLogic from './gameLogic.js';
import * as ui from './ui.js';
import { reconnectVoiceChat } from './voiceChat.js'; 

export let roomSocket, chatSocket, eventSocket;
let heartbeatInterval = null;
let reconnectAttempts = 0;

function createWebSocket(path, onMessageCallback, onOpenCallback) {
	const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
	const socket = new WebSocket(`${protocol}${location.host}${path}?roomNo=${window.MAFIA_GAME_STATE.roomNo}`);

	socket.onmessage = event => onMessageCallback(JSON.parse(event.data));
	socket.onopen = () => {
        console.log(`✅ WebSocket Connected: ${path}`);
        if(onOpenCallback) {
            onOpenCallback(socket);
        }
    };
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

	roomSocket = createWebSocket('/chat/gameRoom', handleRoomMessage, (socket) => {
        reconnectAttempts = 0;
        
        console.log("Requesting full game state sync from server upon connection...");
        socket.send(JSON.stringify({ type: 'requestSync' }));
    });
    
	chatSocket = createWebSocket('/chat/gameChat', handleChatMessage);
	eventSocket = createWebSocket('/chat/gameEvent', handleEventMessage);

	heartbeatInterval = setInterval(() => {
		if (roomSocket && roomSocket.readyState === WebSocket.OPEN) {
			roomSocket.send(JSON.stringify({ type: 'ping' }));
			window.MAFIA_GAME_STATE.lastActivityTime = Date.now();
		}
	}, 25000);
}

export function roomSocket_send(obj) { if (roomSocket && roomSocket.readyState === WebSocket.OPEN) roomSocket.send(JSON.stringify(obj)); }
export function chatSocket_send(obj) { if (chatSocket && chatSocket.readyState === WebSocket.OPEN) chatSocket.send(JSON.stringify(obj)); }

function handleRoomMessage(msg) {
	switch (msg.type) {
		case 'phase':
            window.MAFIA_GAME_STATE.currentPhase = msg.phase; 
			gameLogic.syncGameState(msg);
            reconnectVoiceChat(); 
			break;
		case 'jobInfo':
			window.MAFIA_GAME_STATE.job = msg.job;
			window.MAFIA_GAME_STATE.startJob = msg.startJob;
            // jobInfo를 받을 때 userJobs 맵에도 내 직업 정보를 업데이트
            if (window.MAFIA_GAME_STATE.userName) {
                window.MAFIA_GAME_STATE.userJobs[window.MAFIA_GAME_STATE.userName] = msg.job;
            }
			alert(`당신의 직업은 [${window.MAFIA_GAME_STATE.job.jobVisible}] 입니다.`);
			window.MAFIA_GAME_STATE.isGaming = true;
			ui.updateLobbyButtons();
            reconnectVoiceChat();
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
			window.MAFIA_GAME_STATE.isLastChatPage = true;
			window.MAFIA_GAME_STATE.isChatLoading = false;
			return;
		}
		const scrollBefore = ui.elements.chatArea.scrollHeight - ui.elements.chatArea.scrollTop;
		msg.messages.forEach(messageData => {
			ui.displayMessage(messageData, true);
		});
		const scrollAfter = ui.elements.chatArea.scrollHeight;
		ui.elements.chatArea.scrollTop = scrollAfter - scrollBefore;
		window.MAFIA_GAME_STATE.isChatLoading = false;
	} else {
		ui.displayMessage(msg);
	}
}

function handleEventMessage(msg) {
	ui.displayMessage(msg);

	if (msg.type === 'CONNECTION_STATE_CHANGED') {
		ui.updateUserConnectionStatus(msg.userName, msg.status);
	}

    if (window.MAFIA_GAME_STATE.isGaming) {
        if (msg.type === 'EVENT') {
            requestStateSync();
        }
        else if (msg.type === 'gameEnd') {
            gameLogic.handleGameEnd(msg.winner);
        }
    } else {
        if (['enter', 'leave', 'READY_STATE_CHANGED'].includes(msg.type)) {
            gameLogic.reloadRoomAndUsers();
        }
    }
}

export function requestStateSync() {
    if (roomSocket && roomSocket.readyState === WebSocket.OPEN) {
        console.log("Requesting state sync from server manually.");
        roomSocket.send(JSON.stringify({ type: 'requestSync' }));
    }
}