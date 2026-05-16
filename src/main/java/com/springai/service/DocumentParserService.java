package com.springai.service;

import com.springai.entity.TextChunk;

import java.util.List;

/**
 * 文档解析服务。
 * 支持 Word (.docx) 和 Excel (.xlsx) 的全文解析与分块。
 */
public interface DocumentParserService {
    /** 解析文档全文，自动识别 .docx / .xlsx */
    String parseDocument(String filePath);
    /** 将文档按指定大小分块，保持段落完整性 */
    List<TextChunk> chunkDocument(String filePath, int maxChunkSize);
}
