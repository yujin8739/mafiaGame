let voice_userName;
let voice_roomNo;

let voiceSocket;
let currentVoiceSocketPath = null; 

const voiceState = {
    localStream: null,
    peers: {},
    remoteAudioEls: {},
    localMuted: false,
    isVoiceStarted: false,
};
const voiceUI = {};

export function initVoiceChat(config) {
    voice_userName = config.userName;
    voice_roomNo = config.roomNo;

    voiceUI.startBtn = document.getElementById('startVoiceBtn');
    voiceUI.stopBtn = document.getElementById('stopVoiceBtn');
    voiceUI.muteBtn = document.getElementById('muteVoiceBtn');
    voiceUI.container = document.getElementById('voiceRemoteContainer');

    if (voiceUI.startBtn) voiceUI.startBtn.onclick = startVoiceChat;
    if (voiceUI.stopBtn) voiceUI.stopBtn.onclick = stopVoiceChat;
    if (voiceUI.muteBtn) voiceUI.muteBtn.onclick = toggleMute;
}

export function reconnectVoiceChat() {
    if (!voiceState.isVoiceStarted) return;
    
    const newSocketPath = getCorrectVoiceSocketUrl();
    console.log(`[Voice Check] Current: ${currentVoiceSocketPath}, Required: ${newSocketPath}`);

    if (newSocketPath !== currentVoiceSocketPath) {
        console.log("음성 채널이 변경되어 재연결합니다...");
        
        if (voiceSocket && voiceSocket.readyState === WebSocket.OPEN) {
            voiceSocket.onclose = null; 
            voiceSocket.close(1000);
        }
        clearVoiceConnections();
        
        // 불안정한 네트워크 환경을 고려하여 약간의 딜레이 후 연결 시도
        const randomDelay = 200 + Math.random() * 500;
        setTimeout(() => {
            connectVoiceSocket(newSocketPath);
        }, randomDelay);
    }
}

function getCorrectVoiceSocketUrl() {
    const state = window.MAFIA_GAME_STATE;
    const myJob = state.job;
    const phase = state.currentPhase;
    
    // 게임중이 아니면 모두가 대화 가능
    if (!state.isGaming) return '/voice/alive';
    
    // 내 직업 정보가 없으면 음성채팅 불가
    if (!myJob || !myJob.jobName) return null;

    const amIDead = myJob.jobName.toLowerCase().includes('ghost');

    // 1. 사망자는 언제나 사망자 채널에서 대화
    if (amIDead) return '/voice/dead';

    // 2. 살아있는 경우, 게임 단계별 규칙 적용
    switch (phase) {
        case 'DAY':
            return '/voice/alive';
        case 'NIGHT':
            // 마피아 팀(jobClass 1 또는 5)은 마피아 채널에서 대화
            if (myJob.jobClass === 1 || myJob.jobClass === 5) {
                return '/voice/mafia';
            }
            return null;
        case 'VOTE':
            return null;
        default:
            return null;
    }
}


function connectVoiceSocket(socketPath) {
    currentVoiceSocketPath = socketPath;

    // 연결할 소켓 경로가 없으면(대화가 불가능한 상태) 모든 연결을 종료하고 함수 종료
    if (!socketPath) {
        console.warn("음성 채팅이 허용되지 않는 상태입니다. 모든 연결을 종료합니다.");
        clearVoiceConnections(); 
        return;
    }
    
    const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
    const voiceUrl = protocol + location.host + socketPath + '?roomNo=' + voice_roomNo;

    voiceSocket = new WebSocket(voiceUrl);
    voiceSocket.onopen = () => {
        console.log(`✅ Voice-Signal WebSocket 연결 성공: ${voiceUrl}`);
        voiceSafeSend({ type: 'voiceReady' });
    };
    voiceSocket.onmessage = event => handleVoiceSignal(JSON.parse(event.data));
    voiceSocket.onerror = err => console.error(`❌ Voice-Signal WebSocket 오류 (${voiceUrl})`, err);
    voiceSocket.onclose = () => {
        console.warn(`⚠️ Voice-Signal WebSocket 끊김. (${voiceUrl})`);
        if (currentVoiceSocketPath === socketPath) {
            currentVoiceSocketPath = null;
        }
    };
}

function voiceSafeSend(obj) {
    if (voiceSocket && voiceSocket.readyState === WebSocket.OPEN) {
        voiceSocket.send(JSON.stringify(obj));
    }
}

async function startVoiceChat() {
    if (voiceState.isVoiceStarted) return;
    
    try {
        voiceState.localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
        if (voiceState.localStream.getAudioTracks().length === 0) {
            alert("연결된 마이크 장치가 없거나 인식되지 않습니다.");
            voiceState.localStream = null;
            return;
        }
        
        voiceState.isVoiceStarted = true;
        voiceUI.startBtn.style.display = "none";
        voiceUI.stopBtn.style.display = "inline-block";
        voiceUI.muteBtn.style.display = "inline-block";
        
        reconnectVoiceChat();

    } catch (e) {
        console.error("❌ startVoiceChat 오류:", e);
        if (e.name === "NotAllowedError" || e.name === "PermissionDeniedError") {
            alert('마이크 사용 권한이 거부되었습니다. 브라우저 설정에서 마이크 권한을 허용하고 페이지를 새로고침 해주세요.');
        } else if (e.name === "NotFoundError" || e.name === "DevicesNotFoundError") {
            alert('사용 가능한 마이크 장치를 찾을 수 없습니다.');
        } else {
            alert('마이크를 시작하는 중 오류가 발생했습니다.');
        }
    }
}

