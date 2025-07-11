package com.mafia.game.shop.model.service;

import java.util.List;

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
    
    @Override
    public List<Shop> selectAllArtworks() {
        return shopDao.selectAllArtworks(sqlSession);
    }
    @Override
    public Shop selectArtworkById(int artId) {
        return shopDao.selectArtworkById(sqlSession, artId);
    }
    
    @Override
    public int updateArt(Shop shop) {
        return shopDao.updateArt(sqlSession, shop); // SqlSession 전달
    }
    
    @Override
    public int deleteArt(int artId) {
        return shopDao.deleteArt(sqlSession, artId);
    }
    

    @Override
    public int purchaseArt(int artId, String buyerName) {
        return shopDao.purchaseArt(sqlSession, artId, buyerName);
    }
    
    @Override
    public List<Shop> selectMyPurchaseList(String buyerName) {
        return shopDao.selectMyPurchaseList(sqlSession, buyerName);
    }

    @Override
    public int purchaseBuyArt(int artId, String buyerName) {
        return shopDao.purchaseArt(sqlSession, artId, buyerName);
    }
  
}
