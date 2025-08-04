import * as sockets from './sockets.js';

export const elements = {};
let phaseTimer = null;
let syncCheckTimeout = null;

export function cacheElements() {
    elements.chatArea = document.querySelector('#chatArea');
    elements.chatInput = document.getElementById('chat');
    elements.sendBtn = document.getElementById('sendBtn');
    elements.startBtn = document.getElementById('startBtn');
    elements.readyBtn = document.getElementById('readyBtn');
    elements.readyCount = document.getElementById('readyCount');
    elements.dayCount = document.getElementById('dayCount');
    elements.phaseDisplay = document.getElementById('phaseDisplay');
    elements.timerDisplay = document.getElementById('timerDisplay');
    elements.playerPanelContainer = document.getElementById('playerPanelContainer');
    elements.phoneModal = document.getElementById("phoneModal");
    elements.togglePhoneBtn = document.getElementById("togglePhoneModalBtn");
    elements.closePhoneBtn = document.getElementById("closePhoneModalBtn");
    elements.hackBtn = document.getElementById("hackBtn");
    elements.hintListContainer = $("#hintListContainer");
    elements.controlPanel = document.getElementById("controlPanel");
}

export function displayMessage(msg, isPrepended = false) {
    const div = document.createElement("div");
    if (['enter', 'leave', 'gameEnd', 'READY_STATE_CHANGED'].includes(msg.type)) {
        div.classList.add("system-bubble");
        div.textContent = msg.msg;
    } else if (msg.type === 'EVENT') {
        //서버에서 보낸 JSON 문자열을 파싱하여 이미지와 텍스트를 올바르게 표시
        div.classList.add("system-bubble", "event-bubble");
        try {
            const eventData = JSON.parse(msg.msg);
            const wrapper = document.createElement("div");
            wrapper.classList.add("event-wrapper");
            
            const img = document.createElement("img");
            img.src = eventData.imageUrl;
            img.alt = "event image";
            img.classList.add("event-image");
            
            const contentDiv = document.createElement("div");
            contentDiv.classList.add("event-text");
            contentDiv.textContent = eventData.content;
            
            const senderDiv = document.createElement("div");
            senderDiv.classList.add("chat-sender");
            senderDiv.textContent = msg.userName;
            
            wrapper.appendChild(img);
            wrapper.appendChild(contentDiv);
            div.appendChild(senderDiv);
            div.appendChild(wrapper);
        } catch (e) {
            // 파싱에 실패하면(일반 텍스트 이벤트일 경우) 그냥 텍스트만 표시
            const senderDiv = document.createElement("div");
            senderDiv.classList.add("chat-sender");
            senderDiv.textContent = msg.userName;
            const contentDiv = document.createElement("div");
            contentDiv.textContent = msg.msg;
            div.appendChild(senderDiv);
            div.appendChild(contentDiv);
        }
    } else {
        div.classList.add(msg.userName === window.MAFIA_GAME_STATE.nickName ? "right" : "left");
        div.classList.add(`${msg.type}-bubble`);
        const sender = document.createElement("div");
        sender.className = "chat-sender";
        sender.textContent = msg.userName;
        const content = document.createElement("div");
        content.textContent = msg.msg;
        div.append(sender, content);
    }
    if (isPrepended) {
        elements.chatArea.insertBefore(div, elements.chatArea.firstChild);
    } else {
        const wasAtBottom = (elements.chatArea.scrollHeight - elements.chatArea.scrollTop - elements.chatArea.clientHeight) < 50;
        elements.chatArea.appendChild(div);
        if (wasAtBottom) {
            scrollToBottom(elements.chatArea);
        }
    }
}

// 이하 코드는 변경 없음
export function updateLobbyButtons() {
    const readyDiv = document.querySelector('.readyDiv');
    if (!readyDiv) return;

    if (window.MAFIA_GAME_STATE.isGaming) {
        readyDiv.style.display = 'none';
    } else {
        readyDiv.style.display = 'flex';
        if (elements.startBtn) {
            elements.startBtn.style.display = window.MAFIA_GAME_STATE.isHost ? 'block' : 'none';
        }
        if (elements.readyBtn) {
            elements.readyBtn.style.display = window.MAFIA_GAME_STATE.isHost ? 'none' : 'block';
        }
    }
}

export function updateReadyCount(count) {
    const totalPlayers = window.MAFIA_GAME_STATE.userNickList.length > 0 ? window.MAFIA_GAME_STATE.userNickList.length - 1 : 0;
    elements.readyCount.textContent = `Ready: ${count}/${totalPlayers}`;
}

export function updateTimerUI(phase, time) {
    if (phaseTimer) clearInterval(phaseTimer);
    if (syncCheckTimeout) clearTimeout(syncCheckTimeout);

    let remaining = time;
    const phaseKR = { NIGHT: '밤', DAY: '낮', VOTE: '투표' }[phase] || '대기';
    elements.phaseDisplay.textContent = `현재 단계: ${phaseKR}`;
    elements.timerDisplay.textContent = `남은 시간: ${remaining}초`;

    if (time <= 0) return;

    phaseTimer = setInterval(() => {
        remaining--;
        if (remaining < 0) remaining = 0;
        elements.timerDisplay.textContent = `남은 시간: ${remaining}초`;

        if (remaining <= 0) {
            clearInterval(phaseTimer);
            const phaseWhenTimerEnded = window.MAFIA_GAME_STATE.currentPhase;
            syncCheckTimeout = setTimeout(() => {
                if (window.MAFIA_GAME_STATE.currentPhase === phaseWhenTimerEnded) {
                    sockets.requestStateSync();
                }
            }, 1000);
        }
    }, 1000);
}

