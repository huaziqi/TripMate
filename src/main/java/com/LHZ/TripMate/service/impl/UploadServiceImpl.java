package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Value("${upload.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String save(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("文件不能为空");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只支持图片格式");
        }

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : ".jpg";

        String filename = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath();

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败");
        }

        return baseUrl + "/uploads/" + filename;
    }
}
