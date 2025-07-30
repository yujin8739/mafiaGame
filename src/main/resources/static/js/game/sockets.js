import { state } from './state.js';
import * as gameLogic from './gameLogic.js';
import * as ui from './ui.js';

export let roomSocket, chatSocket, eventSocket;

function createWebSocket(path, onMessageCallback) {
    const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
    const socket = new WebSocket(`${protocol}${location.host}${path}?roomNo=${state.roomNo}`);
    socket.onmessage = event => onMessageCallback(JSON.parse(event.data));
    socket.onopen = () => console.log(`✅ WebSocket Connected: ${path}`);
    socket.onerror = (e) => console.error(`❌ WebSocket Error: ${path}`, e);
    socket.onclose = () => console.warn(`⚠️ WebSocket Closed: ${path}`);
    return socket;
}

export function connectAllSockets() {
    roomSocket = createWebSocket('/chat/gameRoom', handleRoomMessage);
    chatSocket = createWebSocket('/chat/gameChat', handleChatMessage);
    eventSocket = createWebSocket('/chat/gameEvent', handleEventMessage);
}

export function roomSocket_send(obj) { if (roomSocket && roomSocket.readyState === WebSocket.OPEN) roomSocket.send(JSON.stringify(obj)); }
export function chatSocket_send(obj) { if (chatSocket && chatSocket.readyState === WebSocket.OPEN) chatSocket.send(JSON.stringify(obj)); }
export function eventSocket_send(obj) { if (eventSocket && eventSocket.readyState === WebSocket.OPEN) eventSocket.send(JSON.stringify(obj)); }

function handleRoomMessage(msg) {
    switch(msg.type) {
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
            state.isChatLoading = false; // 로딩 완료
            return;
        }
        
        const scrollBefore = ui.elements.chatArea.scrollHeight - ui.elements.chatArea.scrollTop;
        
        // 메시지를 시간순(오래된->최신)으로 받은 상태이므로 그대로 추가
        msg.messages.forEach(messageData => {
            ui.displayMessage(messageData, true); // isPrepended = true
        });

        const scrollAfter = ui.elements.chatArea.scrollHeight;
        ui.elements.chatArea.scrollTop = scrollAfter - scrollBefore;
        
        state.isChatLoading = false; // 로딩 완료
    } else {
        // 일반 채팅 메시지 처리
        ui.displayMessage(msg);
    }
}

function handleEventMessage(msg) {
    ui.displayMessage(msg);
    if (['enter', 'leave', 'EVENT' ,'READY_STATE_CHANGED'].includes(msg.type)) {
        gameLogic.reloadRoomAndUsers();
    }
    if (msg.type === 'gameEnd') {
        gameLogic.handleGameEnd(msg.winner);
    }
}