async function ajaxRequest(url, data) {
    try {
        return await $.ajax({ url, data });
    } catch (error) {
        console.error(`API 요청 실패 [${url}]:`, error);
        throw error;
    }
}

export const api = {
    getUserNickList: (userList) => ajaxRequest('/room/userNickList', { userList }),
    getUserDeathList: () => ajaxRequest('/room/userDeathList', { roomNo: window.MAFIA_GAME_STATE.roomNo }),
    getHintList: () => ajaxRequest('/room/getRoomHint', { roomNo: window.MAFIA_GAME_STATE.roomNo }),
    reloadRoom: () => ajaxRequest('/room/reloadRoom', { roomNo: window.MAFIA_GAME_STATE.roomNo }),
    saveGameResult: (winner) => ajaxRequest('/room/saveGameResult', {
        userName: window.MAFIA_GAME_STATE.userName,
        jobNo: window.MAFIA_GAME_STATE.startJob ? window.MAFIA_GAME_STATE.startJob.jobNo : 0,
        type: winner,
        startJobNo: window.MAFIA_GAME_STATE.startJob ? window.MAFIA_GAME_STATE.startJob.jobNo : 0,
    }),
};