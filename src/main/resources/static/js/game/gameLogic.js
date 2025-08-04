import { api } from './api.js';
import * as sockets from './sockets.js';
import * as ui from './ui.js';
import { reconnectVoiceChat } from './voiceChat.js';

let reloadTimer = null; 

/**
 * UI가 아닌, 신뢰할 수 있는 전역 상태(userJobs)와 jobName 기준으로 사망 여부를 확인합니다.
 * @param {string} userName - 확인할 유저의 아이디
 * @returns {boolean} - 죽었으면 true, 아니면 false
 */
function isPlayerDead(userName) {
    const jobs = window.MAFIA_GAME_STATE.userJobs;
    const playerJob = jobs ? jobs[userName] : null;

    if (!playerJob || !playerJob.jobName) return true; // 직업 정보가 없으면 죽은 것으로 간주

    return playerJob.jobName.toLowerCase().includes('ghost');
}

export function sendChatMessage() {
    const state = window.MAFIA_GAME_STATE;
    const msgText = ui.elements.chatInput.value.trim();
    if (msgText === '') return;
    let type = 'chat';

    if (state.isGaming && state.job) {
        const amIDead = state.job.jobName.toLowerCase().includes('ghost');

        if (amIDead) {
            type = 'death';
        } else if (state.currentPhase === 'NIGHT') { 
            // 마피아 팀 전체가 대화 가능하도록 jobClass 검사
            if (state.job.jobClass === 1 || state.job.jobClass === 5) {
                type = 'mafia';
            } else if (state.job.jobName.includes('marriage')) {
                type = 'lovers';
            }
        }
    }
    sockets.chatSocket_send({ type, userName: state.nickName, msg: msgText, roomNo: state.roomNo });
    ui.elements.chatInput.value = '';
}

export function toggleReady() {
    window.MAFIA_GAME_STATE.isReady = !window.MAFIA_GAME_STATE.isReady;
    sockets.roomSocket_send({ type: window.MAFIA_GAME_STATE.isReady ? 'ready' : 'unReady' });
    ui.elements.readyBtn.textContent = window.MAFIA_GAME_STATE.isReady ? 'Unready' : 'Ready';
}

export function reloadRoomAndUsers() {
    if (reloadTimer) { clearTimeout(reloadTimer); }
    reloadTimer = setTimeout(async () => {
        try {
            const newRoom = await api.reloadRoom();
            const state = window.MAFIA_GAME_STATE;
            state.room = newRoom;
            state.isGaming = newRoom.isGaming === 'Y';
            state.isHost = newRoom.master === state.userName;
            const newUserListJSON = newRoom.userList || '[]';
            
            const nicks = await api.getUserNickList(newUserListJSON);
            state.userNickList = nicks;
            
            ui.loadUserPanel(); 
            updatePlayerClickHandlers();
            ui.updateReadyCount(JSON.parse(newRoom.readyUser || '[]').length);
            ui.updateLobbyButtons();
            
            reconnectVoiceChat();

        } catch (error) {
            console.error("방 정보 리로드 실패:", error);
        } finally {
            reloadTimer = null;
        }
    }, 300);
}

export async function handleGameEnd(winner) {
    const state = window.MAFIA_GAME_STATE;
    state.isGaming = false;
    state.currentPhase = 'WAITING';
    
    let winMsg = '';
    switch (winner) {
        case 'MAFIA_WIN': winMsg = '마피아의 승리!'; break;
        case 'CITIZEN_WIN': winMsg = '시민의 승리!'; break;
        case 'NEUTRALITY_WIN': winMsg = '중립의 승리!'; break;
    }
    alert(`${winMsg} 게임이 종료되었습니다.`);
    
    reconnectVoiceChat();

    if (state.startJob) {
        try { 
            await api.saveGameResult(winner);
        }
        catch (e) { console.error("전적 저장 실패", e); }
    }
    
    setTimeout(() => {
        state.job = null;
        state.startJob = null;
        state.userJobs = {}; 
        reloadRoomAndUsers();
    }, 3000);
}

