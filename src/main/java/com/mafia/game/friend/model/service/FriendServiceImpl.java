package com.mafia.game.friend.model.service;

import java.util.ArrayList;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.friend.model.dao.FriendDao;
import com.mafia.game.friend.model.vo.FriendList;
import com.mafia.game.friend.model.vo.FriendRelation;
import com.mafia.game.friend.model.vo.GameInvite;
import com.mafia.game.member.model.vo.Member;

@Service
@Transactional
public class FriendServiceImpl implements FriendService  {
	@Autowired
	private SqlSessionTemplate sqlSession;
	
	@Autowired
	private FriendDao friendDao;
	
	// 친구 목록 조회
    @Override
    public ArrayList<FriendList> getFriendList(String userName) {
        return friendDao.getFriendList(sqlSession, userName);
    }
    
    // 친구 요청 목록 조회 
    @Override
    public ArrayList<FriendRelation> getPendingRequests(String userName) {
        return friendDao.getPendingRequests(sqlSession, userName);
    }
    
    // 사용자 검색(ID, 넥네임)
    @Override
    public Member searchUser(String searchKeyword) {
        return friendDao.searchUser(sqlSession, searchKeyword);
    }
    
    // 이미 친구인지 확인
    @Override
    public boolean isAlreadyFriend(String userName, String friendUserName) {
        int count = friendDao.isAlreadyFriend(sqlSession, userName, friendUserName);
        return count > 0;
    }
    
    // 친구 요청이 있는지 확인
    @Override
    public boolean hasRequestBetween(String userName, String friendUserName) {
        int count = friendDao.hasRequestBetween(sqlSession, userName, friendUserName);
        return count > 0;
    }
    
    // 친구 요청 보내기
    @Override
    public int sendFriendRequest(FriendRelation friendRequest) {
        return friendDao.sendFriendRequest(sqlSession, friendRequest);
    }
    
    // 친구 요청 수락
    @Override
    public int acceptFriendRequest(int relationNo, String userName) {
        // 1. 친구 요청 수락 처리
        int result1 = friendDao.acceptFriendRequest(sqlSession, relationNo, userName);
        
        if (result1 > 0) {
            // 2. 친구 관계 정보 조회
            FriendRelation request = friendDao.getFriendRequestById(sqlSession, relationNo);
            
            if (request != null) {
                // 3. 친구 목록에 양방향 추가
                FriendList friend1 = new FriendList();
                friend1.setUserName(request.getRequesterName());
                friend1.setFriendUserName(request.getReceiverName());
                friend1.setStatus("ACCEPTED");
                
                FriendList friend2 = new FriendList();
                friend2.setUserName(request.getReceiverName());
                friend2.setFriendUserName(request.getRequesterName());
                friend2.setStatus("ACCEPTED");
                
                friendDao.addFriendToList(sqlSession, friend1);
                friendDao.addFriendToList(sqlSession, friend2);
            }
        }
        
        return result1;
    }
    
    @Override
    public int rejectFriendRequest(int relationNo, String userName) {
        return friendDao.rejectFriendRequest(sqlSession, relationNo, userName);
    }
    
    @Override
    public int deleteFriend(String userName, String friendUserName) {
        // 양방향 친구 관계 삭제
        int result1 = friendDao.deleteFriend(sqlSession, userName, friendUserName);
        int result2 = friendDao.deleteFriend(sqlSession, friendUserName, userName);
        
        return result1 + result2;
    }
    
    @Override
    public int sendGameInvite(GameInvite gameInvite) {
        return friendDao.sendGameInvite(sqlSession, gameInvite);
    }
    
    @Override
    public ArrayList<GameInvite> getPendingGameInvites(String userName) {
        return friendDao.getPendingGameInvites(sqlSession, userName);
    }
    
    @Override
    public int respondGameInvite(int inviteNo, String status, String userName) {
        return friendDao.respondGameInvite(sqlSession, inviteNo, status, userName);
    }
}
