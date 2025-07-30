/**
 * =================================================================
 * Mafia Game - WebRTC Voice Chat Module (with Dedicated WebSocket)
 * =================================================================
 * 이 파일은 음성 시그널링 전용 WebSocket을 사용하여 음성 채팅 로직을 처리합니다.
 */

// 전역 스코프에서 공유될 변수
let voice_userName;
let voice_roomNo;

let voiceSocket;
const voiceState = {
    localStream: null,
    peers: {},
    remoteAudioEls: {},
    localMuted: false,
    isVoiceStarted: false,
};
const voiceUI = {};

/**
 * 음성 채팅 모듈 초기화 함수
 */
function initVoiceChat(config) {
    voice_userName = config.userName;
    voice_roomNo = config.roomNo;

    voiceUI.startBtn = document.getElementById('startVoiceBtn');
    voiceUI.stopBtn = document.getElementById('stopVoiceBtn');
    voiceUI.muteBtn = document.getElementById('muteVoiceBtn');
    voiceUI.container = document.getElementById('voiceRemoteContainer');

    voiceUI.startBtn.onclick = startVoiceChat;
    voiceUI.stopBtn.onclick = stopVoiceChat;
    voiceUI.muteBtn.onclick = toggleMute;
}


/**
 * 음성 시그널링 WebSocket 연결
 */
function connectVoiceSocket() {
    if (voiceSocket && voiceSocket.readyState === WebSocket.OPEN) {
        return;
    }
    const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
    const voiceUrl = protocol + location.host + '/chat/gameMainVoice?roomNo=' + voice_roomNo;
    voiceSocket = new WebSocket(voiceUrl);

    voiceSocket.onopen = () => {
        console.log('✅ Voice-Signal WebSocket 연결 성공');
        if (voiceState.isVoiceStarted) {
             voiceSafeSend({ type: 'voiceReady' });
        }
    };

    voiceSocket.onmessage = event => {
        const msgData = JSON.parse(event.data);
        handleVoiceSignal(msgData);
    };

    voiceSocket.onerror = err => console.error('❌ Voice-Signal WebSocket 오류', err);

    voiceSocket.onclose = () => {
        console.warn('⚠️ Voice-Signal WebSocket 끊김.');
    };
}

function voiceSafeSend(obj) {
    if (voiceSocket && voiceSocket.readyState === WebSocket.OPEN) {
        voiceSocket.send(JSON.stringify(obj));
    } else {
        console.warn('Voice-Signal WebSocket이 열려있지 않아 전송 불가', obj);
    }
}

async function startVoiceChat() {
    if (voiceState.isVoiceStarted) return;
    
    connectVoiceSocket();

    try {
        voiceState.localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });

        if (voiceState.localStream.getAudioTracks().length === 0) {
            alert("연결된 마이크 장치가 없거나 인식되지 않습니다.");
            voiceState.localStream = null;
            return;
        }

        let localPrev = document.getElementById('localAudio');
        if (!localPrev) {
            localPrev = document.createElement('audio');
            localPrev.id = 'localAudio';
            localPrev.autoplay = true;
            localPrev.muted = true;
            localPrev.playsInline = true;
            voiceUI.container.appendChild(localPrev);
        }
        localPrev.srcObject = voiceState.localStream;
        await localPrev.play().catch(e => console.warn("로컬 프리뷰 재생 실패:", e));

        voiceState.isVoiceStarted = true;
        voiceUI.startBtn.style.display = "none";
        voiceUI.stopBtn.style.display = "inline-block";
        voiceUI.muteBtn.style.display = "inline-block";

        voiceSafeSend({ type: 'voiceReady' });

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
    voiceState.isVoiceStarted = false;

    if (voiceState.localStream) {
        voiceState.localStream.getTracks().forEach(t => t.stop());
        voiceState.localStream = null;
    }
    
    if (voiceSocket && voiceSocket.readyState === WebSocket.OPEN) {
        voiceSocket.close();
    }
    
    clearVoiceConnections();
    
    voiceUI.startBtn.style.display = "inline-block";
    voiceUI.stopBtn.style.display = "none";
    voiceUI.muteBtn.style.display = "none";
}

function toggleMute() {
    if (!voiceState.localStream) return;
    voiceState.localMuted = !voiceState.localMuted;
    voiceState.localStream.getAudioTracks().forEach(track => track.enabled = !voiceState.localMuted);
    voiceUI.muteBtn.textContent = voiceState.localMuted ? ' 🔇 ' : ' 🔈 ';
    voiceSafeSend({ type: voiceState.localMuted ? 'voiceMute' : 'voiceUnmute' });
}

function clearVoiceConnections() {
    for (const peerUser in voiceState.peers) {
        if (voiceState.peers[peerUser]) {
            voiceState.peers[peerUser].close();
        }
    }
    voiceState.peers = {};
    Array.from(voiceUI.container.children).forEach(child => child.remove());
    voiceState.remoteAudioEls = {};
}

function handleVoiceSignal(msg) {
    const fromUser = msg.from;
    if (fromUser === voice_userName) return;

    switch (msg.type) {
        case 'voiceReady':
             if (voiceState.isVoiceStarted) {
                createAndSendOffer(fromUser);
             }
             break;
        case 'voiceOffer': onVoiceOffer(msg); break;
        case 'voiceAnswer': onVoiceAnswer(msg); break;
        case 'voiceCandidate': onVoiceCandidate(msg); break;
        case 'voiceMute': muteRemoteAudio(fromUser, true); break;
        case 'voiceUnmute': muteRemoteAudio(fromUser, false); break;
    }
}

async function createAndSendOffer(targetUser) {
    if (voiceState.peers[targetUser]) return;
    
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
    if (pc) {
        try {
            await pc.setRemoteDescription(new RTCSessionDescription(msg.sdp));
        } catch (e) { console.error(`Set Answer 실패 from ${fromUser}:`, e); }
    }
}

function onVoiceCandidate(msg) {
    const fromUser = msg.from;
    const pc = voiceState.peers[fromUser];
    if (pc && msg.candidate) {
        pc.addIceCandidate(new RTCIceCandidate(msg.candidate))
          .catch(e => console.error(`Add ICE Candidate 실패 from ${fromUser}:`, e));
    }
}

function createVoicePeerConnection(remoteUser) {
    if (voiceState.peers[remoteUser]) return voiceState.peers[remoteUser];

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
            if (voiceState.peers[remoteUser]) {
                voiceState.peers[remoteUser].close();
                delete voiceState.peers[remoteUser];
            }
        }
    };

    return pc;
}

function addRemoteAudio(peerUser, stream) {
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
    el.play().catch(err => console.warn(`원격 오디오 재생 실패: ${peerUser}`, err));
}

function removeRemoteAudio(peerUser) {
    const el = voiceState.remoteAudioEls[peerUser];
    if (el) {
        el.srcObject = null;
        el.remove();
        delete voiceState.remoteAudioEls[peerUser];
    }
}

function muteRemoteAudio(user, mute) {
    const el = voiceState.remoteAudioEls[user];
    if (el) el.muted = mute;
}