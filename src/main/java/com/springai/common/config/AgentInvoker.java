package com.springai.common.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.springai.pojo.AgentType;
import com.springai.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgentInvoker {

    private static final Logger log = LoggerFactory.getLogger(AgentInvoker.class);

    private final RestClient restClient;
    private final ChatClient chatClient;
    private final RagService ragService;

    public AgentInvoker(RestClient.Builder restClientBuilder,
                        ChatClient chatClient,
                        RagService ragService) {
        this.restClient = restClientBuilder.build();
        this.chatClient = chatClient;
        this.ragService = ragService;
    }

    public String invoke(String cardJson, String userTask) {
        AgentType type = extractType(cardJson);
        log.info("Invoking agent type={}, task={}", type, userTask);
        try {
            return switch (type) {
                case REMOTE_AGENT -> invokeRemoteAgent(cardJson, userTask);
                case MCP_TOOL     -> invokeMcpTool(cardJson, userTask);
                case REST_API     -> invokeRestApi(cardJson, userTask);
                case RAG_KB       -> invokeRagKb(cardJson, userTask);
            };
        } catch (Exception e) {
            log.warn("Agent invocation failed for type={}: {}", type, e.getMessage());
            return "Agent call failed (" + type + "): " + e.getMessage();
        }
    }

    // ==================== 类型提取 ====================

    AgentType extractType(String cardJson) {
        JSONObject card = JSON.parseObject(cardJson);
        String typeStr = card.getString("type");
        if (typeStr == null || typeStr.isEmpty()) {
            return AgentType.REMOTE_AGENT; // 兼容旧数据
        }
        try {
            return AgentType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown agent type '{}', fallback to REMOTE_AGENT", typeStr);
            return AgentType.REMOTE_AGENT;
        }
    }

    // ==================== 远程 Agent ====================

    private String invokeRemoteAgent(String cardJson, String userTask) {
        JSONObject card = JSON.parseObject(cardJson);
        String url = card.getString("url");
        if (url == null || url.isEmpty()) {
            return "Agent URL not configured";
        }
        return restClient.post()
                .uri(url)
                .body(userTask)
                .retrieve()
                .body(String.class);
    }

    // ==================== MCP 工具 ====================

    private String invokeMcpTool(String cardJson, String userTask) {
        JSONObject card = JSON.parseObject(cardJson);
        String mcpServerUrl = card.getString("mcpServerUrl");
        String toolName = card.getString("toolName");

        if (mcpServerUrl == null || mcpServerUrl.isEmpty()) {
            return "MCP server URL not configured";
        }

        // 用 LLM 从 userTask 中提取参数
        Map<String, Object> arguments = extractArgumentsWithLLM(cardJson, userTask);

        // 构建 MCP JSON-RPC tools/call 请求
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/call");
        request.put("id", UUID.randomUUID().toString());
        JSONObject params = new JSONObject();
        params.put("name", toolName);
        params.put("arguments", arguments);
        request.put("params", params);

        log.info("MCP call: {} -> {}", mcpServerUrl, request.toJSONString());
        return restClient.post()
                .uri(mcpServerUrl)
                .body(request.toJSONString())
                .retrieve()
                .body(String.class);
    }

    private Map<String, Object> extractArgumentsWithLLM(String cardJson, String userTask) {
        JSONObject card = JSON.parseObject(cardJson);
        JSONObject inputSchema = card.getJSONObject("inputSchema");
        String toolName = card.getString("toolName");
        String description = card.getString("description");

        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据用户输入，提取调用工具所需的参数。\n");
        prompt.append("工具名称: ").append(toolName).append("\n");
        prompt.append("工具描述: ").append(description).append("\n");
        if (inputSchema != null) {
            prompt.append("参数定义: ").append(inputSchema.toJSONString()).append("\n");
        }
        prompt.append("用户输入: ").append(userTask).append("\n\n");
        prompt.append("请返回纯JSON对象，包含提取的参数，如 {\"city\": \"北京\"}。无参数时返回 {}。");

        String llmResponse = chatClient.prompt()
                .user(prompt.toString())
                .call()
                .content();
        log.info("LLM extracted arguments: {}", llmResponse);

        String json = extractJson(llmResponse);
        return JSON.parseObject(json);
    }

    // ==================== REST API ====================

    private String invokeRestApi(String cardJson, String userTask) {
        JSONObject card = JSON.parseObject(cardJson);
        String url = card.getString("url");
        String method = card.getString("method");
        if (method == null) method = "GET";

        // 用 LLM 提取路径参数和查询参数
        Map<String, Object> params = extractArgumentsWithLLM(cardJson, userTask);

        // 替换路径参数 {param}
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                url = url.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }

        log.info("REST call: {} {}", method, url);
        if ("GET".equalsIgnoreCase(method)) {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } else {
            return restClient.post()
                    .uri(url)
                    .body(userTask)
                    .retrieve()
                    .body(String.class);
        }
    }

    // ==================== RAG 知识库 ====================

    private String invokeRagKb(String cardJson, String userTask) {
        JSONObject card = JSON.parseObject(cardJson);
        String groupId = card.getString("groupId");

        if (groupId == null || groupId.isEmpty()) {
            return "RAG group ID not configured";
        }

        log.info("RAG query: groupId={}, query={}", groupId, userTask);
        try {
            return ragService.generate(groupId, userTask);
        } catch (Exception e) {
            return "RAG query failed: " + e.getMessage();
        }
    }

    // ==================== JSON 提取 ====================

    private String extractJson(String text) {
        Pattern p = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
