package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.SaveKnowledgeSpotEntryDTO;
import com.LHZ.TripMate.dto.admin.SpotImportResultDTO;
import com.LHZ.TripMate.entity.KnowledgeSpotEntry;
import com.LHZ.TripMate.repository.KnowledgeSpotEntryRepository;
import com.LHZ.TripMate.service.KnowledgeSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSpotServiceImpl implements KnowledgeSpotService {

    private final KnowledgeSpotEntryRepository repository;

    /**
     * 表头列名 → 实体字段写入器；列名去空格后精确匹配。
     * 同一字段可能有多种写法（数据集"字段规范"与实际表头不一致），全部列出。
     */
    private static final Map<String, BiConsumer<KnowledgeSpotEntry, String>> COLUMN_SETTERS = Map.ofEntries(
            Map.entry("景区名称", KnowledgeSpotEntry::setZoneName),
            Map.entry("景点ID", KnowledgeSpotEntry::setSpotCode),
            Map.entry("景点名称", KnowledgeSpotEntry::setName),
            Map.entry("具体位置", KnowledgeSpotEntry::setLocation),
            Map.entry("建筑/景观参数", KnowledgeSpotEntry::setScaleInfo),
            Map.entry("规模/景观数据", KnowledgeSpotEntry::setScaleInfo),
            Map.entry("核心功能", KnowledgeSpotEntry::setCoreFunction),
            Map.entry("文化内涵", KnowledgeSpotEntry::setCulture),
            Map.entry("详细介绍", KnowledgeSpotEntry::setDescription),
            Map.entry("游玩亮点", KnowledgeSpotEntry::setTourTips),
            Map.entry("游览要点", KnowledgeSpotEntry::setTourTips),
            Map.entry("演艺/开放信息", KnowledgeSpotEntry::setTicketInfo),
            Map.entry("门票/开放信息", KnowledgeSpotEntry::setTicketInfo),
            Map.entry("备注", KnowledgeSpotEntry::setRemark));

    @Override
    public List<KnowledgeSpotEntry> list(String spotKey, String zoneName, String keyword) {
        return repository.search(spotKey, zoneName, keyword);
    }

    @Override
    public List<String> zoneNames() {
        return repository.findDistinctZoneNames();
    }

    @Override
    public KnowledgeSpotEntry get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("景点知识条目不存在: " + id));
    }

    @Override
    public KnowledgeSpotEntry create(SaveKnowledgeSpotEntryDTO dto) {
        repository.findBySpotKeyAndSpotCode(dto.getSpotKey().trim(), dto.getSpotCode().trim())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("景点ID已存在: " + dto.getSpotCode());
                });
        KnowledgeSpotEntry entry = new KnowledgeSpotEntry();
        applyDto(entry, dto);
        return repository.save(entry);
    }

    @Override
    public KnowledgeSpotEntry update(Long id, SaveKnowledgeSpotEntryDTO dto) {
        KnowledgeSpotEntry entry = get(id);
        repository.findBySpotKeyAndSpotCode(dto.getSpotKey().trim(), dto.getSpotCode().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(e -> {
                    throw new IllegalArgumentException("景点ID已被其他条目占用: " + dto.getSpotCode());
                });
        applyDto(entry, dto);
        return repository.save(entry);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("景点知识条目不存在: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public SpotImportResultDTO importDocx(MultipartFile file, String spotKey) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (spotKey == null || spotKey.isBlank()) {
            throw new IllegalArgumentException("请先指定所属景区 spotKey");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!filename.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("仅支持 .docx 格式的结构化数据集");
        }
        spotKey = spotKey.trim();

        int created = 0, updated = 0, skipped = 0;
        try (InputStream in = file.getInputStream();
             XWPFDocument document = new XWPFDocument(in)) {

            for (XWPFTable table : document.getTables()) {
                List<XWPFTableRow> rows = table.getRows();
                if (rows.size() < 2) continue;

                Map<Integer, BiConsumer<KnowledgeSpotEntry, String>> colMap = mapHeader(rows.get(0));
                if (colMap.isEmpty()) {
                    skipped += rows.size() - 1;
                    continue;
                }

                for (int r = 1; r < rows.size(); r++) {
                    KnowledgeSpotEntry parsed = new KnowledgeSpotEntry();
                    List<XWPFTableCell> cells = rows.get(r).getTableCells();
                    for (Map.Entry<Integer, BiConsumer<KnowledgeSpotEntry, String>> col : colMap.entrySet()) {
                        if (col.getKey() < cells.size()) {
                            String text = cells.get(col.getKey()).getText().strip();
                            col.getValue().accept(parsed, text.isEmpty() ? null : text);
                        }
                    }
                    if (parsed.getSpotCode() == null || parsed.getName() == null) {
                        skipped++;
                        continue;
                    }

                    KnowledgeSpotEntry target = repository
                            .findBySpotKeyAndSpotCode(spotKey, parsed.getSpotCode())
                            .orElse(null);
                    if (target == null) {
                        parsed.setSpotKey(spotKey);
                        parsed.setEnabled(true);
                        repository.save(parsed);
                        created++;
                    } else {
                        copyFields(parsed, target);
                        repository.save(target);
                        updated++;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析结构化数据集失败: {}", filename, e);
            throw new IllegalArgumentException("文件解析失败，请确认是有效的 docx 结构化数据集");
        }

        if (created == 0 && updated == 0) {
            throw new IllegalArgumentException("未在文档中找到可识别的景点表格（需包含'景点ID'和'景点名称'列）");
        }
        return new SpotImportResultDTO(created, updated, skipped);
    }

    /** 解析表头行：列下标 → 字段写入器。识别不出"景点ID"列则视为非数据表 */
    private Map<Integer, BiConsumer<KnowledgeSpotEntry, String>> mapHeader(XWPFTableRow headerRow) {
        Map<Integer, BiConsumer<KnowledgeSpotEntry, String>> colMap = new HashMap<>();
        boolean hasSpotCode = false;
        List<XWPFTableCell> cells = headerRow.getTableCells();
        for (int i = 0; i < cells.size(); i++) {
            String header = cells.get(i).getText().replaceAll("\\s", "");
            BiConsumer<KnowledgeSpotEntry, String> setter = COLUMN_SETTERS.get(header);
            if (setter != null) {
                colMap.put(i, setter);
                if ("景点ID".equals(header)) hasSpotCode = true;
            }
        }
        return hasSpotCode ? colMap : Map.of();
    }

    /** 导入更新时覆盖内容字段，保留 enabled 等管理状态 */
    private void copyFields(KnowledgeSpotEntry from, KnowledgeSpotEntry to) {
        to.setZoneName(from.getZoneName());
        to.setName(from.getName());
        to.setLocation(from.getLocation());
        to.setScaleInfo(from.getScaleInfo());
        to.setCoreFunction(from.getCoreFunction());
        to.setCulture(from.getCulture());
        to.setDescription(from.getDescription());
        to.setTourTips(from.getTourTips());
        to.setTicketInfo(from.getTicketInfo());
        to.setRemark(from.getRemark());
    }

    private void applyDto(KnowledgeSpotEntry entry, SaveKnowledgeSpotEntryDTO dto) {
        entry.setSpotKey(dto.getSpotKey().trim());
        entry.setSpotCode(dto.getSpotCode().trim());
        entry.setZoneName(blankToNull(dto.getZoneName()));
        entry.setName(dto.getName().trim());
        entry.setLocation(blankToNull(dto.getLocation()));
        entry.setScaleInfo(blankToNull(dto.getScaleInfo()));
        entry.setCoreFunction(blankToNull(dto.getCoreFunction()));
        entry.setCulture(blankToNull(dto.getCulture()));
        entry.setDescription(blankToNull(dto.getDescription()));
        entry.setTourTips(blankToNull(dto.getTourTips()));
        entry.setTicketInfo(blankToNull(dto.getTicketInfo()));
        entry.setRemark(blankToNull(dto.getRemark()));
        entry.setEnabled(dto.isEnabled());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
