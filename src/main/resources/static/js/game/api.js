import { state } from './state.js';

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
    getUserDeathList: () => ajaxRequest('/room/userDeathList', { roomNo: state.roomNo }),
    getHintList: () => ajaxRequest('/room/getRoomHintList', { roomNo: state.roomNo }),
    reloadRoom: () => ajaxRequest('/room/reloadRoom', { roomNo: state.roomNo }),
    saveGameResult: (winner) => ajaxRequest('/room/saveGameResult', {
        userName: state.userName,
        jobNo: state.startJob ? state.startJob.jobNo : 0,
        type: winner,
        startJobNo: state.startJob ? state.startJob.jobNo : 0,
    }),
};