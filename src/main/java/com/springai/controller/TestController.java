package com.springai.controller;

import com.alibaba.fastjson.JSONObject;
import com.springai.common.config.AgentRouter;
import com.springai.entity.KnowledgeGroup;
import com.springai.entity.RetrievedChunk;
import com.springai.service.CategoryToolService;
import com.springai.service.KnowledgeGroupService;
import com.springai.service.RagService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RequestMapping("/ai")
@RestController
public class TestController {

    @Resource
    private ChatClient chatClient;

    @Resource
    private CategoryToolService categoryToolService;

    @Resource
    private KnowledgeGroupService knowledgeGroupService;

    @Resource
    private AgentRouter agentRouter;

    @Resource
    private RagService ragService;

    // ==================== 基础对话 ====================

    @PostMapping("/test")
    public Map<String, String> test(@RequestBody JSONObject param) {
        String message = param.getString("message");
        String answer = chatClient.prompt(message)
                .call()
                .content();

        return Map.of("message", message, "answer", answer);
    }

    @GetMapping("/db-chat")
    public Map<String, String> dbChat(
            @RequestParam(defaultValue = "有哪些分类？") String message) {

        String answer = chatClient.prompt()
                .system("""
                        你是一个系统业务助手。
                        当用户询问分类相关问题时，优先调用工具查询真实分类数据后再回答。
                        不要编造分类ID和分类名称。
                        如果工具没有返回数据，就明确说明当前没有分类数据。
                        """)
                .user(message)
                .tools(categoryToolService)
                .call()
                .content();

        return Map.of("question", message, "answer", answer);
    }

    // ==================== RAG 知识库分组管理 ====================

    /** 创建知识分组，入参：groupName, description, queryHint */
    @PostMapping("/rag/group")
    public KnowledgeGroup createGroup(@RequestBody JSONObject param) {
        return knowledgeGroupService.createGroup(
                param.getString("groupName"),
                param.getString("description"),
                param.getString("queryHint")
        );
    }

    /** 查看所有分组 */
    @GetMapping("/rag/groups")
    public List<KnowledgeGroup> listGroups() {
        return knowledgeGroupService.listGroups();
    }

    /** 查看单个分组详情 */
    @GetMapping("/rag/group/{groupId}")
    public KnowledgeGroup getGroup(@PathVariable String groupId) {
        return knowledgeGroupService.getGroup(groupId);
    }

    /** 删除分组 */
    @DeleteMapping("/rag/group/{groupId}")
    public String deleteGroup(@PathVariable String groupId) {
        knowledgeGroupService.deleteGroup(groupId);
        return "已删除分组: " + groupId;
    }

    /** 向分组添加文件，入参：filePath */
    @PostMapping("/rag/group/{groupId}/file")
    public String addFile(@PathVariable String groupId, @RequestBody JSONObject param) {
        knowledgeGroupService.addFileToGroup(groupId, param.getString("filePath"));
        return "已添加文件到分组";
    }

    /** 从分组移除文件，入参：filePath */
    @DeleteMapping("/rag/group/{groupId}/file")
    public String removeFile(@PathVariable String groupId, @RequestBody JSONObject param) {
        knowledgeGroupService.removeFileFromGroup(groupId, param.getString("filePath"));
        return "已移除文件";
    }

    /** 更新分组的查询目标描述，入参：queryHint */
    @PutMapping("/rag/group/{groupId}/hint")
    public String updateHint(@PathVariable String groupId, @RequestBody JSONObject param) {
        knowledgeGroupService.updateQueryHint(groupId, param.getString("queryHint"));
        return "已更新查询目标";
    }

    // ==================== RAG 检索与生成 ====================

    /** 纯检索：返回相关文档片段及评分，不调用 LLM */
    @GetMapping("/rag/search")
    public List<RetrievedChunk> search(
            @RequestParam String groupId,
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        return ragService.search(groupId, query, topK);
    }

    /** 增强生成：检索相关文档片段后，调用 LLM 生成带来源引用的答案 */
    @GetMapping("/rag/generate")
    public Map<String, String> generate(
            @RequestParam String groupId,
            @RequestParam String query) {
        String answer = ragService.generate(groupId, query);
        return Map.of("query", query, "answer", answer);
    }

    // ==================== Agent 路由 ====================

    /** 通过 AgentRouter 自动匹配合适的远程 Agent 并执行任务 */
    @GetMapping("/agent-chat")
    public Map<String, String> agentChat(
            @RequestParam(defaultValue = "帮我查询天气") String message) {

        String answer = agentRouter.execute(message);

        return Map.of("question", message, "answer", answer);
    }
}
