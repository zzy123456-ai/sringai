package com.springai.service.impl;

import com.springai.entity.KnowledgeGroup;
import com.springai.entity.RetrievedChunk;
import com.springai.entity.TextChunk;
import com.springai.service.DocumentParserService;
import com.springai.service.KnowledgeGroupService;
import com.springai.service.RagService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 检索增强生成实现。
 * 检索：关键词匹配打分（中文词组 + 精确子串），不依赖向量库。
 * 生成：将 top-K 片段拼入 SystemPrompt，由 ChatClient 调用 DeepSeek 回答。
 */
@Service
public class RagServiceImpl implements RagService {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_TOP_K = 3;

    @Resource
    private KnowledgeGroupService knowledgeGroupService;

    @Resource
    private DocumentParserService documentParserService;

    @Resource
    private ChatClient chatClient;

    /**
     * 纯检索：遍历分组内所有文件，分块后逐块计算相似度，返回得分最高的 topK 个片段。
     */
    @Override
    public List<RetrievedChunk> search(String groupId, String query, int topK) {
        KnowledgeGroup group = knowledgeGroupService.getGroup(groupId);
        List<RetrievedChunk> allScored = new ArrayList<>();

        for (String filePath : group.getFilePaths()) {
            List<TextChunk> chunks = documentParserService.chunkDocument(filePath, DEFAULT_CHUNK_SIZE);
            for (TextChunk chunk : chunks) {
                double score = computeSimilarity(query, chunk.getContent());
                if (score > 0) {
                    allScored.add(new RetrievedChunk(
                            chunk.getChunkId(),
                            chunk.getFileName(),
                            chunk.getContent(),
                            score
                    ));
                }
            }
        }

        allScored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int k = Math.min(topK > 0 ? topK : DEFAULT_TOP_K, allScored.size());
        return allScored.subList(0, k);
    }

    /**
     * 增强生成：检索 top-K 片段作为上下文，拼接 SystemPrompt，调用 LLM 生成答案。
     * SystemPrompt 约束 AI 严格依据文档回答，不得编造，并引用来源文件名。
     */
    @Override
    public String generate(String groupId, String query) {
        KnowledgeGroup group = knowledgeGroupService.getGroup(groupId);
        List<RetrievedChunk> retrieved = search(groupId, query, DEFAULT_TOP_K);

        // 构建上下文：将检索到的片段按来源编号
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < retrieved.size(); i++) {
            RetrievedChunk rc = retrieved.get(i);
            context.append("【来源").append(i + 1).append("：").append(rc.getFileName()).append("】\n");
            context.append(rc.getContent()).append("\n\n");
        }

        String systemPrompt = buildSystemPrompt(group, context.toString());
        String userPrompt = query.isEmpty() ? group.getQueryHint() : query;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    /** 构造 SystemPrompt：注入查询目标 + 检索到的文档上下文 + 行为约束 */
    private String buildSystemPrompt(KnowledgeGroup group, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个知识库智能助手。").append(group.getQueryHint()).append("\n");
        sb.append("请严格根据以下参考文档的内容回答用户问题。\n");
        sb.append("如果参考文档中没有相关信息，请明确说明'当前知识库中没有找到相关信息'。\n");
        sb.append("回答时请引用来源文件名。\n\n");
        sb.append("参考文档内容：\n");
        sb.append(context);
        return sb.toString();
    }

    /**
     * 计算查询与文本块的相关度评分（0~1）。
     * 综合两种策略：
     *   1. Token 命中率（70%）：提取 query 中的中文词组和英文单词，计算在 text 中的出现比例
     *   2. 精确子串匹配（30%）：query 完整出现在 text 中额外加分
     */
    double computeSimilarity(String query, String text) {
        if (query == null || text == null || query.isBlank() || text.isBlank()) {
            return 0;
        }

        String q = query.toLowerCase();
        String t = text.toLowerCase();

        double exactBonus = 0;
        if (t.contains(q)) {
            exactBonus = 1.0;
        }

        List<String> qTokens = tokenize(q);

        if (qTokens.isEmpty()) {
            return exactBonus;
        }

        int hitCount = 0;
        for (String token : qTokens) {
            if (t.contains(token)) {
                hitCount++;
            }
        }
        double tokenScore = (double) hitCount / qTokens.size();

        return tokenScore * 0.7 + exactBonus * 0.3;
    }

    /**
     * 分词：提取中文 2-4 字词组和英文/数字 token（长度 ≥ 2）。
     * 不使用外部分词器，纯正则提取，适合中文关键词匹配场景。
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Pattern chinesePattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
        Matcher m = chinesePattern.matcher(text);
        while (m.find()) {
            tokens.add(m.group());
        }
        Pattern wordPattern = Pattern.compile("[a-zA-Z0-9]{2,}");
        Matcher wm = wordPattern.matcher(text);
        while (wm.find()) {
            tokens.add(wm.group().toLowerCase());
        }
        return tokens;
    }
}
