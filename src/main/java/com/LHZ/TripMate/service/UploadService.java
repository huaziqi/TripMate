package com.LHZ.TripMate.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    /** 保存文件到 uploads/ 目录，返回可访问的完整 URL */
    String save(MultipartFile file);
}
