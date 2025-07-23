package com.mafia.game.board.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mafia.game.board.model.service.BoardService;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.BoardLikeDTO;
import com.mafia.game.board.model.vo.Reply;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.BadgeSetUtil;
import com.mafia.game.common.template.Pagination;
import com.mafia.game.common.template.VideoUploadUtil;
import com.mafia.game.member.model.vo.Member;

import jakarta.servlet.http.HttpServletRequest;
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
	
	@Value("${file.uploadVideo.path}")
	private String mp4Path;
	
	@Value("${file.deletedVideo.path}")
	private String deletedVideoPath;
	
	@Value("${file.deletedGalleryImage.path}")
	private String deletedGalleryPath;
	
	/*=======================================라운지============================================*/
	
	@GetMapping("lounge") // 라운지 - 일반 게시판
	public String loungeBoardList(@RequestParam(defaultValue = "1") int currentPage, String typeName, String condition,
			String keyword, Model model) {
		

		HashMap<String, String> filterMap = new HashMap<>(); // 필터링을 위한 상태값,조건값,키워드 맵
		filterMap.put("typeName", typeName);
		filterMap.put("condition", condition);
		filterMap.put("keyword", keyword);
		filterMap.put("typeClass", null);
		if(typeName != null && !"".equals(typeName) && !"자유".equals(typeName) && !"플레이".equals(typeName)) {
			filterMap.put("typeClass", "3");
		}

		int listCount = service.listCount(filterMap); // 가져올 게시글 개수

		int pageLimit = 10;
		int boardLimit = 20;

		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit); // 페이징 처리를 위한 정보

		ArrayList<Board> boardList = service.boardList(filterMap, pi); // 가져올 게시글 목록
		ArrayList<Board> topLikedList = service.topLikedList(filterMap);//가져올 top5 게시글 목록
		
		
		boardList = enrichBoardInfo(boardList);
		topLikedList = enrichBoardInfo(topLikedList);
		
		
		model.addAttribute("topLikedList", topLikedList);
		model.addAttribute("boardList", boardList);
		model.addAttribute("pi", pi);
		model.addAttribute("filterMap", filterMap);

		return "board/lounge";

	}
	
	//게시글리스트 정보 가공을 위한 메소드
	public ArrayList<Board> enrichBoardInfo(ArrayList<Board> boardList) {
		for(Board b : boardList) {
			
			b.setNew(b.getCreateDate().toLocalDate().isEqual(LocalDate.now()));
			
			BadgeSetUtil.setBadgeUrl(b);
			
		}
		
		return boardList;
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
								 , String jobTypeName) {
		
		BoardFile file = null; //저장될 첨부파일 1개 담을 변수 미리 선언
		
		if(!uploadFile.getOriginalFilename().equals("")) {
			String changeName = getChangedFileName(uploadFile);
			
			file = BoardFile.builder()  
						    .originName(uploadFile.getOriginalFilename())
						    .changeName(changeName)
						    .type("image")
						    .fileLevel(1)
						    .build();
			
		}
		
		String typeName = board.getTypeName();
		if(typeName.equals("직업")) {
			board.setTypeName(jobTypeName);
			board.setTypeClass(3);
		}else if(typeName.equals("자유")) {
			board.setTypeClass(1);
		}else if(typeName.equals("플레이")){
			board.setTypeClass(2);
		}
		//두가지 경우 존재
		//1)첨부 파일 1개 있을시, file != null
		//2)첨부 파일 없을 시 , file == null
		//이 상태로 service로 전달
		int result = service.writeLoungeBoard(board,file);
		

		if (result > 0) { // 게시글 등록 성공
			
			if(file != null) {
				saveImage(loungeImagePath, uploadFile, file.getChangeName()); //서버 C://godDaddy_uploadImage//에 첨부파일 저장
			}
			redirectAttributes.addFlashAttribute("msg", "게시글이 정상적으로 등록되었습니다.");
			

		} else { // 게시글 등록 실패

			redirectAttributes.addFlashAttribute("msg", "게시글 등록에 실패하였습니다.");

		}
		
		return "redirect:/board/lounge";
	}

	@GetMapping("/lounge/detail/{boardNo}") // 라운지 게시글 상세보기
	public String loungeDetail(@SessionAttribute Member loginUser,
			@PathVariable int boardNo, Model model,RedirectAttributes redirectAttributes) {
		
		int result = service.increaseCount(boardNo); //게시글 들어가면 조회수 1증가시키기
		
		if(result > 0) { //조회수 증가 성공!
			Board board = service.loungeBoardDetail(boardNo); // 1개의 라운지 게시글 정보 얻어오기
			board = BadgeSetUtil.setBadgeUrl(board);
			
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
		
		int result = service.deleteBoard(board);//삭제할 게시글 정보 보내기
		
		if(result > 0) { //BOARD 테이블에서 삭제 완료
			String statusOfFile = board.getFileList().get(0).getStatus();
			
			if("Y".equals(statusOfFile)) { // 게시글 첨부파일이 존재한다면
				String changeName = board.getFileList().get(0).getChangeName();
				if(deleteFile(loungeImagePath, deletedLoungeImagePath, changeName)) {
					log.debug("게시글 파일 삭제 완료");
				}else {
					redirectAttributes.addFlashAttribute("msg","게시글 첨부파일 삭제 중 오류가 발생하였습니다.");
					return "redirect:/board/lounge/detail/" + boardNo;
				}
			}
		
			
			ArrayList<Reply> replyList = board.getReplyList();
			for(Reply reply : replyList) {
				if(reply.getFileNo() != 0) {
					if(deleteFile(replyImagePath,deletedReplyImagePath,reply.getChangeName())) {
						log.debug("댓글 파일 삭제 완료");
					}else {
						redirectAttributes.addFlashAttribute("msg","댓글 첨부파일 삭제 중 오류가 발생하였습니다.");
						return "redirect:/board/lounge/detail/" + boardNo;
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
	public String updateLoungeBoard(Board board, String jobTypeName, MultipartFile uploadFile, boolean deleteFile, RedirectAttributes redirectAttributes) {
		
		BoardFile file = null;
		
		if(!uploadFile.getOriginalFilename().equals("")) { //수정한 파일이 존재한다면
			
			String changeName = getChangedFileName(uploadFile);
			file = BoardFile.builder()
		         			.originName(uploadFile.getOriginalFilename())
		         			.changeName(changeName)
		         			.type("image")
		         			.boardNo(board.getBoardNo())
		         			.fileLevel(1)
		         			.build();
			
		}
		String typeName = board.getTypeName();
		if(typeName.equals("직업")) {
			board.setTypeName(jobTypeName);
			board.setTypeClass(3);
		}else if(typeName.equals("자유")) {
			board.setTypeClass(1);
		}else if(typeName.equals("플레이")){
			board.setTypeClass(2);
		}
		int result = service.updateLoungeBoard(board,file,deleteFile);
		
		if(result > 0) { //게시글 및 파일 변경 완료
			
			if(!board.getChangeName().equals("")) { //기존 파일 있었을 경우
				if(file != null || deleteFile == true) { //파일 변경했을 경우 또는 파일 삭제 버튼 눌렀을 경우
					if(!deleteFile(loungeImagePath, deletedLoungeImagePath, board.getChangeName())) {
						redirectAttributes.addFlashAttribute("msg","첨부파일 수정 중 오류가 발생하였습니다");
						return "redirect:/board/lounge";
					}
						
					
				}
			}
			
			if(file != null) {
				saveImage(loungeImagePath, uploadFile, file.getChangeName()); //변경된 파일 있다면 서버에 저장
				
				if(!new File(loungeImagePath + file.getChangeName()).exists()) {
					redirectAttributes.addFlashAttribute("msg","첨부파일 등록 중 오류가 발생하였습니다");
					return "redirect:/board/lounge";
				}
			}
			redirectAttributes.addFlashAttribute("msg","게시글이 정상적으로 수정되었습니다");
			
		}else {//게시글 및 파일 변경 실패
			
			redirectAttributes.addFlashAttribute("msg","게시글 수정 중 오류가 발생하였습니다");
			
		}
		return "redirect:/board/lounge";
		
		
		
	}
	
	@PostMapping("/uploadReply") // 댓글 DB 저장 + 이미지 저장 처리
	@ResponseBody                   
	public int uploadReply(Reply reply
	                      ,MultipartFile image
	                      ,HttpSession session) {
		
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
				saveImage(replyImagePath, image, file.getChangeName());
			}
		}
		
		return result;
	}
	
	@GetMapping("/getReplyList")
	@ResponseBody
	public ArrayList<Reply> getReplyList(int boardNo){
		
		ArrayList<Reply> replyList = service.getReplyList(boardNo);
		
		for(Reply r : replyList) {
			BadgeSetUtil.setBadgeUrl(r);
			if(r.getReplyContent() == null) {
				r.setReplyContent("");
			}
		}
		
		return replyList;
	}
	
	@PostMapping("/deleteReply")
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
	public String galleryBoardList(@RequestParam(defaultValue = "1") int currentPage,
								   @RequestParam(defaultValue = "갤러리") String typeName,
								   String condition,
								   String keyword,
								   Model model) {
		
		HashMap<String, String> filterMap = new HashMap<>(); // 필터링을 위한 상태값,조건값,키워드 맵
		filterMap.put("typeName", typeName);
		filterMap.put("condition", condition);
		filterMap.put("keyword", keyword);

		int listCount = service.listCount(filterMap); // 가져올 게시글 개수

		int pageLimit = 10;
		int boardLimit = 12;

		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit); // 페이징 처리를 위한 정보

		ArrayList<Board> boardList = service.boardList(filterMap, pi); // 가져올 게시글 목록
		ArrayList<Board> topBoardList = service.topLikedList(filterMap);//가져올 top5 게시글 목록
		
		
		boardList = enrichBoardInfo(boardList);
		topBoardList = enrichBoardInfo(topBoardList);
		
		
		model.addAttribute("topBoardList", topBoardList);
		model.addAttribute("boardList", boardList);
		model.addAttribute("pi", pi);
		model.addAttribute("filterMap", filterMap);
		return "board/gallery";
	}
	
	@GetMapping("/gallery/upload")
	public String glleryUploadForm() {
		return "board/galleryUploadForm";
	}
	
	@PostMapping("/gallery/upload")
	public String uploadGallery(Board board, MultipartFile[] uploadFiles, RedirectAttributes redirectAttributes) {
		
		try {
			
			if(uploadFiles.length > 5) {
				throw new IllegalArgumentException();
			}
			ArrayList<BoardFile> boardFiles = new ArrayList<>();
			int fl = 1;
			for(MultipartFile f : uploadFiles) {
				String changeName = getChangedFileName(f);
				BoardFile bf = BoardFile.builder()
									    .originName(f.getOriginalFilename())
									    .changeName(changeName)
									    .type("image")
									    .fileLevel(fl++)
									    .build();
			boardFiles.add(bf);
		}
		
			int result = service.uploadGalleryBoard(board, boardFiles);
			
			if(result > 0) {
				
				int i = 0;
				for(BoardFile bf : boardFiles) {
					saveImage(galleryImagePath, uploadFiles[i], bf.getChangeName());
					i++;
				}
				redirectAttributes.addFlashAttribute("msg", "게시글이 정상적으로 등록되었습니다");
			}else {
				redirectAttributes.addFlashAttribute("msg", "게시글 등록에 실패하였습니다.");
			}
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("msg", "사진은 최대 5개까지만 업로드할 수 있습니다." );
			return "redirect:/board/gallery/upload/";
		}catch(Exception e){
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("msg", "서버 오류로 게시글 등록에 실패하였습니다");
		}
		
		return "redirect:/board/gallery";
		
	}
	
	@GetMapping("/gallery/detail/{boardNo}")
	public String galleryDetail(@PathVariable int boardNo, Model model, RedirectAttributes redirectAttributes) {
		
		
		int result = service.increaseCount(boardNo);
		
		if(result > 0) {
			Board board = service.loungeBoardDetail(boardNo);
			board = BadgeSetUtil.setBadgeUrl(board);
			model.addAttribute("board", board);
			
			return "board/galleryDetail";
		}else {
			redirectAttributes.addFlashAttribute("msg", "게시글 조회에 실패하였습니다");
			return "board/gallery";
		}
		
	}
	
	@GetMapping("/gallery/update/{boardNo}")
	public String galleryUpdateForm(@PathVariable int boardNo, Model model) {
		
		
		Board board = service.loungeBoardDetail(boardNo);
		
		model.addAttribute("board", board);
		
		return "board/galleryUpdateForm";
	}
	
	@PostMapping("/gallery/update")
	public String updateGallery(Board board,int[] remainFileList, MultipartFile[] newFiles, String deletedFileList
							   ,RedirectAttributes redirectAttributes) {
		
		
		
		try {
			
			if(remainFileList.length + newFiles.length > 5) {
				throw new IllegalArgumentException();
			}
			//새로운 이미지들 저장하기 위한 로직
			ArrayList<BoardFile> boardFiles = new ArrayList<>();
			for(MultipartFile f : newFiles) {
				String changeName = getChangedFileName(f);
				BoardFile bf = BoardFile.builder()
									    .originName(f.getOriginalFilename())
									    .changeName(changeName)
									    .type("image")
									    .build();
				boardFiles.add(bf);
			}
		
		
		
			int result = service.updateGalleryBoard(board, remainFileList, boardFiles, deletedFileList);
			
			if(result > 0) {
				int i = 0;
				for(BoardFile bf : boardFiles) {
					saveImage(galleryImagePath, newFiles[i], bf.getChangeName());
					i++;
				}
				if (deletedFileList != null && !deletedFileList.isEmpty()) {
					ArrayList<String> deletedFileNames = service.selectFileNames(deletedFileList);
					for(String deletedFileName : deletedFileNames) {
						if(!deleteFile(galleryImagePath, deletedGalleryPath, deletedFileName)) {
							redirectAttributes.addFlashAttribute("msg", "파일 수정 중 오류가 발생하였습니다.");
							return "redirect:/board/gallery";
						};
					}
				}
				redirectAttributes.addFlashAttribute("msg", "게시글이 정상적으로 수정되었습니다.");
				
			}else {
				redirectAttributes.addFlashAttribute("msg", "게시글 수정에 실패하였습니다.");
			}
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("msg", "사진은 최대 5개까지만 업로드할 수 있습니다." );
			
			return "redirect:/board/gallery/update/" + board.getBoardNo();
		}catch(Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("msg", "서버 오류로 게시글 수정에 실패하였습니다.");
		}
		
		
		
		return "redirect:/board/gallery";
		
	}
	
	@PostMapping("/gallery/delete/{boardNo}")
	public String deleteGallery(@PathVariable int boardNo, RedirectAttributes redirectAttributes) {
		Board board = service.loungeBoardDetail(boardNo);
		
		int result = service.deleteBoard(board);//삭제할 게시글 정보 보내기
		
		if(result > 0) { //BOARD 테이블에서 삭제 완료
			
			for(BoardFile file : board.getFileList()) {
				if(!deleteFile(galleryImagePath, deletedGalleryPath, file.getChangeName())) {
					redirectAttributes.addFlashAttribute("msg", "파일 삭제 중 오류가 발생하였습니다.");
					return "redirect:/board/gallery";
				};
			}
		
			
			ArrayList<Reply> replyList = board.getReplyList();
			for(Reply reply : replyList) {
				if(reply.getFileNo() != 0) {
					if(deleteFile(replyImagePath,deletedReplyImagePath,reply.getChangeName())) {
						log.debug("댓글 파일 삭제 완료");
					}else {
						redirectAttributes.addFlashAttribute("msg","댓글 첨부파일 삭제 중 오류가 발생하였습니다.");
						return "redirect:/board/gallery";
					}
				}
			}
			
			redirectAttributes.addFlashAttribute("msg","게시글이 정상적으로 삭제되었습니다.");
			
			return "redirect:/board/gallery";
		}else {
			redirectAttributes.addFlashAttribute("msg","게시글 삭제에 실패하였습니다.");
			return "redirect:/board/gallery";
		}
	}
	
	
	
	
	/*=======================================하이라이트 영상============================================*/
	
	@GetMapping("/video")
	public String videoBoardList(@RequestParam(defaultValue = "1") int currentPage,
								 @RequestParam(defaultValue = "영상") String typeName,
								 String condition,
								 String keyword,
								 Model model) {
		HashMap<String, String> filterMap = new HashMap<>(); // 필터링을 위한 상태값,조건값,키워드 맵
		filterMap.put("typeName", typeName);
		filterMap.put("condition", condition);
		filterMap.put("keyword", keyword);

		int listCount = service.listCount(filterMap); // 가져올 게시글 개수

		int pageLimit = 10;
		int boardLimit = 12;

		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit); // 페이징 처리를 위한 정보

		ArrayList<Board> videoList = service.boardList(filterMap, pi); // 가져올 게시글 목록
		ArrayList<Board> topVideoList = service.topLikedList(filterMap);//가져올 top5 게시글 목록
		
		
		videoList = enrichBoardInfo(videoList);
		topVideoList = enrichBoardInfo(topVideoList);
		
		
		model.addAttribute("topVideoList", topVideoList);
		model.addAttribute("videoList", videoList);
		model.addAttribute("pi", pi);
		model.addAttribute("filterMap", filterMap);

		
		return "board/video";
	}
	
	
	@GetMapping("/video/upload")
	public String videoUploadForm() {
		
		return "board/videoUploadForm";
	}
	
	@PostMapping("/video/upload")
	public String uploadVideo(Board board,
	                          @RequestParam("videoFile") MultipartFile file,
	                          HttpServletRequest request,
	                          RedirectAttributes redirectAttributes) {
		String changeName = "";
		File dir = null;
	    try {
	        dir = new File(mp4Path);
	        if (!dir.exists()) dir.mkdirs();

	        // 1. mp4 저장
	        changeName = VideoUploadUtil.saveMp4File(file, mp4Path);

	        // 2. ffmpeg → m3u8, ts, 썸네일 생성
	        VideoUploadUtil.convertToHLS(mp4Path, changeName);

	        // 3. DB 저장
	        BoardFile f = new BoardFile();
	        f.setType("video");
	        f.setOriginName(file.getOriginalFilename());
	        f.setChangeName(changeName);
	        f.setFileLevel(1);

	        int result = service.writeLoungeBoard(board, f);

	        if (result > 0) {
	            redirectAttributes.addFlashAttribute("msg", "게시글이 정상적으로 등록되었습니다");
	        } else {
	            // DB 저장 실패 시, 관련된 파일들 전부 삭제
	        	deleteHLSFiles(changeName, new File(mp4Path));

	            redirectAttributes.addFlashAttribute("msg", "게시글 등록에 실패하였습니다.");
	        }

	    } catch (Exception e) {
	    	// DB 저장 실패 시, 관련된 파일들 전부 삭제
            deleteHLSFiles(changeName, new File(mp4Path));
	    	
	        e.printStackTrace();
	        redirectAttributes.addFlashAttribute("msg", "업로드 중 오류가 발생했습니다.");
	    }

	    return "redirect:/board/video";
	}
	
	//영상 업로드 실패시 서버에 업로드된 파일들 완전히 제거
	private void deleteHLSFiles(String changeName, File dir) {
		String baseName = changeName.substring(0,changeName.lastIndexOf("."));
		File[] files = dir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(baseName) &&
					   (name.endsWith(".ts") || name.endsWith(".m3u8") ||
					   name.endsWith(".jpg") || name.endsWith(".mp4"));
			}
		});
		
		if(files != null) {
			for(File f : files) {
				f.delete();
			}
		}
	}
	
	private boolean transferDeletedHLSFiles(String changeName, File dir) {
		String baseName = changeName.substring(0,changeName.lastIndexOf("."));
		File[] files = dir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(baseName) &&
					   (name.endsWith(".ts") || name.endsWith(".m3u8") ||
					   name.endsWith(".jpg") || name.endsWith(".mp4"));
			}
		});
		
		boolean flag = true;
		
		if(files != null) {
			File deletedDir = new File(deletedVideoPath);
			if(!deletedDir.exists()) deletedDir.mkdirs(); 
			for(File f : files) {
				flag = f.renameTo(new File(deletedVideoPath + f.getName()));
			}
		}
		
		return flag;
	}
	
	@GetMapping("/video/getViewCount/{boardNo}")
	@ResponseBody
	public int getViewCount(@PathVariable int boardNo) {
		
		return service.getViewCount(boardNo);
	}
	
	@GetMapping("/video/detail/{boardNo}")
	public String videoDetail(@PathVariable int boardNo, Model model, RedirectAttributes redirectAttribute) {
		
		int result = service.increaseCount(boardNo);
		
		if(result > 0) {
			BoardFile video = service.videoDetail(boardNo);
			
			video = BadgeSetUtil.setBadgeUrl(video);
			
			model.addAttribute("video",video);
			
			return "board/videoDetail";
		}else {
			
			redirectAttribute.addFlashAttribute("msg", "게시글이 정상적으로 조회되지 않았습니다");
			return  "board/video";
		}
		
	}
	
	@PostMapping("/video/delete/{boardNo}")
	public String deleteVideoBoard(@PathVariable int boardNo,RedirectAttributes redirectAttributes) {
		

		Board board = service.loungeBoardDetail(boardNo);
		
		int result = service.deleteBoard(board);//삭제할 게시글 정보 보내기
		
		if(result > 0) {
			
			String changeName = board.getFileList().get(0).getChangeName(); //ex) 123123123.mp4
			
			if(transferDeletedHLSFiles(changeName, new File(mp4Path))) {
				System.out.println("게시글 파일 삭제 완료");
			}else {
				redirectAttributes.addFlashAttribute("msg","영상 삭제 중 오류가 발생하였습니다");
				return "redirect:/board/video/detail/" + boardNo;
			}
		
			
			ArrayList<Reply> replyList = board.getReplyList();
			for(Reply reply : replyList) {
				if(reply.getFileNo() != 0) {
					if(deleteFile(replyImagePath,deletedReplyImagePath,reply.getChangeName())) {
						log.debug("댓글 파일 삭제 완료");
					}else {
						redirectAttributes.addFlashAttribute("msg","댓글 첨부파일 삭제 중 오류가 발생하였습니다");
						return "redirect:/board/video/detail/" + boardNo;
					}
				}
			}
			
			redirectAttributes.addFlashAttribute("msg","게시글이 정상적으로 삭제되었습니다.");
			
			return "redirect:/board/video";
		}else {
			redirectAttributes.addFlashAttribute("msg","게시글 삭제에 실패하였습니다.");
			return "redirect:/board/video/detail/" + boardNo;
		}
		
	}
	
	@GetMapping("/video/update/{boardNo}")
	public String videoUpdateForm(@PathVariable int boardNo, Model model) {
		
		Board board = service.loungeBoardDetail(boardNo);
		
		model.addAttribute("board", board);
		
		return "board/videoUpdateForm";
	}
	
	@PostMapping("/video/update")
	public String updateVideo(Board board, MultipartFile videoFile, RedirectAttributes redirectAttributes) {
		
		String changeName = "";
		File dir = new File(mp4Path);
	    try {
	       
	        if (!dir.exists()) dir.mkdirs();

	        // 1. mp4 저장
	        changeName = VideoUploadUtil.saveMp4File(videoFile, mp4Path);

	        // 2. ffmpeg → m3u8, ts, 썸네일 생성
	        VideoUploadUtil.convertToHLS(mp4Path, changeName);

	        // 3. DB 저장
	        BoardFile f = new BoardFile();
	        f.setBoardNo(board.getBoardNo());
	        f.setType("video");
	        f.setOriginName(videoFile.getOriginalFilename());
	        f.setChangeName(changeName);
	        f.setFileLevel(1);

	        int result = service.updateLoungeBoard(board, f, false);
	        
	        if (result > 0) {
	        	transferDeletedHLSFiles(board.getChangeName(), dir);
	            redirectAttributes.addFlashAttribute("msg", "게시글이 정상적으로 수정되었습니다");
	        } else {
	            // DB 저장 실패 시, 관련된 파일들 전부 삭제
	        	deleteHLSFiles(changeName, dir);

	            redirectAttributes.addFlashAttribute("msg", "게시글 수정에 실패하였습니다.");
	        }

	    } catch (Exception e) {
	    	// DB 저장 실패 시, 관련된 파일들 전부 삭제
            deleteHLSFiles(changeName, dir);
	    	
	        e.printStackTrace();
	        redirectAttributes.addFlashAttribute("msg", "수정 중 오류가 발생했습니다.");
	    }

	    return "redirect:/board/video";
								   
		
	}
	


	/*=======================================공통============================================*/
	
	@GetMapping("/like") //게시물에 좋아요 혹은 싫어요 보내기
	@ResponseBody
	public int likeBoard(int boardNo
						,String type
						,@SessionAttribute Member loginUser) {
		
		BoardLikeDTO dto = BoardLikeDTO.builder()
									   .boardNo(boardNo)
									   .type(type)
									   .userName(loginUser.getUserName())
									   .build();
		
		return service.toggleBoardLike(dto);
	}
	
	@GetMapping("/likeReply") //댓글에 좋아요 혹은 싫어요 보내기
	@ResponseBody
	public int likeReply(int replyNo
						,@SessionAttribute Member loginUser) {
		
		
		HashMap<String, Object> needed = new HashMap<>();
		
		needed.put("replyNo", replyNo);
		needed.put("userName", loginUser.getUserName());
		
		return service.toggleReplyLike(needed);
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


		return changeName; // 서버에 업로드된 파일명 반환
	}
	
	
	public void saveImage(String imagePath, MultipartFile uploadFile,String changeName) {
		try {
			// application.properties에 정의했던 파일경로로 파일 저장
			File dir = new File(imagePath);
			if(!dir.exists()) dir.mkdirs();
			
			uploadFile.transferTo(new File(imagePath + changeName));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	
	
	//파일 삭제(deleted 폴더로 이동시키는 메소드)
	public boolean deleteFile(String originPath,String deletePath,String changeName) {
		
		File deleteDir = new File(deletePath);
		if(!deleteDir.exists()) deleteDir.mkdirs();
		
		File originFile = new File(originPath + changeName);
		File deletedFile = new File(deletePath + changeName);
		return originFile.renameTo(deletedFile); 
		
	}
	
	
	
	
	
}
