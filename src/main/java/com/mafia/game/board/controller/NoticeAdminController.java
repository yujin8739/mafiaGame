package com.mafia.game.board.controller;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mafia.game.board.model.service.NoticeService;
import com.mafia.game.board.model.vo.Notice;
import com.mafia.game.common.model.vo.PageInfo;
import com.mafia.game.common.template.Pagination;

@RestController
@RequestMapping("/api") 
public class NoticeAdminController {

	@Autowired
	private NoticeService nService;
	
	@Value("${file.uploadNotice.path}")
    private String uploadPath;
	
	//공지사항 리스트
	@GetMapping("/notices")
	public HashMap<String, Object> getNotices(
				@RequestParam(defaultValue = "1") int currentPage,
				@RequestParam(defaultValue = "") String keyword,
	            @RequestParam(defaultValue = "title") String condition,
	            @RequestParam(defaultValue = "byDate") String sort
			) {
		HashMap<String, Object> result = new HashMap<>();
		
		HashMap<String, String> noticeMap = new HashMap<>();
        noticeMap.put("keyword", keyword);
        noticeMap.put("condition", condition);
        noticeMap.put("sort", sort);

        int noticeCount = nService.noticeCount(noticeMap);
        int pageLimit = 10;
        int boardLimit = 10;

        PageInfo pi = Pagination.getPageInfo(noticeCount, currentPage, pageLimit, boardLimit);
        ArrayList<Notice> noticeList = nService.noticeList(noticeMap, pi);

        result.put("noticeList", noticeList);
        result.put("pi", pi);
        System.out.println(noticeList.toString());
        return result;
	}
	
	//공지사항 삭제
	@DeleteMapping("/noticeDelete/{noticeNo}")
	public ResponseEntity<String> deleteNotice(@PathVariable int noticeNo) {
	    // 1. 삭제할 notice 객체 조회
	    Notice notice = nService.selectNotice(noticeNo);
	    if (notice == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 공지사항을 찾을 수 없습니다.");
	    }

	    // 2. DB 삭제
	    int result = nService.deleteNotice(noticeNo);

	    if (result > 0) {
	        // 3. 첨부파일 삭제
	        if (notice.getChangeName() != null && !notice.getChangeName().isEmpty()) {
	            // 파일 경로에서 마지막 파일명만 추출
	            String fileName = notice.getChangeName().substring(notice.getChangeName().lastIndexOf("/") + 1);
	            String fullPath = uploadPath + fileName;

	            File file = new File(fullPath);
	            if (file.exists()) {
	                file.delete();
	            }
	        }

	        return ResponseEntity.ok("공지사항이 삭제되었습니다.");
	    } else {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("공지사항 삭제에 실패했습니다.");
	    }
	}
	
	//공지사항 수정
	@PutMapping("/noticeUpdate/{noticeNo}")
	public ResponseEntity<String> updateNotice(
	        @PathVariable int noticeNo,
	        @RequestPart("notice") Notice notice,
	        @RequestPart(value = "file", required = false) MultipartFile reUploadFile) {

	    try {
	        // 기존 공지 조회
	        Notice existingNotice = nService.selectNotice(noticeNo);
	        if (existingNotice == null) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("공지사항을 찾을 수 없습니다.");
	        }

	        String deleteFile = null;

	        // 새 파일 업로드 처리
	        if (reUploadFile != null && !reUploadFile.isEmpty()) {
	            if (existingNotice.getChangeName() != null && !existingNotice.getChangeName().isEmpty()) {
	                deleteFile = uploadPath + existingNotice.getChangeName().substring(existingNotice.getChangeName().lastIndexOf("/") + 1);
	            }

	            String changeName = saveFile(reUploadFile);
	            notice.setOriginName(reUploadFile.getOriginalFilename());
	            notice.setChangeName("/resources/uploadFile/" + changeName);
	        } else {
	            notice.setOriginName(existingNotice.getOriginName());
	            notice.setChangeName(existingNotice.getChangeName());
	        }

	        notice.setNoticeNo(noticeNo);
	        int result = nService.updateNotice(notice);

	        if (result > 0) {
	            if (deleteFile != null) {
	                File file = new File(deleteFile);
	                if (file.exists()) {
	                    file.delete();
	                }
	            }
	            return ResponseEntity.ok("공지사항이 수정되었습니다.");
	        } else {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("공지사항 수정 실패");
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생: " + e.getMessage());
	    }
	}
	
	//공지사항 등록
	@PostMapping("/noticeUpload")
    public ResponseEntity<String> uploadNotice(@RequestPart("notice") Notice notice,
                                               @RequestPart(value = "file", required = false) MultipartFile uploadFile) {
        try {
            // 파일 업로드 처리
            if (uploadFile != null && !uploadFile.isEmpty()) {
                String changeName = saveFile(uploadFile);
                notice.setOriginName(uploadFile.getOriginalFilename());
                // ✅ DB에는 웹 접근 가능한 경로 저장
                notice.setChangeName("/resources/uploadFile/" + changeName);
            }

            int result = nService.writeNotice(notice);

            if (result > 0) {
                return ResponseEntity.ok("공지사항 등록 성공");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("공지사항 등록 실패");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류: " + e.getMessage());
        }
    }
	
	public String saveFile(MultipartFile uploadFile) {
        String originName = uploadFile.getOriginalFilename();
        String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int ranNum = (int) (Math.random() * 90000 + 10000);
        String ext = originName.substring(originName.lastIndexOf("."));
        String changeName = currentTime + ranNum + ext;

        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();  // 경로가 없으면 자동 생성

        try {
            uploadFile.transferTo(new File(uploadPath + changeName));
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }

        return changeName;
	
	}
	
	@GetMapping("/download/{fileName}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) throws IOException {
	    String filePath = uploadPath + fileName;
	    Resource resource = new FileSystemResource(filePath);

	    if (!resource.exists()) {
	        return ResponseEntity.notFound().build();
	    }

	    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
	                                       .replaceAll("\\+", "%20");

	    return ResponseEntity.ok()
	        .contentType(MediaType.APPLICATION_OCTET_STREAM)
	        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
	        .body(resource);
	}
}
}
