package com.mafia.game.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.mafia.game.member.model.vo.Member;
import com.mafia.game.shop.model.service.ShopService;
import com.mafia.game.shop.model.vo.Shop;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
    
    @PostMapping("/artshop/edit/{id}")
    public String updateArt(
        @PathVariable("id") int artId,
        @ModelAttribute Shop updatedArt,
        @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
        HttpSession session,
        Model model
    ) {
        String uploadPath = "C:\\godDaddy_uploadImage\\artshopImage\\";
        File folder = new File(uploadPath);
        if (!folder.exists()) folder.mkdirs();

        // 기존 이미지 경로 확보
        Shop originArt = shopService.selectArtworkById(artId);
        String originImagePath = originArt.getImagePath();  // ex: /godDaddy_uploadImage/artshopImage/xxx.jpg

        if (thumbnail != null && !thumbnail.isEmpty()) {
            String originalName = thumbnail.getOriginalFilename();
            String ext = originalName.substring(originalName.lastIndexOf("."));
            String renamed = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                            + "_" + (int)(Math.random() * 10000) + ext;

            try {
                // 새 이미지 저장
                File newFile = new File(uploadPath + renamed);
                thumbnail.transferTo(newFile);
                updatedArt.setImagePath("/godDaddy_uploadImage/artshopImage/" + renamed);

                // ✅ 기존 이미지 삭제 시도
                if (originImagePath != null) {
                    File oldFile = new File("C:" + originImagePath); // 실제 경로로 변환
                    if (oldFile.exists()) {
                        boolean deleted = oldFile.delete();
                        if (!deleted) {
                            System.out.println("[경고] 기존 이미지 삭제 실패: " + oldFile.getAbsolutePath());
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("msg", "파일 업로드 실패!");
                return "common/error";
            }
        } else {
            // 새 이미지 없으면 기존 이미지 유지
            updatedArt.setImagePath(originImagePath);
        }

        updatedArt.setArtId(artId); // ID 설정
        int result = shopService.updateArt(updatedArt);

        if (result > 0) {
            return "redirect:/artshop/detail/" + artId;
        } else {
            model.addAttribute("msg", "수정 실패");
            return "common/error";
        }
    }
    
    @GetMapping("/artshop/editForm/{id}")
    public String showEditForm(@PathVariable("id") int artId, Model model) {
        Shop art = shopService.selectArtworkById(artId);
        if (art == null) {
            model.addAttribute("msg", "존재하지 않는 작품입니다.");
            return "common/error";
        }
        model.addAttribute("art", art);
        return "art/artupdate";  // 수정 폼 뷰
    }
    
    @GetMapping("/artshop/delete/{id}")
    public String deleteArt(@PathVariable("id") int artId, Model model) {
        Shop art = shopService.selectArtworkById(artId);
        if (art == null) {
            model.addAttribute("msg", "삭제할 작품이 존재하지 않습니다.");
            return "common/error";
        }

        // 이미지 파일 삭제
        String imagePath = art.getImagePath();
        if (imagePath != null) {
            File file = new File("C:" + imagePath);
            if (file.exists()) file.delete();
        }

        int result = shopService.deleteArt(artId);
        return (result > 0) ? "redirect:/artshop" : "common/error";
    }
    
    @PostMapping("/artshop/buy/{id}")
    public String buyArt(@PathVariable("id") int artId, HttpSession session, Model model) {
        Member loginUser = (Member) session.getAttribute("loginUser");
        
        if (loginUser == null) {
            return "redirect:/login/view";
        }

        int result = shopService.purchaseArt(artId, loginUser.getUserName());

        if (result > 0) {
            model.addAttribute("msg", "구매가 완료되었습니다.");
            model.addAttribute("loc", "/artshop");
            return "art/success"; // 구매 완료 메시지 띄우고 이동
        } else {
            model.addAttribute("msg", "구매에 실패했습니다.");
            return "common/error";
        }
    }
    
    
    
    // 내가 구매한 일러스트 목록 조회
    @GetMapping("/mypage/myitems")
    public String myPurchaseList(HttpSession session, Model model) {
        String buyerName = (String) session.getAttribute("loginUser");

        if (buyerName == null) {
            model.addAttribute("msg", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        List<Shop> myItems = shopService.selectMyPurchaseList(buyerName);
        model.addAttribute("myItems", myItems);

        return "member/myitems"; // myitems.html 뷰로 이동
    }
}
