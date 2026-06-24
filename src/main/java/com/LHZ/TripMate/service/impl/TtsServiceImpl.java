package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.tts.TtsRequestDTO;
import com.LHZ.TripMate.dto.tts.TtsResponseDTO;
import com.LHZ.TripMate.service.TtsService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceRequest;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class TtsServiceImpl implements TtsService {

    @Value("${tencent.tts.secret-id}")
    private String secretId;

    @Value("${tencent.tts.secret-key}")
    private String secretKey;

    @Value("${tencent.tts.region:ap-beijing}")
    private String region;

    @Value("${tencent.tts.codec:mp3}")
    private String codec;

    @Value("${tencent.tts.sample-rate:16000}")
    private Long sampleRate;

    @Value("${tripmate.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://127.0.0.1:8080}")
    private String baseUrl;

    @Override
    public TtsResponseDTO synthesize(TtsRequestDTO request) {
        String text = request.getText() == null ? "" : request.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("合成文本不能为空");
        }

        // 基础语音合成先做短文本。AI长回复后面可以再做分段合成。
        if (text.length() > 150) {
            throw new IllegalArgumentException("文本过长，请先控制在150字以内");
        }

        try {
            Credential credential = new Credential(secretId, secretKey);

            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("tts.tencentcloudapi.com");

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            TtsClient client = new TtsClient(credential, region, clientProfile);

            String sessionId = UUID.randomUUID().toString();

            String lang = request.getLang() == null ? "zh" : request.getLang().toLowerCase();
            Long voiceType;
            Long primaryLanguage;
            switch (lang) {
                case "zh" -> {
                    voiceType = 601009L;
                    primaryLanguage = 1L;
                }
                case "en" -> {
                    voiceType = 101016L;
                    primaryLanguage = 2L;
                }
                default -> throw new RuntimeException("不支持该语言的语音合成：" + lang);
            }

            TextToVoiceRequest ttsRequest = new TextToVoiceRequest();
            ttsRequest.setText(text);
            ttsRequest.setSessionId(sessionId);
            ttsRequest.setVoiceType(voiceType);
            ttsRequest.setCodec(codec);
            ttsRequest.setSampleRate(sampleRate);
            ttsRequest.setPrimaryLanguage(primaryLanguage);
            ttsRequest.setModelType(1L);
            ttsRequest.setSpeed(0.3F);
            ttsRequest.setVolume(0F);

            TextToVoiceResponse response = client.TextToVoice(ttsRequest);

            String audioBase64 = response.getAudio();
            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);

            String fileName = sessionId + "." + codec;

            Path dir = Paths.get(uploadDir, "tts").toAbsolutePath().normalize();
            Files.createDirectories(dir);

            Path filePath = dir.resolve(fileName);
            Files.write(filePath, audioBytes);

            String audioUrl = baseUrl + "/uploads/tts/" + fileName;

            return new TtsResponseDTO(audioUrl, sessionId);
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException("腾讯云 TTS 调用失败：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("语音文件生成失败：" + e.getMessage(), e);
        }
    }
}