const config = window.MAFIA_GAME_CONFIG || {};

export const state = {
    roomNo: config.roomNo || 0,
    nickName: config.nickName || '익명',
    userName: config.userName || '',
    initialRoom: config.initialRoom || null,
    
    room: config.initialRoom || null,
    job: null,
    startJob: null,
    dayNo: config.initialRoom ? config.initialRoom.dayNo : 0,
    isGaming: config.initialRoom && config.initialRoom.isGaming === 'Y',
    isHost: config.initialRoom && config.initialRoom.master === config.userName,
    isReady: false,
    userNickList: [],
    hasUsedAbility: false,
	
	chatPage: 1,
	isChatLoading: false,
	isLastChatPage: false,
};