function stopVoiceChat() {
    if (!voiceState.isVoiceStarted) return;

    if (voiceState.localStream) {
        voiceState.localStream.getTracks().forEach(t => t.stop());
        voiceState.localStream = null;
    }
    
    if (voiceSocket && voiceSocket.readyState === WebSocket.OPEN) {
        voiceSocket.onclose = null;
        voiceSocket.close(1000);
    }
    clearVoiceConnections();
    currentVoiceSocketPath = null;
    
    voiceState.isVoiceStarted = false;
    voiceUI.startBtn.style.display = "inline-block";
    voiceUI.stopBtn.style.display = "none";
    voiceUI.muteBtn.style.display = "none";
}

function toggleMute() {
    if (!voiceState.localStream) return;
    voiceState.localMuted = !voiceState.localMuted;
    voiceState.localStream.getAudioTracks().forEach(track => track.enabled = !voiceState.localMuted);
    voiceUI.muteBtn.textContent = voiceState.localMuted ? ' 🔇 ' : ' 🔈 ';
}

function clearVoiceConnections() {
    for (const peerUser in voiceState.peers) {
        if (voiceState.peers[peerUser]) {
            voiceState.peers[peerUser].close();
        }
    }
    voiceState.peers = {};
    if(voiceUI.container) voiceUI.container.innerHTML = '';
    voiceState.remoteAudioEls = {};
}

function handleVoiceSignal(msg) {
    const fromUser = msg.from;
    if (fromUser === voice_userName) return;

    switch (msg.type) {
        case 'voiceReady':
             if (voiceState.isVoiceStarted) createAndSendOffer(fromUser);
             break;
        case 'voiceOffer': onVoiceOffer(msg); break;
        case 'voiceAnswer': onVoiceAnswer(msg); break;
        case 'voiceCandidate': onVoiceCandidate(msg); break;
    }
}

async function createAndSendOffer(targetUser) {
    const pc = createVoicePeerConnection(targetUser);
    try {
        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        voiceSafeSend({ type: 'voiceOffer', target: targetUser, sdp: offer });
    } catch (e) {
        console.error(`Offer 생성 실패 to ${targetUser}:`, e);
    }
}

async function onVoiceOffer(msg) {
    const fromUser = msg.from;
    const pc = createVoicePeerConnection(fromUser);
    try {
        await pc.setRemoteDescription(new RTCSessionDescription(msg.sdp));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        voiceSafeSend({ type: 'voiceAnswer', target: fromUser, sdp: answer });
    } catch (e) { console.error(`Answer 생성 실패 for ${fromUser}:`, e); }
}

async function onVoiceAnswer(msg) {
    const fromUser = msg.from;
    const pc = voiceState.peers[fromUser];
    if (pc && pc.signalingState === "have-local-offer") {
        try {
            await pc.setRemoteDescription(new RTCSessionDescription(msg.sdp));
        } catch (e) { console.error(`Set Answer 실패 from ${fromUser}:`, e); }
    } else {
        console.warn(`[WebRTC] 잘못된 상태(${pc?.signalingState})에서 Answer를 받아 무시합니다: from ${fromUser}`);
    }
}

function onVoiceCandidate(msg) {
    const fromUser = msg.from;
    const pc = voiceState.peers[fromUser];
    if (pc && msg.candidate) {
        pc.addIceCandidate(new RTCIceCandidate(msg.candidate))
          .catch(e => {}); 
    }
}

function createVoicePeerConnection(remoteUser) {
    if (voiceState.peers[remoteUser]) {
        voiceState.peers[remoteUser].close();
    }
    const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });
    voiceState.peers[remoteUser] = pc;
    if (voiceState.localStream) {
        voiceState.localStream.getAudioTracks().forEach(track => pc.addTrack(track, voiceState.localStream));
    }
    pc.onicecandidate = e => {
        if (e.candidate) {
            voiceSafeSend({ type: 'voiceCandidate', target: remoteUser, candidate: e.candidate });
        }
    };
    pc.ontrack = e => { addRemoteAudio(remoteUser, e.streams[0]); };
    pc.onconnectionstatechange = () => {
        if (['failed', 'disconnected', 'closed'].includes(pc.connectionState)) {
            removeRemoteAudio(remoteUser);
            delete voiceState.peers[remoteUser];
        }
    };
    return pc;
}

function addRemoteAudio(peerUser, stream) {
    if (!voiceUI.container) return;
    let el = voiceState.remoteAudioEls[peerUser];
    if (!el) {
        el = document.createElement('audio');
        el.id = 'remote-' + peerUser;
        el.autoplay = true;
        el.playsInline = true;
        voiceUI.container.appendChild(el);
        voiceState.remoteAudioEls[peerUser] = el;
    }
    el.srcObject = stream;
    el.muted = false; 
    el.play().catch(err => {});
}

function removeRemoteAudio(peerUser) {
    const el = voiceState.remoteAudioEls[peerUser];
    if (el) {
        el.srcObject = null;
        el.remove();
        delete voiceState.remoteAudioEls[peerUser];
    }
}