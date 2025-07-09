package com.mafia.game.board.controller;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mafia.game.board.model.service.BoardService;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.Reply;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("board")
@Slf4j
public class BoardController {

	@Autowired
	private BoardService service;
	
	@Value("${file.uploadLoungImage.path}")
	private String loungeImagePath;
	
	@Value("${file.uploadReplyImage.path}")
	private String replyImagePath;
	
	@Value("${file.uploadGalleryImage.path}")
	private String galleryImagePath;
	
	@Value("${file.deletedReplyImage.path}")
	private String deletedReplyImagePath;
	
	@Value("${file.deletedLoungeImage.path}")
	private String deletedLoungeImagePath;
	
	/*=======================================라운지============================================*/
	
	@GetMapping("lounge") // 라운지 - 일반 게시판
	public String loungeBoardList(@RequestParam(defaultValue = "1") int currentPage, String type, String condition,
			String keyword, Model model, HttpSession session) {


		HashMap<String, String> filterMap = new HashMap<>(); // 필터링을 위한 상태값,조건값,키워드 맵
		filterMap.put("type", type);
		filterMap.put("condition", condition);
		filterMap.put("keyword", keyword);

		int listCount = service.listCount(filterMap); // 가져올 게시글 개수

		System.out.println("게시글 개수 :" + listCount);
		int pageLimit = 10;
		int boardLimit = 20;

		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit); // 페이징 처리를 위한 정보

		ArrayList<Board> boardList = service.boardList(filterMap, pi); // 가져올 게시글 목록
		
		for(Board b : boardList) {
			int rankPoint = b.getRankPoint();
			if(0 <= rankPoint && rankPoint < 500) {
				b.setProfileUrl("/godDaddy_uploadImage/rankImage/iron.png");
			}else if(500 <= rankPoint && rankPoint < 1200) {
				b.setProfileUrl("/godDaddy_uploadImage/rankImage/thug.png");
			}else if(1200 <= rankPoint && rankPoint < 2000) {
				b.setProfileUrl("/godDaddy_uploadImage/rankImage/agent.png");
			}else if(2000 <= rankPoint && rankPoint < 3000) {
				b.setProfileUrl("/godDaddy_uploadImage/rankImage/underBoss.png");
			}else if(3000 <= rankPoint) {
				b.setProfileUrl("/godDaddy_uploadImage/rankImage/boss.png");
			}
		}

		model.addAttribute("boardList", boardList);
		model.addAttribute("pi", pi);
		model.addAttribute("filterMap", filterMap);

