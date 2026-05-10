package com.videotogether.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    // Store videos in a temporary directory for now
    private final String uploadDir = System.getProperty("java.io.tmpdir") + "/videotogether_uploads/";

    public VideoController() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate a unique filename to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;
            
            Path path = Paths.get(uploadDir + fileName);
            Files.write(path, file.getBytes());

            response.put("fileName", fileName);
            response.put("streamUrl", "/api/video/stream/" + fileName);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/stream/{fileName}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String fileName) {
        File file = new File(uploadDir + fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        
        // Spring Boot automatically handles Range requests when returning a Resource
        // but we can explicitly set the content type
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType != null) {
                mediaType = MediaType.parseMediaType(mimeType);
            }
        } catch (IOException e) {
            // Ignore, default to octet stream
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
