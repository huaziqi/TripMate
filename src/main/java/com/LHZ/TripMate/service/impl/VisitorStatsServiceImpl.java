package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.VisitorImportResultDTO;
import com.LHZ.TripMate.entity.VisitorDailyStat;
import com.LHZ.TripMate.entity.VisitorDimStat;
import com.LHZ.TripMate.repository.VisitorDailyStatRepository;
import com.LHZ.TripMate.repository.VisitorDimStatRepository;
import com.LHZ.TripMate.service.VisitorStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 流式解析游客行为 xlsx（14 万行级），只落库聚合结果：
 * 按日统计（人次/满意度/消费）+ 四个维度分布。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorStatsServiceImpl implements VisitorStatsService {

    private final VisitorDailyStatRepository dailyRepo;
    private final VisitorDimStatRepository dimRepo;

    @Override
    @Transactional
    public VisitorImportResultDTO importXlsx(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("仅支持 .xlsx 格式");
        }

        VisitorSheetHandler handler = new VisitorSheetHandler();
        try (InputStream in = file.getInputStream();
             OPCPackage pkg = OPCPackage.open(in)) {

            XSSFReader reader = new XSSFReader(pkg);
            XMLReader parser = XMLHelper.newXMLReader();
            parser.setContentHandler(new XSSFSheetXMLHandler(
                    reader.getStylesTable(), reader.getSharedStringsTable(),
                    handler, new DataFormatter(), false));

            try (InputStream sheet = reader.getSheetsData().next()) {
                parser.parse(new InputSource(sheet));
            }
        } catch (Exception e) {
            log.error("解析游客行为数据失败: {}", filename, e);
            throw new IllegalArgumentException("文件解析失败，请确认是有效的行为分析数据 xlsx");
        }

        if (handler.daily.isEmpty()) {
            throw new IllegalArgumentException("未解析到有效数据行（需包含 visit_date / satisfaction 等列）");
        }

        // 覆盖式落库
        dailyRepo.deleteAllInBatch();
        dimRepo.deleteAllInBatch();

        List<VisitorDailyStat> dailyStats = handler.daily.entrySet().stream().map(e -> {
            VisitorDailyStat s = new VisitorDailyStat();
            s.setStatDate(e.getKey());
            s.setVisitCount(e.getValue().visits);
            s.setSatisfactionSum(e.getValue().satSum);
            s.setSpendSum(e.getValue().spendSum);
            return s;
        }).toList();
        dailyRepo.saveAll(dailyStats);

        List<VisitorDimStat> dimStats = new ArrayList<>();
        handler.dims.forEach((dimension, counter) -> counter.forEach((label, cnt) -> {
            VisitorDimStat s = new VisitorDimStat();
            s.setDimension(dimension);
            s.setLabel(label);
            s.setCount(cnt);
            dimStats.add(s);
        }));
        dimRepo.saveAll(dimStats);

        LocalDate min = Collections.min(handler.daily.keySet());
        LocalDate max = Collections.max(handler.daily.keySet());
        log.info("游客行为数据导入完成：{} 行有效，{} 行跳过，日期 {} ~ {}",
                handler.validRows, handler.skippedRows, min, max);
        return new VisitorImportResultDTO(handler.validRows, handler.skippedRows,
                min.toString(), max.toString());
    }

    // ------------------------------------------------------------------
    // SAX 行处理器
    // ------------------------------------------------------------------

    private static class DayAgg {
        long visits;
        double satSum;
        double spendSum;
    }

    private static class VisitorSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        final Map<LocalDate, DayAgg> daily = new HashMap<>();
        final Map<VisitorDimStat.Dimension, Map<String, Long>> dims = new EnumMap<>(VisitorDimStat.Dimension.class);
        long validRows = 0, skippedRows = 0;

        /** 列下标 → 列名（由表头行建立） */
        private final Map<Integer, String> headerByCol = new HashMap<>();
        /** 当前行：列名 → 单元格文本 */
        private final Map<String, String> row = new HashMap<>();
        private int currentRow = -1;

        VisitorSheetHandler() {
            for (VisitorDimStat.Dimension d : VisitorDimStat.Dimension.values()) {
                dims.put(d, new HashMap<>());
            }
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = rowNum;
            row.clear();
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null || formattedValue == null) return;
            int col = new org.apache.poi.ss.util.CellReference(cellReference).getCol();
            if (currentRow == 0) {
                headerByCol.put(col, formattedValue.strip());
            } else {
                String header = headerByCol.get(col);
                if (header != null) row.put(header, formattedValue.strip());
            }
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) return;
            LocalDate date = parseDate(row.get("visit_date"));
            Double satisfaction = parseDouble(row.get("satisfaction"));
            if (date == null || satisfaction == null) {
                skippedRows++;
                return;
            }
            double spend = Optional.ofNullable(parseDouble(row.get("total_cost"))).orElse(0.0);

            DayAgg agg = daily.computeIfAbsent(date, d -> new DayAgg());
            agg.visits++;
            agg.satSum += satisfaction;
            agg.spendSum += spend;

            bump(VisitorDimStat.Dimension.SATISFACTION, Math.round(satisfaction) + "星");
            bumpIfPresent(VisitorDimStat.Dimension.ATTRACTION_TYPE, row.get("attraction_type"));
            bumpIfPresent(VisitorDimStat.Dimension.GENDER, row.get("gender"));
            Double age = parseDouble(row.get("age"));
            if (age != null) bump(VisitorDimStat.Dimension.AGE_GROUP, ageGroup(age.intValue()));

            validRows++;
        }

        private void bumpIfPresent(VisitorDimStat.Dimension dim, String label) {
            if (label != null && !label.isBlank()) bump(dim, label);
        }

        private void bump(VisitorDimStat.Dimension dim, String label) {
            dims.get(dim).merge(label, 1L, Long::sum);
        }

        private static String ageGroup(int age) {
            if (age < 18) return "18岁以下";
            if (age <= 30) return "18-30岁";
            if (age <= 45) return "31-45岁";
            if (age <= 60) return "46-60岁";
            return "60岁以上";
        }

        /**
         * visit_date 可能是 Excel 序列号（如 45931），也可能被单元格样式
         * 格式化成日期字符串：yyyy-M-d、m/d/yyyy 或 m/d/yy（内置格式14）。
         */
        private static LocalDate parseDate(String value) {
            if (value == null || value.isBlank()) return null;
            try {
                String v = value.strip();
                if (v.contains("-") || v.contains("/")) {
                    String[] p = v.split("[-/]");
                    if (p.length != 3) return null;
                    int a = Integer.parseInt(p[0].trim());
                    int b = Integer.parseInt(p[1].trim());
                    int c = Integer.parseInt(p[2].trim());
                    if (p[0].trim().length() == 4) return LocalDate.of(a, b, c);   // yyyy-M-d
                    int year = (p[2].trim().length() == 4) ? c : 2000 + c;          // m/d/yyyy | m/d/yy
                    return LocalDate.of(year, a, b);
                }
                double serial = Double.parseDouble(v);
                return DateUtil.getJavaDate(serial).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
                return null;
            }
        }

        private static Double parseDouble(String value) {
            if (value == null || value.isBlank()) return null;
            try {
                return Double.parseDouble(value.replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
