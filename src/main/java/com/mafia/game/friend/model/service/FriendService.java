package com.mafia.game.friend.model.service;

import java.util.ArrayList;

import com.mafia.game.friend.model.vo.FriendList;
import com.mafia.game.friend.model.vo.FriendRelation;
import com.mafia.game.friend.model.vo.GameInvite;
import com.mafia.game.member.model.vo.Member;

public interface FriendService {
    
    /**
     * 친구 목록 조회
     */
    ArrayList<FriendList> getFriendList(String userName);
    
    /**
     * 받은 친구 요청 목록 조회
     */
    ArrayList<FriendRelation> getPendingRequests(String userName);
    
    /**
     * 받은 게임 초대 목록 조회
     */
    ArrayList<GameInvite> getPendingGameInvites(String userName);
    

    /**
     * 사용자 검색 (아이디 또는 닉네임으로)
     */
    Member searchUser(String searchKeyword);
    
    /**
     * 친구 요청 보내기
     */
    int sendFriendRequest(FriendRelation friendRequest);
    boolean checkExistingRelation(String userName1, String userName2);

    
    /**
     * 친구 요청 수락
     */
    int acceptFriendRequest(int relationNo, String userName);
    
    
    /**
     * 친구 요청 거절
     */
    int rejectFriendRequest(int relationNo, String userName);
    
    
    /**
     * 친구 삭제
     */
    int deleteFriend(String userName, String friendUserName);
    
    /**
     * 게임 초대 보내기
     */
    int sendGameInvite(GameInvite gameInvite);
    
    /**
     * 게임 초대 응답 (수락/거절)
     */
    int respondGameInvite(int inviteNo, String status, String userName);
    
    /**
     * 친구 여부 확인
     */
    boolean checkFriendship(String userName1, String userName2);
    
    /**
     * 기존 게임 초대 여부 확인 (중복 초대 방지)
     */
    boolean checkExistingGameInvite(String receiverName, int roomNo);
    
    /**
     * 게임방 검증 (방 상태, 인원, 권한을 한번에 체크)
     * @return null이면 정상, 문자열이면 오류 메시지
     */
    String validateGameRoom(int roomNo, String senderUserName);
    

}