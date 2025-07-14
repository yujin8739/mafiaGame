package com.mafia.game.shop.model.service;

import java.util.List;

import com.mafia.game.shop.model.vo.Shop;

public interface ShopService {
    int insertArtwork(Shop shop);
    
    List<Shop> selectAllArtworks();
    
    Shop selectArtworkById(int artId);
    
    int updateArt(Shop shop);
    
    int deleteArt(int artId);
    
	int purchaseArt(int artId, String userName);

	List<Shop> selectMyPurchaseList(String buyerName);
	
	int purchaseBuyArt(int artId, String buyerName);
	
	int updateProfileImageByUserName(String userName, String profileImgPath);
    
    String getProfileImage(int userName);

	



	
}
