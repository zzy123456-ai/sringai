package com.springai.service;

import com.springai.entity.RetrievedChunk;

import java.util.List;

/**
 * RAG 检索增强生成服务。
 * 检索：对分组内文档做关键词匹配打分，返回 top-K 片段。
 * 生成：将检索到的片段作为上下文，调用 LLM 生成答案。
 */
public interface RagService {
    /** 纯检索：返回相关文档片段，不调用 LLM */
    List<RetrievedChunk> search(String groupId, String query, int topK);
    /** 增强生成：检索 + LLM 生成回答，含来源引用 */
    String generate(String groupId, String query);
}