		return "board/lounge";

	}

	@GetMapping("/lounge/write") // 라운지 게시글 작성 폼으로 이동
	public String loungeWriteForm() {

		return "board/loungeWriteForm";
	}

	@PostMapping("/lounge/write") // 라운지에 게시글 작성하기
	public String writeLoungeBoard(Board board
								 , MultipartFile uploadFile
								 , HttpSession session
								 , RedirectAttributes redirectAttributes
								 , @RequestParam(defaultValue="0") int jobTypeNo) {
		
		BoardFile file = null; //저장될 첨부파일 1개 담을 변수 미리 선언
		
		if(!uploadFile.getOriginalFilename().equals("")) {
			String changeName = getChangedFileName(uploadFile);
			
			file = BoardFile.builder()  
						    .originName(uploadFile.getOriginalFilename())
						    .changeName(changeName)
						    .type("image")
						    .build();
			
		}
		
		if(board.getTypeNo() == 0) {
			board.setTypeNo(jobTypeNo);
		}
		//두가지 경우 존재
		//1)첨부 파일 1개 있을시, file != null
		//2)첨부 파일 없을 시 , file == null
		//이 상태로 service로 전달
		int result = service.writeLoungeBoard(board,file);
		

		if (result > 0) { // 게시글 등록 성공
			
			if(file != null) {
				saveLoungeImage(uploadFile, file.getChangeName()); //서버 C://godDaddy_uploadImage//에 첨부파일 저장
			}
			redirectAttributes.addFlashAttribute("msg", "게시글이 정상적으로 등록되었습니다");
			

		} else { // 게시글 등록 실패

			redirectAttributes.addFlashAttribute("msg", "게시글 등록에 실패하였습니다.");

		}
		
		return "redirect:/board/lounge";
	}

	@GetMapping("/lounge/detail/{boardNo}") // 라운지 게시글 상세보기
	public String loungeDetail(@PathVariable int boardNo, Model model,RedirectAttributes redirectAttributes) {
		
		int result = service.increaseCount(boardNo); //게시글 들어가면 조회수 1증가시키기
		
		if(result > 0) { //조회수 증가 성공!
			Board board = service.loungeBoardDetail(boardNo); // 1개의 라운지 게시글 정보 얻어오기
			
			model.addAttribute("board", board);
		}else { //조회수 증가 실패ㅠㅠ
			redirectAttributes.addFlashAttribute("msg","게시글이 정상적으로 조회되지 않았습니다.");
			return "redirect:/board/lounge";
		}


		return "board/loungeDetail";
	}
	
	@PostMapping("/lounge/delete/{boardNo}")
	public String deleteLoungeBoard(@PathVariable int boardNo,RedirectAttributes redirectAttributes) {
		
		Board board = service.loungeBoardDetail(boardNo);
		
		int result = service.deleteLoungeBoard(board);//삭제할 게시글 정보 보내기
		
		if(result > 0) { //BOARD 테이블에서 삭제 완료
			
			if(!board.getFileList().isEmpty()) { //게시글에 첨부파일 있었을 경우
				String changeName = board.getFileList().get(0).getChangeName();
				if(deleteFile(loungeImagePath, deletedLoungeImagePath, changeName)) {
					log.debug("게시글 파일 삭제 완료");
				}else {
					log.debug("게시글 파일 삭제 실패");
				}
			}
			
			ArrayList<Reply> replyList = board.getReplyList();
			for(Reply reply : replyList) {
				reply = service.selectReply(reply.getReplyNo());
				if(reply.getChangeName() != null) {
					if(deleteFile(replyImagePath,deletedReplyImagePath,reply.getChangeName())) {
						log.debug("댓글 파일 삭제 완료");
					}else {
						log.debug("댓글 파일 삭제 실패");
					}
				}
			}
			
			redirectAttributes.addFlashAttribute("msg","게시글이 정상적으로 삭제되었습니다.");
			
			return "redirect:/board/lounge";
		}else {
			redirectAttributes.addFlashAttribute("msg","게시글 삭제에 실패하였습니다.");
			return "redirect:/board/lounge/detail/" + boardNo;
		}
		
	}
	
	@GetMapping("/lounge/update/{boardNo}")
	public String loungeUpdateForm(@PathVariable int boardNo, Model model) {
		
		Board board = service.loungeBoardDetail(boardNo);
		
		model.addAttribute("board", board);
		
		return "board/loungeUpdateForm";
	}
	
	@PostMapping("/lounge/update")
	public String updateLoungeBoard(Board board, MultipartFile uploadFile, boolean deleteFile) {
		
		BoardFile file = null;
		
		if(!uploadFile.getOriginalFilename().equals("")) { //수정한 파일이 존재한다면
			
			String changeName = getChangedFileName(uploadFile);
			file = BoardFile.builder()
		         			.originName(uploadFile.getOriginalFilename())
		         			.changeName(changeName)
		         			.type("image")
		         			.boardNo(board.getBoardNo())
		         			.build();
			
		}
		
		int result = service.updateLoungeBoard(board,file,deleteFile);
		
		if(result > 0) { //게시글 및 파일 변경 완료
			
			if(!board.getChangeName().equals("")) { //기존 파일 있었을 경우
				if(file != null || deleteFile == true) { //파일 변경했을 경우 또는 파일 삭제 버튼 눌렀을 경우
					if(deleteFile(loungeImagePath, deletedLoungeImagePath, board.getChangeName())) {
						log.debug("기존 파일 삭제 완료");
					}else {
						log.debug("기존 파일 삭제 실패");
					}
				}
			}
			
			if(file != null) {
				saveLoungeImage(uploadFile, file.getChangeName()); //변경된 파일 있다변 서버에 저장
			}
			
			return "redirect:/board/lounge/detail/" + board.getBoardNo();
			
		}else {//게시글 및 파일 변경 실패
			
			return "redirect:/board/lounge/update/" + board.getBoardNo();
			
		}
		
		
		
	}
	
	@PostMapping("/lounge/uploadReply") // 댓글 DB 저장 + 이미지 저장 처리
	@ResponseBody                   
	public int uploadReply(Reply reply
	                      ,MultipartFile image
	                      ,HttpSession session) {
		
		System.out.println("댓글 : " + reply);
		System.out.println("이미지 : " + image);
		BoardFile file = null;
		
		if(image != null) {
			String changeName = getChangedFileName(image);
			file = BoardFile.builder()  
						    .originName(image.getOriginalFilename())
						    .changeName(changeName)
						    .type("image")
						    .build();
		}
		
		int result = service.uploadReply(reply,file);
		
		if(result > 0) {
			if(image != null) { //업로드한 파일 있을 경우 서버에 등록
				saveReplyImage(image, file.getChangeName());
			}
		}
		
		return result;
	}
	
	@GetMapping("/lounge/getReplyList")
	@ResponseBody
	public ArrayList<Reply> getReplyList(int boardNo){
		
		ArrayList<Reply> replyList = service.getReplyList(boardNo);
		
		return replyList;
	}
	
	@PostMapping("/lounge/deleteReply")
	@ResponseBody
	public int deleteReply(int replyNo) {
		
		Reply reply = service.selectReply(replyNo);
		String changeName = reply.getChangeName();
		
		int result = service.deleteReply(reply); // 댓글 삭제
		if(result > 0) { //정상적으로 REPLY 및 BOARD_FILE 테이블에서 삭제 완료
			if(changeName != null) { //파일 있을 경우 -> 삭제 폴더로 이동시키기
				
				
				if(deleteFile(replyImagePath,deletedReplyImagePath,changeName)) {
					log.debug("파일 삭제 완료");
				}else {
					log.debug("파일 삭제 실패");
				}
			}
		}
		
		
		
		return result;
	}
	
	
	/*=======================================갤러리============================================*/
	
	@GetMapping("/gallery")
	public String galleryBoardList() {
		return "board/gallery";
	}
	
	
	
	
	/*=======================================하이라이트 영상============================================*/
	@GetMapping("/video")
	public String videoBoardList() {
		return "board/video";
	}
	

	/*=======================================파일 저장 및 변경 이름 반환 메소드============================================*/
	
	// 변경된 파일 이름 반환 메소드
	public String getChangedFileName(MultipartFile uploadFile) {
		// 1.원본 파일명 추출
		String originName = uploadFile.getOriginalFilename();

		// 2.시간 형식 문자열로 추출
		String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

		// 3.랜덤값 5자리 추출
		int ranNum = (int) (Math.random() * 90000 + 10000);

		// 4.원본파일에서 확장자 추출
		String ext = originName.substring(originName.lastIndexOf("."));

		// 5. 합치기
		String changeName = currentTime + ranNum + ext;

		// 6. 서버에 업로드 처리할때 물리적인 경로 추출하기
		// String savePath =
		// "C:\\07_Springboot-workspace\\springbootThymeleaf\\src\\main\\resources\\static\\uploadFiles";

		// 7.경로와 변경된 이름을 이용하여 파일 업로드 처리 메소드 수행
		// MultipartFile 의 transferTo() 메소드 이용
		

		return changeName; // 서버에 업로드된 파일명 반환
	}
	
	
	public void saveLoungeImage(MultipartFile uploadFile,String changeName) {
		try {
			// application.properties에 정의했던 파일경로로 파일 저장
			uploadFile.transferTo(new File(loungeImagePath + changeName));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	public void saveReplyImage(MultipartFile uploadFile,String changeName) {
		try {
			// application.properties에 정의했던 파일경로로 파일 저장
			uploadFile.transferTo(new File(replyImagePath + changeName));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	public void saveGalleryImage(MultipartFile uploadFile,String changeName) {
		try {
			// application.properties에 정의했던 파일경로로 파일 저장
			uploadFile.transferTo(new File(galleryImagePath + changeName));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	//파일 삭제(deletedImage 폴더로 이동시키는 메소드)
	public boolean deleteFile(String originPath,String deletePath,String changeName) {
		
		
		File originFile = new File(originPath + changeName);
		File deletedFile = new File(deletePath + changeName);
		return originFile.renameTo(deletedFile); 
		
	}
	
	
	
	
	
}