export async function loadHintList() {
    try {
		if (window.MAFIA_GAME_STATE.isGaming && window.MAFIA_GAME_STATE.job) {
	        const hints = await api.getHintList();
	        ui.loadHintListUI(hints);
		}
    } catch (e) { console.error("힌트 로딩 실패", e); }
}

export function useAbility(targetUserName, targetNickName) {
    const state = window.MAFIA_GAME_STATE;
    
    if (state.hasUsedAbility) {
        alert("이번 턴에 이미 능력을 사용했습니다.");
        return;
    }
    
    const jobName = state.job ? state.job.jobName : '';
    const canTargetDead = jobName === 'robber' || jobName === 'necromancer';
    
    if (isPlayerDead(targetUserName) && !canTargetDead) {
        alert("죽은 유저는 지목할 수 없습니다.");
        return;
    }

    if (confirm(`[${targetNickName}]님에게 능력을 사용하시겠습니까?`)) {
        sockets.roomSocket_send({ type: 'useAbility', targetName: targetUserName });
        state.hasUsedAbility = true;
        alert("능력을 사용했습니다.");
    }
}

export function votePlayer(targetUserName, targetNickName) {
    if (window.MAFIA_GAME_STATE.hasVoted) {
        alert("이번 턴에 이미 투표했습니다.");
        return;
    }

    if (isPlayerDead(targetUserName)) {
        alert("죽은 유저에게는 투표할 수 없습니다.");
        return;
    }

    if (confirm(`[${targetNickName}]님에게 투표하시겠습니까? 이것은 능력 사용이 아닙니다.`)) {
        sockets.roomSocket_send({ type: 'vote', targetName: targetUserName });
        window.MAFIA_GAME_STATE.hasVoted = true;
        alert(`[${targetNickName}]님에게 투표했습니다.`);
    }
}

function updatePlayerClickHandlers() {
    const state = window.MAFIA_GAME_STATE;
    const currentPhase = state.currentPhase;
    
    const amISelfDead = state.job ? state.job.jobName.toLowerCase().includes('ghost') : false;

    document.querySelectorAll('.slot').forEach(slot => {
        const targetUserName = slot.dataset.userName;
        if (!targetUserName) return;
        
        const targetNickName = slot.querySelector('.player-name').value;
        const img = slot.querySelector('img.someNails');

        let canClick = false;
        let clickAction = null;
        
        if (!amISelfDead && state.isGaming && state.job && state.userName !== targetUserName) {
            if (currentPhase === 'NIGHT') {
                canClick = true;
                clickAction = useAbility;
            } else if (currentPhase === 'VOTE') {
                canClick = true;
                clickAction = votePlayer;
            }
        }
        
        if (canClick && clickAction) {
            img.style.cursor = 'pointer';
            img.onclick = () => clickAction(targetUserName, targetNickName);
        } else {
            img.style.cursor = 'default';
            img.onclick = null;
        }
    });
}

export async function syncGameState(phaseData) {
    console.log("Syncing game state from server:", phaseData);

    const state = window.MAFIA_GAME_STATE;
    state.hasUsedAbility = false;
    state.hasVoted = false;
    state.dayNo = phaseData.dayNo;
    state.room = phaseData.room;
    
    state.userJobs = phaseData.userJobs; 
    state.job = phaseData.userJobs[state.userName];

    if (ui.elements.dayCount) {
        ui.elements.dayCount.textContent = `DAY ${state.dayNo}`;
    }

    ui.loadUserPanel(); 
    
    updatePlayerClickHandlers();
    ui.updateReadyCount(JSON.parse(state.room.readyUser || '[]').length);
    ui.updateTimerUI(phaseData.phase, phaseData.remaining);
    ui.updateChatInputState(phaseData.phase);
    
    loadHintList();
}