package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.common.ResultCode;
import com.LHZ.TripMate.dto.TranslationRequestDTO;
import com.LHZ.TripMate.dto.TranslationResponseDTO;
import com.LHZ.TripMate.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    @PostMapping("/translate")
    public Result<TranslationResponseDTO> translate(@RequestBody TranslationRequestDTO request) {
        if (request.getText() == null || request.getText().isBlank()) {
            return Result.fail("请输入要翻译的文字");
        }
        if (request.getText().length() > 500) {
            return Result.fail("文字长度不能超过 500 字符");
        }
        try {
            TranslationResponseDTO data = translationService.translate(
                    request.getText().trim(),
                    request.getFrom() != null ? request.getFrom() : "zh",
                    request.getTo() != null ? request.getTo() : "en"
            );
            return Result.success(data);
        } catch (Exception e) {
            log.error("翻译失败：{}", e.getMessage());
            return Result.fail(ResultCode.SERVER_ERROR);
        }
    }
}
