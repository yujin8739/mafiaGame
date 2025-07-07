package com.mafia.game.shop.model.service;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.shop.model.dao.ShopDao;
import com.mafia.game.shop.model.vo.Shop;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private SqlSession sqlSession;

    @Autowired
    private ShopDao shopDao;

    @Transactional
    @Override
    public int insertArtwork(Shop shop) {
        return shopDao.insertArtwork(sqlSession, shop);
    }
}
