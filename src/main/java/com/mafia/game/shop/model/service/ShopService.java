package com.mafia.game.shop.model.service;

import java.util.List;

import com.mafia.game.shop.model.vo.Shop;

public interface ShopService {
    int insertArtwork(Shop shop);
    
    List<Shop> selectAllArtworks();
    
    Shop selectArtworkById(int artId);
}
