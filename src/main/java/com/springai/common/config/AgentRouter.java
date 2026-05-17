package com.springai.common.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.springai.pojo.RouteDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgentRouter {

    private static final Logger log = LoggerFactory.getLogger(AgentRouter.class);

    private final DiscoveryClient discoveryClient;
    private final ChatClient chatClient;
    private final AgentInvoker agentInvoker;

    @Value("${a2a.router.llm.enabled:true}")
    private boolean llmEnabled;

    public AgentRouter(DiscoveryClient discoveryClient,
                       ChatClient chatClient,
                       AgentInvoker agentInvoker) {
        this.discoveryClient = discoveryClient;
        this.chatClient = chatClient;
        this.agentInvoker = agentInvoker;
    }

    public String execute(String userTask) {
        List<String> cards = discoveryClient.getAllAgents();
        if (cards.isEmpty()) {
            return "No agents available. Please register an agent first.";
        }

        log.info("Matching task to {} available agents: {}", cards.size(), userTask);
        RouteDecision decision = matchAgent(userTask, cards);

        log.info("Routing to agent={}, params={}",
                decision.getAgentName(), decision.getParams());

        return invokeAgent(decision, userTask);
    }

    private RouteDecision matchAgent(String task, List<String> cards) {
        if (llmEnabled) {
            return matchWithLLM(task, cards);
        }
        return matchWithRules(task, cards);
    }

    // ==================== LLM 匹配策略 ====================

    private RouteDecision matchWithLLM(String task, List<String> cards) {
        String prompt = buildPrompt(task, cards);
        String llmResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        log.info("LLM routing response: {}", llmResponse);
        return parseDecision(llmResponse);
    }

    String buildPrompt(String task, List<String> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是一个智能路由器。请根据用户任务，从可用 Agent 列表中选择最合适的一个并提取参数。

                可用 Agent 列表：
                """);

        for (String cardJson : cards) {
            JSONObject card = JSON.parseObject(cardJson);
            sb.append("- name: ").append(card.getString("name")).append("\n");
            sb.append("  description: ").append(card.getString("description")).append("\n");

            JSONArray skills = card.getJSONArray("skills");
            if (skills != null && !skills.isEmpty()) {
                sb.append("  skills:\n");
                for (int i = 0; i < skills.size(); i++) {
                    JSONObject skill = skills.getJSONObject(i);
                    sb.append("    - id: ").append(skill.getString("id"))
                            .append(", description: ").append(skill.getString("description"))
                            .append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("用户任务: ").append(task).append("\n\n");
        sb.append("""
                请返回纯JSON（不要包含```json标记）：
                {"agent":"AgentName","params":{"key":"value"}}""");

        return sb.toString();
    }

    RouteDecision parseDecision(String llmResponse) {
        String json = extractJson(llmResponse);
        JSONObject obj = JSON.parseObject(json);

        RouteDecision decision = new RouteDecision();
        decision.setAgentName(obj.getString("agent"));
        decision.setSkillId(obj.getString("skill_id"));

        JSONObject params = obj.getJSONObject("params");
        if (params != null) {
            decision.setParams(new HashMap<>(params));
        } else {
            decision.setParams(Map.of());
        }

        return decision;
    }

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

    // ==================== 关键词匹配策略 ====================

    private RouteDecision matchWithRules(String task, List<String> cards) {
        String taskLower = task.toLowerCase();
        RouteDecision best = new RouteDecision();
        best.setParams(Map.of());
        int bestScore = -1;

        for (String cardJson : cards) {
            JSONObject card = JSON.parseObject(cardJson);
            String name = card.getString("name");
            String desc = card.getString("description");
            int score = countKeywords(taskLower, name) * 3 + countKeywords(taskLower, desc);

            if (score > bestScore) {
                bestScore = score;
                best.setAgentName(name);
            }
        }

        if (best.getAgentName() == null) {
            JSONObject first = JSON.parseObject(cards.get(0));
            best.setAgentName(first.getString("name"));
        }

        return best;
    }

    private int countKeywords(String task, String text) {
        if (text == null) return 0;
        int count = 0;
        for (String word : text.toLowerCase().split("\\W+")) {
            if (word.length() >= 2 && task.contains(word)) {
                count++;
            }
        }
        return count;
    }

    // ==================== 远程调用 ====================

    private String invokeAgent(RouteDecision decision, String userTask) {
        String cardJson = discoveryClient.getAgent(decision.getAgentName())
                .orElseThrow(() -> new RuntimeException("Agent not found: " + decision.getAgentName()));

        return agentInvoker.invoke(cardJson, userTask);
    }
}
