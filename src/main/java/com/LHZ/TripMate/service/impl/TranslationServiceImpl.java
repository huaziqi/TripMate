package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.TranslationResponseDTO;
import com.LHZ.TripMate.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class TranslationServiceImpl implements TranslationService {

    private static final int CACHE_MAX = 500;

    private final RestClient restClient;

    private final Map<String, TranslationResponseDTO> cache = Collections.synchronizedMap(
        new LinkedHashMap<>(CACHE_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TranslationResponseDTO> eldest) {
                return size() > CACHE_MAX;
            }
        }
    );

    public TranslationServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.mymemory.translated.net")
                .requestFactory(factory)
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public TranslationResponseDTO translate(String text, String from, String to) {
        String cacheKey = from + "|" + to + "|" + text;
        TranslationResponseDTO cached = cache.get(cacheKey);
        if (cached != null) {
            log.debug("翻译命中缓存");
            return cached;
        }

        // MyMemory 不支持 auto 检测，默认以中文为源语言
        String sourceLang = "auto".equals(from) ? "zh-CN" : from;
        String langPair = sourceLang + "|" + to;

        Map<?, ?> root = restClient.get()
                .uri(b -> b.path("/get")
                           .queryParam("q", text)
                           .queryParam("langpair", langPair)
                           .build())
                .retrieve()
                .body(Map.class);

        if (root == null) {
            throw new RuntimeException("翻译接口无响应");
        }

        Object statusObj = root.get("responseStatus");
        int status = statusObj instanceof Number n ? n.intValue() : -1;
        if (status != 200) {
            throw new RuntimeException("翻译接口返回错误: " + status);
        }

        Map<?, ?> responseData = (Map<?, ?>) root.get("responseData");
        if (responseData == null) {
            throw new RuntimeException("翻译结果格式错误");
        }

        String translated = (String) responseData.get("translatedText");
        if (translated == null || translated.isBlank()
                || translated.toUpperCase().contains("QUERY LENGTH LIMIT")) {
            throw new RuntimeException("翻译配额已用尽，请稍后再试");
        }

        TranslationResponseDTO result = TranslationResponseDTO.builder()
                .translatedText(translated.trim())
                .detectedLang(sourceLang)
                .from(from)
                .to(to)
                .build();

        cache.put(cacheKey, result);
        return result;
    }
}
