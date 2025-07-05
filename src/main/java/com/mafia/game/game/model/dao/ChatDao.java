package com.mafia.game.game.model.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.game.model.vo.GameRoom;
import com.mafia.game.game.model.vo.Message;

import java.util.List;

@Repository
public class ChatDao {

    public GameRoom selectRoomById(SqlSessionTemplate sqlSession, int roonNo) {
        return sqlSession.selectOne("chatMapper.selectRoomById", roonNo);
    }

    public void insertMessage(SqlSessionTemplate sqlSession, Message message) {
        sqlSession.insert("chatMapper.insertMessage", message);
    }

    public List<Message> selectMessagesByRoom(SqlSessionTemplate sqlSession, int roomNo) {
        return sqlSession.selectList("chatMapper.selectMessagesByRoom", roomNo);
    }

    public void insertRoom(SqlSessionTemplate sqlSession, GameRoom room) {
        sqlSession.insert("chatMapper.insertRoom", room);
    }
}
