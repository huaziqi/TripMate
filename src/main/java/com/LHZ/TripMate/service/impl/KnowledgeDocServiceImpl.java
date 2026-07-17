package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.KnowledgeDocItemDTO;
import com.LHZ.TripMate.dto.admin.SaveKnowledgeDocDTO;
import com.LHZ.TripMate.entity.KnowledgeDoc;
import com.LHZ.TripMate.repository.KnowledgeDocRepository;
import com.LHZ.TripMate.service.KnowledgeDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocServiceImpl implements KnowledgeDocService {

    private final KnowledgeDocRepository repository;

    @Override
    public List<KnowledgeDocItemDTO> list(String spotKey, KnowledgeDoc.Category category, String keyword) {
        return repository.search(spotKey, category, keyword).stream()
                .map(KnowledgeDocItemDTO::from)
                .toList();
    }

    @Override
    public KnowledgeDoc get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识文档不存在: " + id));
    }

    @Override
    public KnowledgeDoc create(SaveKnowledgeDocDTO dto) {
        KnowledgeDoc doc = new KnowledgeDoc();
        applyDto(doc, dto);
        return repository.save(doc);
    }

    @Override
    public KnowledgeDoc update(Long id, SaveKnowledgeDocDTO dto) {
        KnowledgeDoc doc = get(id);
        applyDto(doc, dto);
        return repository.save(doc);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("知识文档不存在: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    public KnowledgeDoc importFile(MultipartFile file, String spotKey,
                                   KnowledgeDoc.Category category, String title) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String content = extractText(file, filename);
        if (content.isBlank()) {
            throw new IllegalArgumentException("未能从文件中提取到文本内容");
        }

        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setSpotKey(normalizeSpotKey(spotKey));
        doc.setTitle(title != null && !title.isBlank() ? title.trim() : stripExtension(filename));
        doc.setCategory(category == null ? KnowledgeDoc.Category.OTHER : category);
        doc.setContent(content);
        doc.setSourceFileName(filename);
        doc.setEnabled(true);
        return repository.save(doc);
    }

    private void applyDto(KnowledgeDoc doc, SaveKnowledgeDocDTO dto) {
        doc.setSpotKey(normalizeSpotKey(dto.getSpotKey()));
        doc.setTitle(dto.getTitle().trim());
        doc.setCategory(dto.getCategory());
        doc.setContent(dto.getContent());
        doc.setEnabled(dto.isEnabled());
    }

    private String normalizeSpotKey(String spotKey) {
        return (spotKey == null || spotKey.isBlank()) ? null : spotKey.trim();
    }

    /** 按扩展名提取纯文本：docx 用 POI，txt/md 按 UTF-8 读取 */
    static String extractText(MultipartFile file, String filename) {
        String ext = extension(filename);
        try {
            switch (ext) {
                case "docx" -> {
                    try (InputStream in = file.getInputStream();
                         XWPFDocument document = new XWPFDocument(in);
                         XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                        return cleanup(extractor.getText());
                    }
                }
                case "txt", "md" -> {
                    return cleanup(new String(file.getBytes(), StandardCharsets.UTF_8));
                }
                default -> throw new IllegalArgumentException("不支持的文件类型 ." + ext + "，仅支持 .docx / .txt / .md");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析知识文档失败: {}", filename, e);
            throw new IllegalArgumentException("文件解析失败，请确认文件未损坏");
        }
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot <= 0 ? filename : filename.substring(0, dot);
    }

    /** 去掉多余空白行，压缩连续空行为一个 */
    private static String cleanup(String text) {
        return text.replace("\r\n", "\n")
                .replaceAll("[ \t ]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }
}
