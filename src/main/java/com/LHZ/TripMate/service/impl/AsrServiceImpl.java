package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.asr.AsrResponseDTO;
import com.LHZ.TripMate.service.AsrService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 科大讯飞语音听写（IAT）WebSocket 协议实现
 *
 * <p>协议文档：https://www.xfyun.cn/doc/asr/voicedictation/API.html
 * <p>流程：HMAC-SHA256 签名握手 → 分帧上传音频（首帧带 common/business 参数，
 * 尾帧 status=2 结束）→ 监听结果帧拼接文本，data.status=2 表示识别完成。
 */
@Slf4j
@Service
public class AsrServiceImpl implements AsrService {

    /** 每帧音频字节数，官方建议 1280（约 40ms 的 16k/16bit 单声道音频） */
    private static final int FRAME_SIZE = 1280;

    @Value("${xfyun.asr.host-url}")
    private String hostUrl;

    @Value("${xfyun.asr.appid}")
    private String appId;

    @Value("${xfyun.asr.api-key}")
    private String apiKey;

    @Value("${xfyun.asr.api-secret}")
    private String apiSecret;

    @Value("${xfyun.asr.frame-interval-ms:40}")
    private long frameIntervalMs;

    @Value("${xfyun.asr.timeout-seconds:60}")
    private long timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AsrResponseDTO recognize(byte[] audio, String format, String language) {
        String fmt = format == null ? "pcm" : format.toLowerCase(Locale.ROOT);
        String encoding = switch (fmt) {
            case "pcm", "wav" -> "raw";
            case "mp3" -> "lame";
            default -> throw new IllegalArgumentException("不支持的音频格式：" + fmt + "（支持 pcm / wav / mp3）");
        };
        String lang = language == null || language.isBlank() ? "zh_cn" : language;
        if (!lang.equals("zh_cn") && !lang.equals("en_us")) {
            throw new IllegalArgumentException("不支持的语种：" + lang + "（支持 zh_cn / en_us）");
        }
        // wav 本质是带头的 pcm，讯飞 raw 编码只收裸 pcm，需要去掉文件头
        byte[] payload = "wav".equals(fmt) ? stripWavHeader(audio) : audio;
        if (payload.length == 0) {
            throw new IllegalArgumentException("音频内容为空");
        }

        IatSession session = new IatSession();
        WebSocket webSocket = null;
        try {
            String authUrl = buildAuthUrl();
            webSocket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(authUrl), session)
                    .join();

            sendAudioFrames(webSocket, session, payload, encoding, lang);

            if (!session.latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new RuntimeException("语音识别超时（" + timeoutSeconds + "s）");
            }
            if (session.error != null) {
                throw new RuntimeException(session.error);
            }
            return new AsrResponseDTO(session.text.toString(), session.sid);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("语音识别被中断", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("讯飞语音识别调用失败：" + e.getMessage(), e);
        } finally {
            if (webSocket != null && !webSocket.isOutputClosed()) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
        }
    }

    /**
     * 分帧上传音频：首帧 status=0 携带 common/business 参数，中间帧 status=1，尾帧 status=2
     */
    private void sendAudioFrames(WebSocket webSocket, IatSession session,
                                 byte[] audio, String encoding, String language)
            throws InterruptedException {
        boolean first = true;
        for (int offset = 0; offset < audio.length; offset += FRAME_SIZE) {
            if (session.finished.get()) {
                break; // 服务端已返回错误或提前结束，停止上传
            }
            int len = Math.min(FRAME_SIZE, audio.length - offset);
            String chunk = Base64.getEncoder()
                    .encodeToString(Arrays.copyOfRange(audio, offset, offset + len));

            ObjectNode frame = objectMapper.createObjectNode();
            if (first) {
                frame.putObject("common").put("app_id", appId);
                ObjectNode business = frame.putObject("business");
                business.put("language", language);
                business.put("domain", "iat");
                business.put("accent", "mandarin");
                // 静默多久判定为语音结束（ms），调大避免长句中间停顿被截断
                business.put("vad_eos", 5000);
                business.put("ptt", 1); // 返回标点
            }
            ObjectNode data = frame.putObject("data");
            data.put("status", first ? 0 : 1);
            data.put("format", "audio/L16;rate=16000");
            data.put("encoding", encoding);
            data.put("audio", chunk);

            webSocket.sendText(frame.toString(), true).join();
            first = false;
            Thread.sleep(frameIntervalMs);
        }

        // 尾帧：通知服务端音频发送完毕
        ObjectNode endFrame = objectMapper.createObjectNode();
        ObjectNode endData = endFrame.putObject("data");
        endData.put("status", 2);
        endData.put("format", "audio/L16;rate=16000");
        endData.put("encoding", encoding);
        endData.put("audio", "");
        webSocket.sendText(endFrame.toString(), true).join();
    }

