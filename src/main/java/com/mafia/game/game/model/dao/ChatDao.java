package com.mafia.game.game.model.dao;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ChatDao {

    public GameRoom selectRoomById(SqlSessionTemplate sqlSession, int roonNo) {
        return sqlSession.selectOne("chatMapper.selectRoomById", roonNo);
    }

    public void insertMessage(SqlSessionTemplate sqlSession, Message message) {
        sqlSession.insert("chatMapper.insertMessage", message);
    }

    public List<Message> selectMessagesByRoom(SqlSessionTemplate sqlSession, int roomNo, RowBounds rowBounds) {
        return sqlSession.selectList("chatMapper.selectMessagesByRoom", roomNo, rowBounds);
    }

    public void insertRoom(SqlSessionTemplate sqlSession, GameRoom room) {
        sqlSession.insert("chatMapper.insertRoom", room);
    }
    
    public String selectEvent(SqlSessionTemplate sqlSession, int eventNo, String userName) {
    	Map<String, Object> param = new HashMap<>();
    	param.put("eventNo", eventNo);
    	param.put("userName", userName);

    	return sqlSession.selectOne("chatMapper.selectEvent", param);
    }
}
