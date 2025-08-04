const config = window.MAFIA_GAME_CONFIG || {};

const state = {
    roomNo: config.roomNo || 0,
    nickName: config.nickName || '익명',
    userName: config.userName || '',
    initialRoom: config.initialRoom || null,
    
    room: config.initialRoom || null,
    job: null,
    startJob: null,
    // userJobs를 모든 상태 판단의 유일한 기준으로 사용하기 위해 전역 상태에 유지합니다.
    userJobs: config.initialUserJobs || {}, 
    
    dayNo: config.initialRoom ? config.initialRoom.dayNo : 0,
    isGaming: config.initialRoom && config.initialRoom.isGaming === 'Y',
    isHost: config.initialRoom && config.initialRoom.master === config.userName,
    isReady: false,
    userNickList: [],
	chatPage: 1,
	isChatLoading: false,
	isLastChatPage: false,
	lastActivityTime: Date.now(),
	hasUsedAbility: false,
	hasVoted: false,
    currentPhase: 'WAITING',
};

window.MAFIA_GAME_STATE = state;
