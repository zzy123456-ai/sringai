package com.springai.entity;

/**
 * 检索结果实体。
 * 检索后返回的文档片段，附带来源文件名和相关度评分。
 */
public class RetrievedChunk {
    private String chunkId;      // 分块唯一标识
    private String fileName;     // 来源文件名
    private String content;      // 片段内容
    private double score;        // 与查询关键词的相关度评分（0~1）

    public RetrievedChunk() {}

    public RetrievedChunk(String chunkId, String fileName, String content, double score) {
        this.chunkId = chunkId;
        this.fileName = fileName;
        this.content = content;
        this.score = score;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
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

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