export function updateChatInputState(phase) {
    const state = window.MAFIA_GAME_STATE;
    const myJob = state.job;
    let enabled = false;
    let placeholder = "대화할 수 없습니다.";

    if (myJob && myJob.jobName) {
        const isGhost = myJob.jobName.toLowerCase().includes('ghost');
        if (isGhost) {
            enabled = true;
            placeholder = "사망자와 대화하세요...";
        } else if (!state.isGaming || phase === 'DAY') {
            enabled = true;
            placeholder = "메시지를 입력하세요...";
        } else if (phase === 'NIGHT') {
            if (myJob.jobClass === 1 || myJob.jobName.includes('marriage')) {
                enabled = true;
                placeholder = "팀과 대화하세요...";
            }
        }
    } else if (!state.isGaming) {
        enabled = true;
        placeholder = "메시지를 입력하세요...";
    }
    
    elements.chatInput.disabled = !enabled;
    elements.chatInput.placeholder = placeholder;
}

export function loadUserPanel() {
    const state = window.MAFIA_GAME_STATE;
    const nicks = state.userNickList || [];
    const userList = state.room && state.room.userList ? JSON.parse(state.room.userList) : [];
    
    elements.playerPanelContainer.innerHTML = '';

    nicks.forEach((name, index) => {
        const userName = userList[index] || '';
        const userJob = state.userJobs[userName];
        
        const isDead = userJob ? userJob.jobName.toLowerCase().includes('ghost') : false;

        // 1. 전체 슬롯 컨테이너
        const div = document.createElement('div');
        div.className = 'slot';
        div.dataset.userName = userName;
        
        // 2. 플레이어 이름 (상단)
        const input = document.createElement('input');
        input.className = 'player-name';
        input.type = 'text';
        input.readOnly = true;
        input.value = name;
        div.appendChild(input);

        // 3. 이미지 패널 (중단)
        const panels = document.createElement('div');
        panels.className = 'panels';
        const someNails = document.createElement('img');
        someNails.className = 'someNails';
        someNails.src = isDead ? '/godDaddy_etc/statusprofile/사망이미지.png' : '/godDaddy_etc/statusprofile/생존이미지.png';
        panels.appendChild(someNails);
        div.appendChild(panels);

        // 4. 직업 표시 (하단) - 현재 로그인한 유저에게만 표시
        if (state.isGaming && userName === state.userName && state.job) {
            const jobDisplay = document.createElement('div');
            jobDisplay.className = 'player-job';
            jobDisplay.textContent = `[${state.job.jobVisible}]`;
            
            // 스타일 직접 지정
            jobDisplay.style.color = '#ffc107'; // 강조되는 노란색
            jobDisplay.style.fontWeight = 'bold';
            jobDisplay.style.textAlign = 'center';
            jobDisplay.style.width = '100%'; // 슬롯 너비에 맞춰 중앙 정렬
            jobDisplay.style.marginTop = '4px'; // 이미지와의 간격
            
            div.appendChild(jobDisplay);
        }
        
        elements.playerPanelContainer.appendChild(div);
    });
}

export function updateUserConnectionStatus(userName, status) {
    const slot = document.querySelector(`.slot[data-user-name="${userName}"]`);
    if (!slot) return;
    let icon = slot.querySelector('.connection-icon');
    if (!icon) {
        icon = document.createElement('div');
        icon.className = 'connection-icon';
        icon.style.position = 'absolute';
        icon.style.top = '5px';
        icon.style.right = '5px';
        icon.style.color = 'red';
        icon.style.fontWeight = 'bold';
        slot.appendChild(icon);
    }
    if (status === 'disconnected') {
        icon.textContent = '❌';
        icon.style.display = 'block';
    } else {
        icon.style.display = 'none';
    }
}

export function loadHintListUI(hintList) {
    elements.hintListContainer.empty();

    if (hintList && Array.isArray(hintList)) {
        hintList.forEach(hint => {
            const div = $("<div>").addClass("slot-phone");
            const panels = $("<div>").addClass("panels-phone").append($("<img>").addClass("someNails").attr("src", "/godDaddy_etc/statusprofile/생존이미지.png"));
            const input = $("<input>").addClass("player-name-phone").attr({ type: "text", readonly: true }).val(`${hint.userNick}: ${hint.hint}`);
            div.append(panels).append(input);
            elements.hintListContainer.append(div);
        });
    }
}

export function togglePhoneModal() {
    const isHidden = elements.phoneModal.style.display === "none" || elements.phoneModal.style.display === "";
    elements.phoneModal.style.display = isHidden ? "block" : "none";
    elements.togglePhoneBtn.style.display = isHidden ? "none" : "block";
    elements.controlPanel.style.display = isHidden ? "none" : "block";
}

function scrollToBottom(element) {
    element.scrollTop = element.scrollHeight;
}