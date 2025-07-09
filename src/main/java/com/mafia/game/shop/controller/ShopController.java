package com.mafia.game.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.mafia.game.shop.model.service.ShopService;
import com.mafia.game.shop.model.vo.Shop;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Controller
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

//    @GetMapping("/artshop")
//    public String showArtShopPage() {
//        return "art/artshop";
//    }
    @GetMapping("/artshop")
    public String showArtShopPage(Model model) {
        List<Shop> shopList = shopService.selectAllArtworks();
        model.addAttribute("shopList", shopList);
        return "art/artshop";
    }
    

    @GetMapping("/artshop/uploadForm")
    public String showUploadForm() {
        return "art/artupload";
    }

    @PostMapping("/artshop/upload")
    public String uploadArtwork(@RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam("price") int price,
                                 @RequestParam("sellerName") String sellerName,
                                 @RequestParam("thumbnail") MultipartFile file,
                                 HttpSession session,
                                 Model model) {

    	String uploadPath = "C:\\godDaddy_uploadImage\\artshopImage\\";
        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();

        String originName = file.getOriginalFilename();
        String ext = originName.substring(originName.lastIndexOf("."));
        String changeName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + "_" + (int)(Math.random() * 10000) + ext;

        try {
            file.transferTo(new File(uploadPath + changeName));
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("msg", "파일 업로드 실패!");
            return "common/error";
        }

        Shop shop = new Shop();
        shop.setTitle(title);
        shop.setDescription(description);
        shop.setPrice(price);
        shop.setSellerName(sellerName);
        shop.setImagePath("/godDaddy_uploadImage/artshopImage/" + changeName);
        shop.setStatus("판매중");
        shop.setUploadDate(new Date());

        int result = shopService.insertArtwork(shop);
        if (result > 0) {
            return "redirect:/artshop";
        } else {
            model.addAttribute("msg", "DB 저장 실패");
            return "common/error";
        }
    }
    
    @GetMapping("/artshop/detail/{artId}")
    public String showArtDetail(@PathVariable("artId") int artId, Model model) {
        Shop shop = shopService.selectArtworkById(artId);
        if (shop == null) {
            model.addAttribute("msg", "존재하지 않는 작품입니다.");
            return "common/error";
        }

        model.addAttribute("art", shop);
        return "art/artDetail";
    }
    

    
    
    
    
    
}
