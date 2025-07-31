import { state } from './state.js';
import { api } from './api.js';
import * as sockets from './sockets.js';
import * as ui from './ui.js';

let reloadTimer = null; 

export function sendChatMessage() {
    const msgText = ui.elements.chatInput.value.trim();
    if (msgText === '') return;
    let type = 'chat';
    if (state.isGaming && state.job) {
        const jobName = state.job.jobName;
        if (jobName.includes('Ghost') || jobName === 'spiritualists') type = 'death';
        else if (state.job.jobClass === 1) type = 'mafia';
        else if (jobName.includes('marriage')) type = 'lovers';
    }
    sockets.chatSocket_send({ type, userName: state.nickName, msg: msgText, roomNo: state.roomNo });
    ui.elements.chatInput.value = '';
}

export function toggleReady() {
    state.isReady = !state.isReady;
    sockets.roomSocket_send({ type: state.isReady ? 'ready' : 'unReady' });
    ui.elements.readyBtn.textContent = state.isReady ? 'Unready' : 'Ready';
}

export function reloadRoomAndUsers() {
    if (reloadTimer) {
        clearTimeout(reloadTimer);
    }
    reloadTimer = setTimeout(async () => {
        try {
            const newRoom = await api.reloadRoom();
            state.room = newRoom;
            state.isGaming = newRoom.isGaming === 'Y';
            state.isHost = newRoom.master === state.userName;
            const newUserListJSON = newRoom.userList || '[]';
            if (typeof updateUserListForVoice === 'function') {
                updateUserListForVoice(JSON.parse(newUserListJSON));
            }
            const nicks = await api.getUserNickList(newUserListJSON);
            state.userNickList = nicks;
            const deaths = await api.getUserDeathList();
            
            ui.loadUserPanel(nicks, deaths);
            updatePlayerClickHandlers();
            ui.updateReadyCount(JSON.parse(newRoom.readyUser || '[]').length);
            ui.updateLobbyButtons();
        } catch (error) {
            console.error("방 정보 리로드 실패:", error);
        } finally {
            reloadTimer = null;
        }
    }, 300);
}

export async function handleGameEnd(winner) {
    state.isGaming = false;
    let winMsg = '';
    switch (winner) {
        case 'MAFIA_WIN': winMsg = '마피아의 승리!'; break;
        case 'CITIZEN_WIN': winMsg = '시민의 승리!'; break;
        case 'NEUTRALITY_WIN': winMsg = '중립의 승리!'; break;
    }
    alert(`${winMsg} 게임이 종료되었습니다.`);
    if (state.startJob) {
        try { 
            await api.saveGameResult(winner);
            console.log("전적 저장 성공");
        }
        catch (e) { console.error("전적 저장 실패", e); }
    }
    setTimeout(() => {
        reloadRoomAndUsers();
        state.job = null;
        state.startJob = null;
    }, 3000);
}

export async function loadHintList() {
    try {
		if (state.isGaming && state.job) {
	        const hints = await api.getHintList();
	        ui.loadHintListUI(hints);
		}
    } catch (e) { console.error("힌트 로딩 실패", e); }
}

export function useAbility(targetUserName, targetNickName) {
    if (state.hasUsedAbility) {
        alert("이번 턴에 이미 능력을 사용했습니다."); return;
    }
    if (confirm(`[${targetNickName}]님에게 능력을 사용하시겠습니까?`)) {
        sockets.roomSocket_send({ type: 'useAbility', targetName: targetUserName });
        state.hasUsedAbility = true;
        alert("능력을 사용했습니다.");
    }
}

function updatePlayerClickHandlers() {
    const phaseText = ui.elements.phaseDisplay.textContent;
    const phase = phaseText.includes('밤') ? 'NIGHT' : phaseText.includes('투표') ? 'VOTE' : 'DAY';
    document.querySelectorAll('.slot').forEach(slot => {
        const targetUserName = slot.dataset.userName;
        const targetNickName = slot.querySelector('.player-name').value;
        const img = slot.querySelector('img.someNails');
        const isDead = img.src.includes('사망이미지');
        let canClick = false;
        if(state.isGaming && state.job) {
            const jobName = state.job.jobName;
            if (!isDead) {
                if (phase === 'NIGHT') {
                    if(['mafia', 'doctor', 'police', 'spy', 'necromancer'].includes(jobName) && jobName !== 'necromancerUsed') canClick = true;
                } else if (phase === 'VOTE') {
                    canClick = true;
                }
            } else {
                if (phase === 'NIGHT' && jobName === 'robber' && state.dayNo >= 1) {
                    canClick = true;
                }
            }
        }
        if (canClick) {
            img.style.cursor = 'pointer';
            img.onclick = () => useAbility(targetUserName, targetNickName);
        } else {
            img.style.cursor = 'default';
            img.onclick = null;
        }
    });
}

export async function syncGameState(phaseData) {
    console.log("Syncing game state from server:", phaseData);

    // 1. 전역 상태(state) 갱신
    state.hasUsedAbility = false;
    state.dayNo = phaseData.dayNo;
    state.room = phaseData.room;
    
    const myJob = phaseData.userJobs[state.userName];
    if (myJob) {
        state.job = myJob;
    }

    // 2. UI 업데이트를 위한 데이터 준비
    const newUserListJSON = state.room.userList || '[]';
    const nicks = await api.getUserNickList(newUserListJSON);
    state.userNickList = nicks;
    
    const userList = JSON.parse(newUserListJSON);
    const deaths = userList.map(userName => {
        const job = phaseData.userJobs[userName];
        return (job && (job.jobNo === 0 || job.jobName.includes("Ghost"))) ? 'dead' : 'alive';
    });

    // 3. UI 업데이트 함수들을 순서대로 지휘
    ui.elements.dayCount.textContent = `일차 : ${state.dayNo}일`;
    ui.loadUserPanel(nicks, deaths);
    updatePlayerClickHandlers();
    ui.updateReadyCount(JSON.parse(state.room.readyUser || '[]').length);
    ui.updateTimerUI(phaseData.phase, phaseData.remaining);
    ui.updateChatInputState(phaseData.phase);

    // 4. 추가 데이터 로드
    loadHintList();
}