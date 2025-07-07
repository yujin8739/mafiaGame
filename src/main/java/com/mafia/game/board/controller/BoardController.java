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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mafia.game.board.model.service.BoardService;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("board")
public class BoardController {

	@Autowired
	private BoardService service;
	
	@Value("${file.upload.path}")
	private String filePath;
	

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
								 , RedirectAttributes redirectAttributes) {
		
		BoardFile file = null; //저장될 첨부파일 1개 담을 변수 미리 선언
		
		if(!uploadFile.getOriginalFilename().equals("")) {
			String changeName = getChangedFileName(uploadFile);
			
			file = BoardFile.builder()  
						    .originName(uploadFile.getOriginalFilename())
						    .changeName(changeName)
						    .type("image")
						    .build();
			
		}
		
		//두가지 경우 존재
		//1)첨부 파일 1개 있을시, file != null
		//2)첨부 파일 없을 시 , file == null
		//이 상태로 service로 전달
		int result = service.writeLoungeBoard(board,file);
		

		if (result > 0) { // 게시글 등록 성공
			
			if(file != null) {
				saveFile(uploadFile, file.getChangeName()); //서버 C://godDaddy_uploadImage//에 첨부파일 저장
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
	
	
	public void saveFile(MultipartFile uploadFile,String changeName) {
		try {
			// application.properties에 정의했던 파일경로로 파일 저장
			uploadFile.transferTo(new File(filePath + changeName));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	
}
