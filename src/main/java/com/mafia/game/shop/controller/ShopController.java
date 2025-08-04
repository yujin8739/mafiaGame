package com.mafia.game.shop.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;
import com.mafia.game.member.model.vo.Member;
import com.mafia.game.shop.model.service.ShopService;
import com.mafia.game.shop.model.vo.Shop;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
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
    public String showArtShopPage(@RequestParam(defaultValue = "1")int currentPage,Model model) {
    	int pageLimit= 10;
    	int boardLimit= 6;
    	int listCount =shopService.getListCount();
    	PageInfo pi =Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit);
        List<Shop> shopList = shopService.selectAllArtworks(pi);
        model.addAttribute("shopList", shopList);
        model.addAttribute("pi", pi);
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
    
    
    
    @GetMapping("/mypage/myitems")
    public String myPurchaseList(
            HttpSession session,
            Model model,
            @RequestParam(value = "selectedImagePath", required = false) String selectedImagePath) {

        Member loginUser = (Member) session.getAttribute("loginUser");

        if (loginUser == null) {
            model.addAttribute("msg", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        String buyerName = loginUser.getUserName();
        List<Shop> myItems = shopService.selectMyPurchaseList(buyerName);
        model.addAttribute("myItems", myItems);

        // ✅ 프로필 이미지 설정 요청이 있는 경우
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            int result = shopService.updateProfileImageByUserName(loginUser.getUserName(), selectedImagePath);  // userName 기준
            if (result > 0) {
                loginUser.setProfileImage(selectedImagePath); // 세션 반영
                session.setAttribute("loginUser", loginUser);
            } else {
                model.addAttribute("msg", "프로필 이미지 업데이트 실패");
                return "common/error";
            }
        }

        return "member/myitems";
    }
    
    @PostMapping("/profile/setImage")
    public String setProfileImage(@RequestParam("imagePath") String imagePath,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs) {

        Member loginUser = (Member) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        int result = shopService.updateProfileImageByUserName(loginUser.getUserName(), imagePath);
        if (result > 0) {
            loginUser.setProfileImage(imagePath);
            session.setAttribute("loginUser", loginUser);
            redirectAttrs.addFlashAttribute("msg", "프로필 이미지가 변경되었습니다.");
        } else {
            redirectAttrs.addFlashAttribute("msg", "프로필 이미지 변경 실패");
        }

        return "redirect:/mypage/myitems";
    }
 // ✅ 테두리 표시 설정을 세션에 저장하는 메서드
    @PostMapping("/profile/setBorder")
    @ResponseBody
    public String setProfileBorder(@RequestParam("borderEnabled") String borderEnabled,
                                   HttpSession session) {

        // "Y" 또는 "N"을 세션에 저장
        session.setAttribute("borderEnabled", borderEnabled);

        return "ok";
    }
    @PostMapping("/mypage/reset-profile")
    @ResponseBody
    public ResponseEntity<?> resetProfileImage(HttpSession session) {
        // 세션에서 로그인된 사용자 정보 추출
        Member loginUser = (Member) session.getAttribute("loginUser");

        // 로그인 상태 확인
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String userName = loginUser.getUserName(); // 또는 getId() 등, DB 컬럼에 맞춰 사용
       // System.out.println(">>> userName: " + userName); // 로그 확인용

        try {
            int result = shopService.resetProfileImage(userName); // DB 업데이트 시도
            //System.out.println(">>> DB update result: " + result); // 디버깅용 로그

            if (result > 0) {
                return ResponseEntity.ok().build(); // 성공 응답
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("업데이트 대상 없음");
            }

        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 예외 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류 발생");
        }
    }

    
    
}
