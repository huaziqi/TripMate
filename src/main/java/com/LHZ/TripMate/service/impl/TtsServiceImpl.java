package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.tts.PhonemeItemDTO;
import com.LHZ.TripMate.dto.tts.TtsRequestDTO;
import com.LHZ.TripMate.dto.tts.TtsResponseDTO;
import com.LHZ.TripMate.dto.tts.VisemeItemDTO;
import com.LHZ.TripMate.service.TtsService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceRequest;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceResponse;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class TtsServiceImpl implements TtsService {

    @Value("${tencent.tts.secret-id}")
    private String secretId;

    @Value("${tencent.tts.secret-key}")
    private String secretKey;

    @Value("${tencent.tts.region:ap-beijing}")
    private String region;

    @Value("${tencent.tts.codec:wav}")
    private String codec;

    @Value("${tencent.tts.sample-rate:16000}")
    private Long sampleRate;

    @Value("${tripmate.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://127.0.0.1:8080}")
    private String baseUrl;

    /**
     * 未来预留外置对齐服务开关
     * false: 当前使用本地规则解析
     * true : 未来切到外置 Python 对齐服务
     */
    @Value("${tripmate.tts.use-external-align:false}")
    private boolean useExternalAlign;

    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = new HanyuPinyinOutputFormat();

    static {
        PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    private static final List<String> INITIALS = Arrays.asList(
            "zh", "ch", "sh",
            "b", "p", "m", "f",
            "d", "t", "n", "l",
            "g", "k", "h",
            "j", "q", "x",
            "r", "z", "c", "s",
            "y", "w"
    );

    private static final Map<String, String> PHONE_TO_VISEME = new HashMap<>();

    static {
        PHONE_TO_VISEME.put("b", "rest");
        PHONE_TO_VISEME.put("p", "rest");
        PHONE_TO_VISEME.put("m", "rest");
        PHONE_TO_VISEME.put("d", "rest");
        PHONE_TO_VISEME.put("t", "rest");
        PHONE_TO_VISEME.put("n", "rest");
        PHONE_TO_VISEME.put("g", "rest");
        PHONE_TO_VISEME.put("k", "rest");

        PHONE_TO_VISEME.put("f", "I");
        PHONE_TO_VISEME.put("h", "I");
        PHONE_TO_VISEME.put("j", "I");
        PHONE_TO_VISEME.put("q", "I");
        PHONE_TO_VISEME.put("x", "I");
        PHONE_TO_VISEME.put("zh", "I");
        PHONE_TO_VISEME.put("ch", "I");
        PHONE_TO_VISEME.put("sh", "I");
        PHONE_TO_VISEME.put("r", "I");
        PHONE_TO_VISEME.put("z", "I");
        PHONE_TO_VISEME.put("c", "I");
        PHONE_TO_VISEME.put("s", "I");
        PHONE_TO_VISEME.put("l", "I");
        PHONE_TO_VISEME.put("y", "I");

        PHONE_TO_VISEME.put("a", "A");
        PHONE_TO_VISEME.put("ai", "A");
        PHONE_TO_VISEME.put("an", "A");
        PHONE_TO_VISEME.put("ang", "A");

        PHONE_TO_VISEME.put("i", "I");
        PHONE_TO_VISEME.put("e", "I");
        PHONE_TO_VISEME.put("ei", "I");
        PHONE_TO_VISEME.put("en", "I");
        PHONE_TO_VISEME.put("eng", "I");

        PHONE_TO_VISEME.put("u", "U");
        PHONE_TO_VISEME.put("v", "U");
        PHONE_TO_VISEME.put("w", "U");

        PHONE_TO_VISEME.put("o", "O");
        PHONE_TO_VISEME.put("ou", "O");
        PHONE_TO_VISEME.put("ao", "O");
        PHONE_TO_VISEME.put("ong", "O");
        PHONE_TO_VISEME.put("er", "O");
    }

    @Override
    public TtsResponseDTO synthesize(TtsRequestDTO request) {
        GeneratedAudio generatedAudio = generateAudio(request);
        return new TtsResponseDTO(generatedAudio.audioUrl, generatedAudio.sessionId);
    }

    @Override
    public TtsResponseDTO synthesizeWithTimeline(TtsRequestDTO request) {
        GeneratedAudio generatedAudio = generateAudio(request);

        TimelineResult timelineResult;
        if (useExternalAlign) {
            // 这里是未来外置 Python 服务预留位
            // 当前先回退到本地规则解析
            timelineResult = buildTimelineByRule(request.getText(), request.getLang(), generatedAudio.filePath);
        } else {
            timelineResult = buildTimelineByRule(request.getText(), request.getLang(), generatedAudio.filePath);
        }

        return new TtsResponseDTO(
                generatedAudio.audioUrl,
                generatedAudio.sessionId,
                timelineResult.duration,
                timelineResult.phonemes,
                timelineResult.visemes
        );
    }

    /**
     * 统一的 TTS 生成逻辑，保留你原来的能力
     */
    private GeneratedAudio generateAudio(TtsRequestDTO request) {
        String text = request.getText() == null ? "" : request.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("合成文本不能为空");
        }

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
            if (audioBase64 == null || audioBase64.isBlank()) {
                throw new RuntimeException("腾讯云 TTS 返回的音频数据为空，请检查 API 响应: " + response.getRequestId());
            }
            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);

            if (audioBytes.length < 100) {
                throw new RuntimeException("TTS 音频数据异常，文件过小: " + audioBytes.length + " bytes");
            }

            String fileName = sessionId + "." + codec;
            Path dir = Paths.get(uploadDir, "tts").toAbsolutePath().normalize();
            Files.createDirectories(dir);

            Path filePath = dir.resolve(fileName);
            Files.write(filePath, audioBytes);

            String audioUrl = baseUrl + "/uploads/tts/" + fileName;

            return new GeneratedAudio(sessionId, audioUrl, filePath);

        } catch (TencentCloudSDKException e) {
            throw new RuntimeException("腾讯云 TTS 调用失败：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("语音文件生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 当前阶段：按文字规则解析 timeline
     * 把：
     * - 文本清洗
     * - 中文转拼音
     * - 拼音拆声母/韵母
     * - 音素时间分配
     * - viseme 映射
     * - 音频时长计算
     * 全部集中到这个 service 内
     */
    private TimelineResult buildTimelineByRule(String text, String lang, Path audioPath) {
        long duration = detectAudioDurationMs(audioPath);

        if (duration <= 0) {
            duration = 1500L;
        }

        String normalized = normalizeText(text, lang);
        List<PhonemeItemDTO> phonemes = new ArrayList<>();

        if ("zh".equalsIgnoreCase(lang)) {
            phonemes = buildChinesePhonemes(normalized, duration);
        } else {
            phonemes = buildFallbackPhonemes(normalized, duration);
        }

        List<VisemeItemDTO> visemes = buildVisemesFromPhonemes(phonemes);

        return new TimelineResult(duration, phonemes, visemes);
    }

    /**
     * 文本标准化
     */
    private String normalizeText(String text, String lang) {
        if (text == null) {
            return "";
        }
        String result = text.trim()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", "")
                .replaceAll("[“”\"'`]", "");

        // 标点先保留一部分，用于将来做停顿
        return result;
    }

    /**
     * 中文规则版时间轴
     */
    private List<PhonemeItemDTO> buildChinesePhonemes(String text, long durationMs) {
        List<Character> chineseChars = extractChineseChars(text);
        List<PhonemeItemDTO> result = new ArrayList<>();

        if (chineseChars.isEmpty()) {
            return result;
        }

        long lead = 60L;
        long tail = 80L;
        long usable = Math.max(200L, durationMs - lead - tail);

        long perChar = usable / chineseChars.size();
        long current = lead;

        for (int i = 0; i < chineseChars.size(); i++) {
            char ch = chineseChars.get(i);

            long charStart = current;
            long charEnd = (i == chineseChars.size() - 1) ? (lead + usable) : (current + perChar);
            long charDuration = Math.max(40L, charEnd - charStart);

            String pinyin = toPinyin(ch);
            String[] split = splitInitialFinal(pinyin);
            String initial = split[0];
            String fin = split[1];

            // 如果完全拿不到拼音，给一个 rest 占位
            if ((initial == null || initial.isBlank()) && (fin == null || fin.isBlank())) {
                result.add(new PhonemeItemDTO("rest", charStart, charEnd));
                current = charEnd;
                continue;
            }

            if (initial != null && !initial.isBlank() && fin != null && !fin.isBlank()) {
                long initialDuration = Math.max(30L, Math.round(charDuration * getInitialRatio(initial)));
                long initialEnd = Math.min(charEnd - 10L, charStart + initialDuration);

                result.add(new PhonemeItemDTO(initial, charStart, initialEnd));
                result.add(new PhonemeItemDTO(normalizeFinal(fin), initialEnd, charEnd));
            } else if (fin != null && !fin.isBlank()) {
                result.add(new PhonemeItemDTO(normalizeFinal(fin), charStart, charEnd));
            } else {
                result.add(new PhonemeItemDTO(initial, charStart, charEnd));
            }

            current = charEnd;
        }

        return result;
    }

    /**
     * 未来英文或其它语言先走兜底逻辑
     * 这里只做非常粗略的按字符拆分
     */
    private List<PhonemeItemDTO> buildFallbackPhonemes(String text, long durationMs) {
        List<PhonemeItemDTO> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        List<Character> chars = new ArrayList<>();
        for (char c : text.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                chars.add(c);
            }
        }

        if (chars.isEmpty()) {
            return result;
        }

        long lead = 60L;
        long tail = 80L;
        long usable = Math.max(200L, durationMs - lead - tail);
        long per = usable / chars.size();
        long current = lead;

        for (int i = 0; i < chars.size(); i++) {
            long start = current;
            long end = (i == chars.size() - 1) ? (lead + usable) : (current + per);

            String token = String.valueOf(Character.toLowerCase(chars.get(i)));
            String phone;
            if ("ae".contains(token)) {
                phone = "a";
            } else if ("i".equals(token) || "y".equals(token)) {
                phone = "i";
            } else if ("o".equals(token)) {
                phone = "o";
            } else if ("u".equals(token) || "w".equals(token)) {
                phone = "u";
            } else {
                phone = "rest";
            }

            result.add(new PhonemeItemDTO(phone, start, end));
            current = end;
        }

        return result;
    }

    /**
     * 从 phoneme 构建 viseme，并合并连续相同 viseme 段
     */
    private List<VisemeItemDTO> buildVisemesFromPhonemes(List<PhonemeItemDTO> phonemes) {
        List<VisemeItemDTO> source = new ArrayList<>();
        if (phonemes == null || phonemes.isEmpty()) {
            return source;
        }

        for (PhonemeItemDTO item : phonemes) {
            String viseme = PHONE_TO_VISEME.getOrDefault(item.getPhone(), "rest");
            source.add(new VisemeItemDTO(viseme, item.getStart(), item.getEnd()));
        }

        List<VisemeItemDTO> merged = new ArrayList<>();
        for (VisemeItemDTO item : source) {
            if (merged.isEmpty()) {
                merged.add(new VisemeItemDTO(item.getViseme(), item.getStart(), item.getEnd()));
                continue;
            }

            VisemeItemDTO last = merged.get(merged.size() - 1);
            if (Objects.equals(last.getViseme(), item.getViseme()) && last.getEnd() == item.getStart()) {
                last.setEnd(item.getEnd());
            } else {
                merged.add(new VisemeItemDTO(item.getViseme(), item.getStart(), item.getEnd()));
            }
        }

        return merged;
    }

    /**
     * 中文转拼音（无声调）
     */
    private String toPinyin(char ch) {
        try {
            String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, PINYIN_FORMAT);
            if (arr != null && arr.length > 0) {
                return arr[0];
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * 拆分声母 / 韵母
     */
    private String[] splitInitialFinal(String pinyin) {
        if (pinyin == null || pinyin.isBlank()) {
            return new String[]{"", ""};
        }

        for (String initial : INITIALS) {
            if (pinyin.startsWith(initial)) {
                return new String[]{initial, pinyin.substring(initial.length())};
            }
        }

        return new String[]{"", pinyin};
    }

    /**
     * 韵母标准化
     * 这里先做最基础版本，够 Live2D 口型使用
     */
    private String normalizeFinal(String fin) {
        if (fin == null || fin.isBlank()) {
            return "";
        }

        return switch (fin) {
            case "ua" -> "a";
            case "uo" -> "o";
            case "ie" -> "i";
            case "ve", "ue" -> "v";
            case "iu" -> "u";
            case "ui" -> "i";
            case "un" -> "en";
            default -> fin;
        };
    }

    /**
     * 不同声母占字内时长比例
     * 爆破音短一点，摩擦音可稍长
     */
    private double getInitialRatio(String initial) {
        if (initial == null || initial.isBlank()) {
            return 0.0;
        }

        return switch (initial) {
            case "b", "p", "d", "t", "g", "k" -> 0.18;
            case "zh", "ch", "sh", "z", "c", "s", "j", "q", "x", "h", "f", "r" -> 0.28;
            default -> 0.25;
        };
    }

    /**
     * 提取中文字符
     */
    private List<Character> extractChineseChars(String text) {
        List<Character> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                result.add(c);
            }
        }
        return result;
    }

    private boolean isChinese(char c) {
        return String.valueOf(c).matches("[\\u4e00-\\u9fa5]");
    }

    /**
     * 计算音频时长（毫秒）
     *
     * 说明：
     * 1. Java 原生对 mp3 时长支持不稳定
     * 2. 如果你的 codec 配置为 wav，读取会更稳定
     * 3. 如果当前 mp3 读不到，就做兜底估算
     */
    private long detectAudioDurationMs(Path audioPath) {
        if (audioPath == null || !Files.exists(audioPath)) {
            return 0L;
        }

        try {
            long fileSize = Files.size(audioPath);
            if (fileSize <= 0) {
                return 0L;
            }

            File file = audioPath.toFile();
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);

            Map<String, Object> properties = fileFormat.properties();
            if (properties != null) {
                Object duration = properties.get("duration");
                if (duration instanceof Long d) {
                    return d / 1000L;
                }
                if (duration instanceof Number d) {
                    return d.longValue() / 1000L;
                }
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            try {
                long frames = ais.getFrameLength();
                float frameRate = ais.getFormat().getFrameRate();
                if (frames > 0 && frameRate > 0) {
                    return (long) (frames / frameRate * 1000);
                }
            } finally {
                ais.close();
            }
        } catch (UnsupportedAudioFileException e) {
            // ignore
        } catch (Exception e) {
            // ignore
        }

        try {
            long size = Files.size(audioPath);
            if ("mp3".equalsIgnoreCase(codec)) {
                long estimatedMs = Math.max(500L, size * 1000L / 7000L);
                return estimatedMs;
            }
            if ("wav".equalsIgnoreCase(codec)) {
                long pcmDataSize = size - 44;
                if (pcmDataSize > 0) {
                    long bytesPerSecond = sampleRate * 2;
                    return Math.max(500L, pcmDataSize * 1000L / bytesPerSecond);
                }
            }
        } catch (Exception ignored) {
        }

        return 0L;
    }
    /**
     * 未来外置 Python 服务预留的数据结构
     * 当前先不拆成单独 service，避免你说“太多工具类”
     */
    @SuppressWarnings("unused")
    private TimelineResult buildTimelineByExternalService(String text, String lang, Path audioPath) {
        // 未来这里可用 RestTemplate / WebClient 调 Python:
        // POST /align
        // {
        //   "text": text,
        //   "lang": lang,
        //   "audioPath": audioPath.toAbsolutePath().toString()
        // }
        //
        // 当前先回退本地规则解析
        return buildTimelineByRule(text, lang, audioPath);
    }

    private static class GeneratedAudio {
        private final String sessionId;
        private final String audioUrl;
        private final Path filePath;

        public GeneratedAudio(String sessionId, String audioUrl, Path filePath) {
            this.sessionId = sessionId;
            this.audioUrl = audioUrl;
            this.filePath = filePath;
        }
    }

    private static class TimelineResult {
        private final long duration;
        private final List<PhonemeItemDTO> phonemes;
        private final List<VisemeItemDTO> visemes;

        public TimelineResult(long duration, List<PhonemeItemDTO> phonemes, List<VisemeItemDTO> visemes) {
            this.duration = duration;
            this.phonemes = phonemes;
            this.visemes = visemes;
        }
    }
}