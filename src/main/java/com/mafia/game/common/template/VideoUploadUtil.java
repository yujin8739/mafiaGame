package com.mafia.game.common.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID; //Universally Unique Identifier : 서버에서 고유 식별자 얻어내기 위한 유틸 클래스 (ex : 파일명, 세션ID, 고유 식별자)

import org.springframework.web.multipart.MultipartFile;

public class VideoUploadUtil { //비디오를 업로드하기위한 유틸 클래스
	
	
	//비디오 파일 서버에 저장 및 최종 변경된 파일명 반환하는 메소드
    public static String saveMp4File(MultipartFile file, String videoUploadDir) throws Exception {
        String originalName = file.getOriginalFilename(); //사용자로부터 넘어온 MultipartFile의 원본명 얻기 => originalName
        String ext = originalName.substring(originalName.lastIndexOf(".")); //원본명에서 확장자만 얻기 => ext
        String uuid = UUID.randomUUID().toString().replaceAll("-", ""); //UUID(고유식별자) 얻기, -은 제거하기!
        String changeName = uuid + ext; //최종 변경된 파일명

        File saveFile = new File(videoUploadDir + changeName); //매개변수로 전달된 경로로 파일 객체 생성
        file.transferTo(saveFile); //해당 경로에 파일 저장

        return changeName;//최종 변경된 파일명 반환
    }
    
    
    
    public static void convertToHLS(String videoUploadDir, String fileName) throws IOException, InterruptedException {
        String inputPath = videoUploadDir + fileName;
        String baseName = fileName.substring(0, fileName.lastIndexOf("."));
        String outputM3u8Path = videoUploadDir + baseName + ".m3u8";
        String outputThumbnailPath = videoUploadDir + baseName + "_thumb.jpg";

        // HLS 변환 명령어 - 덮어쓰기 옵션 -y 추가
        String[] hlsCommand = {
            "ffmpeg", "-y",
            "-i", inputPath,
            "-codec:", "copy",
            "-start_number", "0",
            "-hls_time", "10",
            "-hls_list_size", "0",
            "-f", "hls",
            outputM3u8Path
        };

        // 썸네일 추출 명령어 - 덮어쓰기 옵션 -y 추가
        String[] thumbnailCommand = {
            "ffmpeg", "-y",
            "-i", inputPath,
            "-ss", "00:00:01.000",
            "-vframes", "1",
            outputThumbnailPath
        };

        // HLS 변환 실행
        runProcess(hlsCommand, "HLS 변환");

        // 썸네일 생성 실행
        runProcess(thumbnailCommand, "썸네일 생성");
    }
    
    
    private static void runProcess(String[] command, String desc) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // stderr도 같이 읽음

        Process process = pb.start();

        // 로그 출력 (중요!)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(desc + " 로그: " + line);
            }
        }

        int exitCode = process.waitFor();
        System.out.println(desc + " 종료 코드: " + exitCode);
    }


    
    
}

