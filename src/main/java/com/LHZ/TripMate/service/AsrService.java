package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.asr.AsrResponseDTO;

/**
 * 语音转文字（科大讯飞 语音听写 IAT）
 */
public interface AsrService {

    /**
     * 识别一段完整音频
     *
     * @param audio    音频字节（pcm / wav / mp3）
     * @param format   音频格式：pcm | wav | mp3
     * @param language 识别语种：zh_cn | en_us
     * @return 识别结果文本
     */
    AsrResponseDTO recognize(byte[] audio, String format, String language);
}
