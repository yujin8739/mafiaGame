package com.mafia.game.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ShopController {

    @GetMapping("/artshop")
    public String showArtShopPage() {
        return "art/artshop"; // templates/art/artshop.html 렌더링
    }
}
