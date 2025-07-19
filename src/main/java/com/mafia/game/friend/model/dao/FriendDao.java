package com.mafia.game.friend.model.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;

import com.mafia.game.friend.model.vo.FriendList;
import com.mafia.game.friend.model.vo.FriendRelation;
import com.mafia.game.friend.model.vo.GameInvite;
import com.mafia.game.member.model.vo.Member;

import org.springframework.stereotype.Repository;

@Repository
public class FriendDao {
    
    /**
     * 친구 목록 조회
     */
    public ArrayList<FriendList> getFriendList(SqlSessionTemplate sqlSession, String userName) {
        return new ArrayList<>(sqlSession.selectList("friendMapper.getFriendList", userName));
    }
    
    /**
     * 친구 요청 목록 조회 (받은 요청)
     */
    public ArrayList<FriendRelation> getPendingRequests(SqlSessionTemplate sqlSession, String userName) {
        return new ArrayList<>(sqlSession.selectList("friendMapper.getPendingRequests", userName));
    }
    
    /**
     * 사용자 검색 (아이디 또는 닉네임)
     */
    public Member searchUser(SqlSessionTemplate sqlSession, String searchKeyword) {
        return sqlSession.selectOne("friendMapper.searchUser", searchKeyword);
    }
    
    /**
     * 이미 친구인지 확인
     */
    public int isAlreadyFriend(SqlSessionTemplate sqlSession, String userName, String friendUserName) {
        Map<String, String> params = new HashMap<>();
        params.put("userName", userName);
        params.put("friendUserName", friendUserName);
        return sqlSession.selectOne("friendMapper.isAlreadyFriend", params);
    }
    
    /**
     * 친구 요청이 있는지 확인
     */
    public int hasRequestBetween(SqlSessionTemplate sqlSession, String userName, String friendUserName) {
        Map<String, String> params = new HashMap<>();
        params.put("userName", userName);
        params.put("friendUserName", friendUserName);
        return sqlSession.selectOne("friendMapper.hasRequestBetween", params);
    }
    
    /**
     * 친구 요청 보내기
     */
    public int sendFriendRequest(SqlSessionTemplate sqlSession, FriendRelation friendRequest) {
        return sqlSession.insert("friendMapper.sendFriendRequest", friendRequest);
    }
    
    /**
     * 친구 요청 수락
     */
    public int acceptFriendRequest(SqlSessionTemplate sqlSession, int relationNo, String userName) {
        Map<String, Object> params = new HashMap<>();
        params.put("relationNo", relationNo);
        params.put("userName", userName);
        return sqlSession.update("friendMapper.acceptFriendRequest", params);
    }
    
    /**
     * 친구 요청 거절
     */
    public int rejectFriendRequest(SqlSessionTemplate sqlSession, int relationNo, String userName) {
        Map<String, Object> params = new HashMap<>();
        params.put("relationNo", relationNo);
        params.put("userName", userName);
        return sqlSession.update("friendMapper.rejectFriendRequest", params);
    }
    
    /**
     * 친구 요청 정보 조회
     */
    public FriendRelation getFriendRequestById(SqlSessionTemplate sqlSession, int relationNo) {
        return sqlSession.selectOne("friendMapper.getFriendRequestById", relationNo);
    }
    
    /**
     * 친구 목록에 추가
     */
    public int addFriendToList(SqlSessionTemplate sqlSession, FriendList friendList) {
        return sqlSession.insert("friendMapper.addFriendToList", friendList);
    }
    
    /**
     * 친구 삭제
     */
    public int deleteFriend(SqlSessionTemplate sqlSession, String userName, String friendUserName) {
        Map<String, String> params = new HashMap<>();
        params.put("userName", userName);
        params.put("friendUserName", friendUserName);
        return sqlSession.delete("friendMapper.deleteFriend", params);
    }
    
    /**
     * 게임 초대 보내기
     */
    public int sendGameInvite(SqlSessionTemplate sqlSession, GameInvite gameInvite) {
        return sqlSession.insert("friendMapper.sendGameInvite", gameInvite);
    }
    
    /**
     * 게임 초대 목록 조회 (받은 초대)
     */
    public ArrayList<GameInvite> getPendingGameInvites(SqlSessionTemplate sqlSession, String userName) {
        return new ArrayList<>(sqlSession.selectList("friendMapper.getPendingGameInvites", userName));
    }
    
    /**
     * 게임 초대 응답
     */
    public int respondGameInvite(SqlSessionTemplate sqlSession, int inviteNo, String status, String userName) {
        Map<String, Object> params = new HashMap<>();
        params.put("inviteNo", inviteNo);
        params.put("status", status);
        params.put("userName", userName);
        return sqlSession.update("friendMapper.respondGameInvite", params);
    }

    public boolean checkExistingRelation(SqlSessionTemplate sqlSession, String userName1, String userName2) {
        Map<String, Object> params = new HashMap<>();
        params.put("userName1", userName1);
        params.put("userName2", userName2);
        
        Integer count = sqlSession.selectOne("friendMapper.checkExistingRelation", params);
        return count != null && count > 0;
    }
    
   
}