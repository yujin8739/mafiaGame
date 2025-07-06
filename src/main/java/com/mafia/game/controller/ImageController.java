package com.mafia.game.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/images")
public class ImageController {

    private static final String IMAGE_FOLDER = "C:/godDaddy_uploadImage/gameEvent";

    @GetMapping("/events/{fileName:.+}")
    public ResponseEntity<InputStreamResource> getEventImage(
            @PathVariable String fileName,
            HttpServletResponse response) throws IOException {

        File file = new File(IMAGE_FOLDER, fileName);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // MIME 타입 추론
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.length())
                .body(resource);
    }
}
