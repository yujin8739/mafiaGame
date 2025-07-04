package com.mafia.game.webSocket.model.dao;

import com.mafia.game.webSocket.model.vo.GameRoom;
import com.mafia.game.webSocket.model.vo.Message;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatDao {

    public List<GameRoom> selectAllRooms(SqlSessionTemplate sqlSession) {
        return sqlSession.selectList("chatMapper.selectAllRooms");
    }

    public GameRoom selectRoomById(SqlSessionTemplate sqlSession, int roonNo) {
        return sqlSession.selectOne("chatMapper.selectRoomById", roonNo);
    }

    public void insertMessage(SqlSessionTemplate sqlSession, Message message) {
        sqlSession.insert("chatMapper.insertMessage", message);
    }

    public List<Message> selectMessagesByRoom(SqlSessionTemplate sqlSession, int roonNo) {
        return sqlSession.selectList("chatMapper.selectMessagesByRoom", roonNo);
    }

    public void insertRoom(SqlSessionTemplate sqlSession, GameRoom room) {
        sqlSession.insert("chatMapper.insertRoom", room);
    }
}
