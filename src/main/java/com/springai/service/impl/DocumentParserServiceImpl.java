package com.springai.service.impl;

import com.springai.entity.TextChunk;
import com.springai.service.DocumentParserService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析实现，基于 Apache POI。
 * 支持 .docx（按段落提取）和 .xlsx（按行提取，多 Sheet 附带 Sheet 名）。
 */
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    /** 根据文件扩展名自动选择解析方式 */
    @Override
    public String parseDocument(String filePath) {
        String lower = filePath.toLowerCase();
        try {
            if (lower.endsWith(".docx")) {
                return parseWord(filePath);
            } else if (lower.endsWith(".xlsx")) {
                return parseExcel(filePath);
            } else {
                throw new IllegalArgumentException("不支持的文件类型: " + filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("解析文档失败: " + filePath, e);
        }
    }

    /**
     * 将文档全文按指定大小分块。
     * 以换行符分隔段落，逐段累积进 buffer，超过 maxChunkSize 时切出一个新块。
     * 这样能保持段落完整性，不会把一句话从中间切断。
     */
    @Override
    public List<TextChunk> chunkDocument(String filePath, int maxChunkSize) {
        String fullText = parseDocument(filePath);
        String fileName = Paths.get(filePath).getFileName().toString();
        List<TextChunk> chunks = new ArrayList<>();

        String[] paragraphs = fullText.split("\n");
        StringBuilder buffer = new StringBuilder();
        int chunkIdx = 0;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 当前 buffer 加上新段落会超出限制 → 切块
            if (buffer.length() + trimmed.length() > maxChunkSize && buffer.length() > 0) {
                chunks.add(new TextChunk(
                        filePath + "#" + chunkIdx,
                        filePath,
                        fileName,
                        buffer.toString().trim()
                ));
                chunkIdx++;
                buffer.setLength(0);
            }
            if (buffer.length() > 0) {
                buffer.append("\n");
            }
            buffer.append(trimmed);
        }

        // 最后剩余的文本作为一块
        if (buffer.length() > 0) {
            chunks.add(new TextChunk(
                    filePath + "#" + chunkIdx,
                    filePath,
                    fileName,
                    buffer.toString().trim()
            ));
        }

        return chunks;
    }

    /** 解析 .docx：逐段落读取非空文本 */
    private String parseWord(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText().trim();
                if (!text.isEmpty()) {
                    sb.append(text).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /** 解析 .xlsx：逐 Sheet → 逐行 → 逐单元格，多 Sheet 时注入 Sheet 名作为分隔标识 */
    private String parseExcel(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                var sheet = wb.getSheetAt(i);
                if (wb.getNumberOfSheets() > 1) {
                    sb.append("【").append(sheet.getSheetName()).append("】\n");
                }
                for (var row : sheet) {
                    List<String> cellValues = new ArrayList<>();
                    for (var cell : row) {
                        String val = getCellString(cell);
                        if (!val.isEmpty()) {
                            cellValues.add(val);
                        }
                    }
                    if (!cellValues.isEmpty()) {
                        // 同一行的单元格用两个空格分隔，形成一行可读文本
                        sb.append(String.join("  ", cellValues)).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    /** 将单元格值转为字符串。整数数值避免显示小数点（如 2024 而非 2024.0） */
    private String getCellString(org.apache.poi.ss.usermodel.Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