    /**
     * 按讯飞规范生成带 HMAC-SHA256 签名的握手 URL
     */
    private String buildAuthUrl() throws Exception {
        URI uri = URI.create(hostUrl);
        String host = uri.getHost();
        String date = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                .withZone(ZoneId.of("GMT"))
                .format(Instant.now());

        String signatureOrigin = "host: " + host + "\n"
                + "date: " + date + "\n"
                + "GET " + uri.getPath() + " HTTP/1.1";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder()
                .encodeToString(mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));

        String authorizationOrigin = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", signature);
        String authorization = Base64.getEncoder()
                .encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        return hostUrl + "?authorization=" + urlEncode(authorization)
                + "&date=" + urlEncode(date)
                + "&host=" + urlEncode(host);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * 去掉 wav 文件头，返回裸 pcm 数据；找不到 data 块时原样返回
     */
    private byte[] stripWavHeader(byte[] bytes) {
        if (bytes.length < 44
                || bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F' || bytes[3] != 'F') {
            return bytes;
        }
        // RIFF 头 12 字节之后是若干 chunk：4 字节 id + 4 字节小端长度 + 内容
        int pos = 12;
        while (pos + 8 <= bytes.length) {
            int size = (bytes[pos + 4] & 0xFF)
                    | (bytes[pos + 5] & 0xFF) << 8
                    | (bytes[pos + 6] & 0xFF) << 16
                    | (bytes[pos + 7] & 0xFF) << 24;
            if (bytes[pos] == 'd' && bytes[pos + 1] == 'a'
                    && bytes[pos + 2] == 't' && bytes[pos + 3] == 'a') {
                int start = pos + 8;
                int end = Math.min(start + size, bytes.length);
                return Arrays.copyOfRange(bytes, start, end);
            }
            pos += 8 + size + (size & 1); // chunk 按 2 字节对齐
        }
        return bytes;
    }

    /**
     * 一次识别会话的状态：拼接结果、错误信息、完成信号
     */
    private class IatSession implements WebSocket.Listener {

        final StringBuilder text = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean finished = new AtomicBoolean(false);
        volatile String error;
        volatile String sid;

        private final StringBuilder messageBuffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                handleMessage(messageBuffer.toString());
                messageBuffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable throwable) {
            log.error("讯飞 ASR WebSocket 异常", throwable);
            error = "WebSocket 连接异常：" + throwable.getMessage();
            complete();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            // 正常流程在收到 status=2 结果后已 complete；此处兜底服务端异常断开
            if (!finished.get()) {
                error = "连接被服务端关闭（" + statusCode + " " + reason + "）";
                complete();
            }
            return null;
        }

        private void handleMessage(String message) {
            try {
                JsonNode root = objectMapper.readTree(message);
                if (root.hasNonNull("sid")) {
                    sid = root.get("sid").asText();
                }
                int code = root.path("code").asInt();
                if (code != 0) {
                    error = "讯飞识别失败（code=" + code + "）：" + root.path("message").asText();
                    complete();
                    return;
                }
                JsonNode result = root.path("data").path("result");
                for (JsonNode ws : result.path("ws")) {
                    for (JsonNode cw : ws.path("cw")) {
                        text.append(cw.path("w").asText());
                    }
                }
                if (root.path("data").path("status").asInt() == 2) {
                    complete();
                }
            } catch (Exception e) {
                error = "识别结果解析失败：" + e.getMessage();
                complete();
            }
        }

        private void complete() {
            finished.set(true);
            latch.countDown();
        }
    }
}
