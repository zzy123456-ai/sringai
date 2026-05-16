package com.springai.entity;

/**
 * 文档分块实体。
 * 将 Word/Excel 文档按 500 字符切分后的一个文本片段。
 */
public class TextChunk {
    private String chunkId;      // 分块唯一标识：文件路径#序号
    private String filePath;     // 来源文件绝对路径
    private String fileName;     // 来源文件名（展示用）
    private String content;      // 分块的文本内容

    public TextChunk() {}

    public TextChunk(String chunkId, String filePath, String fileName, String content) {
        this.chunkId = chunkId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.content = content;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
