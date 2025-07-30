import { state } from './state.js';
import { useAbility } from './gameLogic.js';

export const elements = {};
let phaseTimer = null;

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
    elements.hintListContainer = $("#hintListContainer"); // jQuery 객체
    elements.controlPanel = document.getElementById("controlPanel");
}

export function displayMessage(msg, isPrepended = false) {
    const div = document.createElement("div");

    // ✨ [핵심 수정] 메시지 타입에 따른 클래스 분기 강화
    if (['enter', 'leave', 'gameEnd', 'READY_STATE_CHANGED'].includes(msg.type)) {
        div.classList.add("system-bubble");
        div.textContent = msg.msg;
    } else if (msg.type === 'EVENT') {
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
            const senderDiv = document.createElement("div");
            senderDiv.classList.add("chat-sender");
            senderDiv.textContent = msg.userName;
            const contentDiv = document.createElement("div");
            contentDiv.textContent = msg.msg;
            div.appendChild(senderDiv);
            div.appendChild(contentDiv);
        }
    } else {
        // ✨ 일반 채팅, 마피아 채팅, 사망자 채팅 등을 여기서 처리
        
        // 1. 방향 결정 (right/left)
        div.classList.add(msg.userName === state.nickName ? "right" : "left");
        
        // 2. 채팅 타입에 따른 버블 스타일 클래스 추가
        //    (예: chat-bubble, mafia-bubble, death-bubble)
        div.classList.add(`${msg.type}-bubble`);

        // 3. 발신자와 내용 추가
        const sender = document.createElement("div");
        sender.className = "chat-sender";
        sender.textContent = msg.userName;
        const content = document.createElement("div");
        content.textContent = msg.msg;
        div.append(sender, content);
    }
    
    // 최종적으로 생성된 div를 채팅창에 추가
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

export function updateLobbyButtons() {
    const readyDiv = document.querySelector('.readyDiv');
    if (state.isGaming) {
        readyDiv.style.display = 'none';
    } else {
        readyDiv.style.display = 'flex';
        if (state.isHost) {
            elements.startBtn.style.display = 'block';
            elements.readyBtn.style.display = 'none';
        } else {
            elements.startBtn.style.display = 'none';
            elements.readyBtn.style.display = 'block';
        }
    }
}

export function updateReadyCount(count) {
    const totalPlayers = state.userNickList.length;
    elements.readyCount.textContent = `Ready: ${count}/${totalPlayers > 1 ? totalPlayers - 1 : 0}`;
}

export function updateTimerUI(phase, time) {
    if(phaseTimer) clearInterval(phaseTimer);
    let remaining = time;
    const phaseKR = { NIGHT: '밤', DAY: '낮', VOTE: '투표' }[phase] || '대기';
    elements.phaseDisplay.textContent = `현재 단계: ${phaseKR}`;
    elements.timerDisplay.textContent = `남은 시간: ${remaining}초`;

    phaseTimer = setInterval(() => {
        remaining--;
        if(remaining < 0) remaining = 0;
        elements.timerDisplay.textContent = `남은 시간: ${remaining}초`;
        if (remaining <= 0) clearInterval(phaseTimer);
    }, 1000);
}

export function updateChatInputState(phase) {
    const jobName = state.job ? state.job.jobName : '';
    let enabled = false;
    let placeholder = "대화할 수 없습니다.";

    if (!state.isGaming || phase === 'DAY') {
        enabled = true; placeholder = "메시지를 입력하세요...";
    } else if (phase === 'NIGHT') {
        if (state.job.jobClass === 1 || jobName.includes('marriage')) {
            enabled = true; placeholder = "팀과 대화하세요...";
        }
    }
    if (jobName.includes('Ghost') || jobName === 'spiritualists') {
        enabled = true; placeholder = "사망자와 대화하세요...";
    }
    
    elements.chatInput.disabled = !enabled;
    elements.chatInput.placeholder = placeholder;
}

export function loadUserPanel(nicks, deaths) {
    elements.playerPanelContainer.innerHTML = '';
    const userList = JSON.parse(state.room.userList || '[]');
    nicks.forEach((name, index) => {
        const div = document.createElement('div');
        div.className = 'slot';
        div.dataset.userName = userList[index] || '';
        
        const panels = document.createElement('div');
        panels.className = 'panels';
        
        const isDead = deaths[index] === 'dead';
        const someNails = document.createElement('img');
        someNails.className = 'someNails';
        someNails.src = isDead ? '/images/사망이미지.png' : '/images/생존이미지.png';
        
        const input = document.createElement('input');
        input.className = 'player-name';
        input.type = 'text';
        input.readOnly = true;
        input.value = name;
        
        panels.appendChild(someNails);
        div.appendChild(input);
        div.appendChild(panels);
        elements.playerPanelContainer.appendChild(div);
    });
    updatePlayerClickHandlers();
}

export function updatePlayerClickHandlers() {
    const phaseText = elements.phaseDisplay.textContent;
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

export function loadHintListUI(hintList) {
    elements.hintListContainer.empty();
    hintList.forEach(hint => {
        const div = $("<div>").addClass("slot-phone");
        const panels = $("<div>").addClass("panels-phone").append($("<img>").addClass("someNails").attr("src", "/images/생존이미지.png"));
        const input = $("<input>").addClass("player-name-phone").attr({type: "text", readonly: true}).val(`${hint.userNick}: ${hint.hint}`);
        div.append(panels).append(input);
        elements.hintListContainer.append(div);
    });
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