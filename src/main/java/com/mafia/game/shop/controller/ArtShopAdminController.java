package com.mafia.game.shop.controller;

import com.mafia.game.shop.model.service.ShopService;
import com.mafia.game.shop.model.vo.Shop; // Shop 모델을 가져옵니다.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artList") // 관리자 관련 API 경로로 변경
public class ArtShopAdminController {

	public ArtShopAdminController() {
		
	}



	/*
	 * // 아트샵 게시글 목록 조회
	 * 
	 * @GetMapping("/artlist") public ResponseEntity<List<Shop>> getArtList() {
	 * List<Shop> artList = shopService.getAllArt(); // 서비스 레이어를 통해 데이터 가져오기 return
	 * ResponseEntity.ok(artList); }
	 * 
	 * // 아트샵 게시글 삭제
	 * 
	 * @PostMapping("/artDelete") public ResponseEntity<String>
	 * deleteArt(@RequestBody int artId) { boolean deleted =
	 * shopService.deleteArtById(artId); // 서비스 레이어에서 삭제 if (deleted) { return
	 * ResponseEntity.ok("삭제 완료"); } else { return
	 * ResponseEntity.status(400).body("삭제 실패"); } }
	 */
}
