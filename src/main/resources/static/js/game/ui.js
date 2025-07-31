import { state } from './state.js';

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
    elements.hintListContainer = $("#hintListContainer");
    elements.controlPanel = document.getElementById("controlPanel");
}

export function displayMessage(msg, isPrepended = false) {
    const div = document.createElement("div");
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
        div.classList.add(msg.userName === state.nickName ? "right" : "left");
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
    if (time <= 0) return;
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
        someNails.src = isDead ? '/godDaddy_etc/statusprofile/사망이미지.png' : '/godDaddy_etc/statusprofile/생존이미지.png';
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
    hintList.forEach(hint => {
        const div = $("<div>").addClass("slot-phone");
        const panels = $("<div>").addClass("panels-phone").append($("<img>").addClass("someNails").attr("src", "/godDaddy_etc/statusprofile/생존이미지.png"));
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