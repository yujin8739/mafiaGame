package com.mafia.game.shop.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.shop.model.vo.Shop;

@Repository
public class ShopDao {

    public int insertArtwork(SqlSession session, Shop shop) {
        return session.insert("artshopMapper.insertArtwork", shop);
    }
    
    
    
    public List<Shop> selectAllArtworks(SqlSession session, PageInfo pi) {
    	
    	//currentPage = 3 이라고 가정
    	int limit = pi.getBoardLimit(); // 6개씩
    	int offset = (pi.getCurrentPage()-1) * limit; //
    	
    	RowBounds rowBounds = new RowBounds(offset, limit);
    	
    	
        return session.selectList("artshopMapper.selectAllArtworks", null , rowBounds);
    }
    
    public Shop selectArtworkById(SqlSession session, int artId) {
        return session.selectOne("artshopMapper.selectArtworkById", artId);
    }
    public int updateArt(SqlSession session, Shop shop) {
        return session.update("artshopMapper.updateArt", shop);
    }
    
    public int deleteArt(SqlSession session, int artId) {
        return session.delete("artshopMapper.deleteArt", artId);
    }


    public int purchaseArt(SqlSession sqlSession, int artId, String buyerName) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("artId", artId);
        paramMap.put("buyerName", buyerName);
        return sqlSession.update("artshopMapper.purchaseArt", paramMap);
    }

    public List<Shop> selectMyPurchaseList(SqlSession sqlSession, String buyerName) {
        return sqlSession.selectList("artshopMapper.selectMyPurchaseList", buyerName);
    }

    public int purchaseBuyArt(SqlSession sqlSession, int artId, String buyerName) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("artId", artId);
        paramMap.put("buyerName", buyerName);
        return sqlSession.update("artshopMapper.purchaseArt", paramMap);
    }


    public int updateProfileImageByUserName(SqlSession sqlSession, String userName, String profileImgPath) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userName", userName);
        paramMap.put("profileImgPath", profileImgPath);

        return sqlSession.update("artshopMapper.updateProfileImageByUserName", paramMap);
    }

    public String getProfileImage(SqlSession sqlSession, int userName) {
        return sqlSession.selectOne("artshopMapper.getProfileImage", userName);
    }

    public int resetProfileImage(SqlSession sqlSession, String userName) {
        return sqlSession.update("artshopMapper.resetProfileImage", userName);
    }



	public int getListCount(SqlSession sqlSession) {
		// TODO Auto-generated method stub
		return sqlSession.selectOne("artshopMapper.getListCount");
	}



	public List<Shop> selectAllArtworks(SqlSession sqlSession, int offset, int limit) {
		
		RowBounds rb = new RowBounds(offset, limit);
		return sqlSession.selectList("artshopMapper.selectAllArtworks", null, rb);
	}
	
	
    
    
    
    
  


    
}
