package com.LHZ.TripMate.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class FileServeController {

    @Value("${tripmate.upload-dir:uploads}")
    private String uploadDir;

    // 1×1 透明 GIF，用于文件不存在时的占位图
    private static final byte[] TRANSPARENT_GIF = {
        (byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38,(byte)0x39,(byte)0x61,
        0x01,0x00,0x01,0x00,(byte)0x80,0x00,0x00,(byte)0xff,(byte)0xff,
        (byte)0xff,0x00,0x00,0x00,0x21,(byte)0xf9,0x04,0x01,0x00,0x00,
        0x00,0x00,0x2c,0x00,0x00,0x00,0x00,0x01,0x00,0x01,0x00,
        0x00,0x02,0x02,0x44,0x01,0x00,0x3b
    };

    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String relativePath = uri.substring("/uploads/".length());

        Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(relativePath);

        if (Files.exists(filePath) && Files.isReadable(filePath)) {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new FileSystemResource(filePath));
        }

        // 文件不存在：返回透明占位图，不报 404
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/gif"))
                .body(new ByteArrayResource(TRANSPARENT_GIF));
    }
}
