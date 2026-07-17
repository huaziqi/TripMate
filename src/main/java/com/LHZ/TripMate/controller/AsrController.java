package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.common.ResultCode;
import com.LHZ.TripMate.dto.asr.AsrResponseDTO;
import com.LHZ.TripMate.service.AsrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/asr")
@RequiredArgsConstructor
public class AsrController {

    private final AsrService asrService;

    /**
     * 语音转文字：上传一段完整音频，返回识别文本
     *
     * @param file     音频文件（pcm / wav / mp3，16k 采样率 16bit 单声道，时长 ≤ 60s）
     * @param format   音频格式，缺省时按文件扩展名推断，推断不出按 pcm 处理
     * @param language 识别语种：zh_cn（默认）| en_us
     */
    @PostMapping("/recognize")
    public Result<AsrResponseDTO> recognize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "language", defaultValue = "zh_cn") String language) {

        if (file == null || file.isEmpty()) {
            return Result.fail("请上传音频文件");
        }
        String fmt = format != null && !format.isBlank()
                ? format.toLowerCase(Locale.ROOT)
                : inferFormat(file.getOriginalFilename());
        try {
            return Result.success(asrService.recognize(file.getBytes(), fmt, language));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("语音识别失败：{}", e.getMessage());
            return Result.fail(ResultCode.SERVER_ERROR.getCode(), "语音识别失败：" + e.getMessage());
        }
    }

    private String inferFormat(String filename) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        return "pcm";
    }
}
