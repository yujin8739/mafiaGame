package com.mafia.game.shop.model.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.mafia.game.shop.model.vo.Shop;

@Repository
public class ShopDao {

    public int insertArtwork(SqlSession session, Shop shop) {
        return session.insert("artshopMapper.insertArtwork", shop);
    }
}
