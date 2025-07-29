package com.mafia.game.board.controller;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mafia.game.board.model.service.BoardService;
import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.FileDownloadDTO;
import com.mafia.game.board.model.vo.Reply;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.BadgeSetUtil;
import com.mafia.game.common.template.Pagination;

import jakarta.data.repository.Delete;

@RestController
@RequestMapping("/api/board")
public class BoardAdminController {

    @Autowired
    private BoardService service;

    @Value("${file.uploadLoungImage.path}")
    private String loungeImagePath;

    @Value("${file.uploadReplyImage.path}")
    private String replyImagePath;

    @Value("${file.deletedReplyImage.path}")
    private String deletedReplyImagePath;

    @Value("${file.deletedLoungeImage.path}")
    private String deletedLoungeImagePath;
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getLoungeBoardList(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(required = false) String typeName,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> response = new HashMap<>();
        try {
            HashMap<String, String> filterMap = new HashMap<>();
            filterMap.put("typeName", typeName);
            filterMap.put("condition", condition);
            filterMap.put("keyword", keyword);
            filterMap.put("typeClass", null);

            if (typeName != null && !"".equals(typeName) && !"자유".equals(typeName) && !"플레이".equals(typeName)) {
                filterMap.put("typeClass", "3");
            }

            int listCount = service.listCount(filterMap);
            int pageLimit = 10;
            int boardLimit = 10;
            PageInfo pi = Pagination.getPageInfo(listCount, currentPage, pageLimit, boardLimit);

            ArrayList<Board> boardList = service.boardList(filterMap, pi);
            ArrayList<Board> topLikedList = service.topLikedList(filterMap);


            response.put("success", true);
            response.put("boardList", boardList);
            response.put("listCount", listCount);
            response.put("filterMap", filterMap);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "게시판 데이터를 가져오는 중 오류 발생");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    
    @GetMapping("/lounge/detail/{boardNo}")
    public ResponseEntity<?> loungeDetail(@PathVariable int boardNo) {
        Board board = service.loungeBoardDetail(boardNo);
        board = BadgeSetUtil.setBadgeUrl(board);
        
        return ResponseEntity.ok(board);
    }

    @PostMapping("/lounge/delete/{boardNo}")
    public ResponseEntity<?> deleteLoungeBoard(@PathVariable int boardNo) {
        Board board = service.loungeBoardDetail(boardNo);
        int result = service.deleteBoard(board);
        if (result > 0) {
            deleteFileIfExists(board);
            deleteReplyFiles(board.getReplyList());
            return ResponseEntity.ok("삭제 성공");
        } else {
            return ResponseEntity.status(500).body("삭제 실패");
        }
    }
    

    @GetMapping("/getReplyList")
    public ResponseEntity<?> getReplyList(int boardNo) {
        ArrayList<Reply> replyList = service.getReplyList(boardNo);
        for (Reply r : replyList) {
            BadgeSetUtil.setBadgeUrl(r);
            if (r.getReplyContent() == null) {
                r.setReplyContent("");
            }
        }
        return ResponseEntity.ok(replyList);
    }

    @DeleteMapping("/deleteReply")
    public ResponseEntity<?> deleteReply(int replyNo) {
        Reply reply = service.selectReply(replyNo);
        String changeName = reply.getChangeName();
        int result = service.deleteReply(reply);
        if (result > 0 && changeName != null) {
            deleteFile(replyImagePath, deletedReplyImagePath, changeName);
        }
        return ResponseEntity.ok(result);
    }
    
    private void classifyType(Board board, String jobTypeName) {
        String typeName = board.getTypeName();
        if ("직업".equals(typeName)) {
            board.setTypeName(jobTypeName);
            board.setTypeClass(3);
        } else if ("자유".equals(typeName)) {
            board.setTypeClass(1);
        } else if ("플레이".equals(typeName)) {
            board.setTypeClass(2);
        }
    }  
    
    @PostMapping("/file/download")
    public ResponseEntity<Resource> downloadFile(@RequestBody FileDownloadDTO fileDTO) throws IOException {
    	
    	
    	
    	String filePath = "C:/godDaddy_upload";
    	
    	if(fileDTO.getTypeName() != null) {
    		BoardFile file = fileDTO.getFile();
    		String typeName = fileDTO.getTypeName();
    		
    		switch(typeName) {
    		case "" : filePath += "Image/loungeImage/" + file.getChangeName(); break;
    		case "갤러리" : filePath += "Image/galleryImage/" + file.getChangeName(); break;
    		case "영상" : filePath += "Video/hls/" + file.getChangeName(); break;
    		}
    	}else{
    		filePath += "Image/ReplyImage/" + fileDTO.getChangeName();
    	}
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encodedFilename = URLEncoder.encode("아무말", StandardCharsets.UTF_8.toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
    
    @GetMapping("/file/download")
    public ResponseEntity<Resource> downloadFile(String changeName) throws IOException {
    	
    	
    	
    	String filePath = "C:/godDaddy_uploadImage/ReplyImage/" + changeName;
    	
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encodedFilename = URLEncoder.encode("아무말", StandardCharsets.UTF_8.toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
    

    private void deleteFileIfExists(Board board) {
        if (board.getFileList() != null && !board.getFileList().isEmpty()) {
            BoardFile file = board.getFileList().get(0);
            deleteFile(loungeImagePath, deletedLoungeImagePath, file.getChangeName());
        }
    }

    private void deleteReplyFiles(ArrayList<Reply> replies) {
        for (Reply r : replies) {
            if (r.getFileNo() != 0) {
                deleteFile(replyImagePath, deletedReplyImagePath, r.getChangeName());
            }
        }
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
