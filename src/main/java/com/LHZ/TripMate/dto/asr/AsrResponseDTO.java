package com.LHZ.TripMate.dto.asr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语音转文字响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsrResponseDTO {

    /** 识别出的文本 */
    private String text;

    /** 讯飞本次会话 sid，用于排查问题 */
    private String sid;
}
