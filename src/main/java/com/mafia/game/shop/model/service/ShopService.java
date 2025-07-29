package com.mafia.game.shop.model.service;

import java.util.List;

import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.shop.model.vo.Shop;

public interface ShopService {
    int insertArtwork(Shop shop);
    
    List<Shop> selectAllArtworks(PageInfo pi);
    
    Shop selectArtworkById(int artId);
    
    int updateArt(Shop shop);
    
    int deleteArt(int artId);
    
	int purchaseArt(int artId, String userName);

	List<Shop> selectMyPurchaseList(String buyerName);
	
	int purchaseBuyArt(int artId, String buyerName);
	
	int updateProfileImageByUserName(String userName, String profileImgPath);
    
    String getProfileImage(int userName);

    int resetProfileImage(String userName);

	int getListCount();

	List<Shop> selectAllArtworks(int offset, int limit);

	int deleteArtwork(int artId);
}

	

	
