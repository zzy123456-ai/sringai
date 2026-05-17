package com.springai.common.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.springai.pojo.WorkflowPlan;
import com.springai.pojo.WorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkflowRouter {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRouter.class);

    private final DiscoveryClient discoveryClient;
    private final ChatClient chatClient;
    private final AgentInvoker agentInvoker;

    public WorkflowRouter(DiscoveryClient discoveryClient,
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

        log.info("Decomposing task with {} available agents: {}", cards.size(), userTask);
        WorkflowPlan plan = decomposeTask(userTask, cards);

        if (plan.getSteps() == null || plan.getSteps().isEmpty()) {
            return "Failed to decompose task into steps.";
        }

        // Sort by order
        plan.getSteps().sort(Comparator.comparingInt(WorkflowStep::getOrder));

        log.info("Workflow plan: {} steps", plan.getSteps().size());
        for (WorkflowStep step : plan.getSteps()) {
            log.info("  Step {}: agent={}, task={}, dependsOn={}",
                    step.getOrder(), step.getAgent(), step.getTask(), step.getDependsOn());
        }

        return executeSteps(plan, userTask);
    }

    // ==================== LLM 任务拆解 ====================

    WorkflowPlan decomposeTask(String userTask, List<String> cards) {
        String prompt = buildWorkflowPrompt(userTask, cards);
        String llmResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        log.info("LLM decomposition response: {}", llmResponse);
        return parseWorkflowPlan(llmResponse);
    }

    String buildWorkflowPrompt(String userTask, List<String> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个任务规划器。请分析用户需求，判断是否可以由一个Agent直接完成，还是需要拆解为多个步骤，由不同的Agent顺序协作完成。\n\n");
        sb.append("可用Agent列表：\n");

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

        sb.append("用户需求: ").append(userTask).append("\n\n");
        sb.append("""
                请返回纯JSON（不要包含```json标记）：
                {
                  "steps": [
                    {"order":1, "agent":"AgentName", "task":"该步骤具体任务描述", "dependsOn":[]},
                    {"order":2, "agent":"AgentName", "task":"该步骤任务（可引用前序结果）", "dependsOn":[1]}
                  ]
                }
                注意：
                - order从1开始递增
                - dependsOn为依赖的前序步骤order列表，无依赖则填空数组[]
                - 如果只需要一个Agent，返回单个step的数组
                - task描述要具体明确，让Agent能直接执行""");

        return sb.toString();
    }

    WorkflowPlan parseWorkflowPlan(String llmResponse) {
        String json = extractJson(llmResponse);
        JSONObject obj = JSON.parseObject(json);
        JSONArray stepsArr = obj.getJSONArray("steps");

        WorkflowPlan plan = new WorkflowPlan();
        List<WorkflowStep> steps = new ArrayList<>();

        for (int i = 0; i < stepsArr.size(); i++) {
            JSONObject stepObj = stepsArr.getJSONObject(i);
            WorkflowStep step = new WorkflowStep();
            step.setOrder(stepObj.getIntValue("order"));
            step.setAgent(stepObj.getString("agent"));
            step.setTask(stepObj.getString("task"));

            JSONArray dependsArr = stepObj.getJSONArray("dependsOn");
            if (dependsArr != null && !dependsArr.isEmpty()) {
                List<Integer> depends = new ArrayList<>();
                for (int j = 0; j < dependsArr.size(); j++) {
                    depends.add(dependsArr.getInteger(j));
                }
                step.setDependsOn(depends);
            } else {
                step.setDependsOn(List.of());
            }

            steps.add(step);
        }

        plan.setSteps(steps);
        return plan;
    }

    // ==================== 顺序执行 ====================

    private String executeSteps(WorkflowPlan plan, String userTask) {
        Map<Integer, String> results = new LinkedHashMap<>();
        StringBuilder finalResult = new StringBuilder();

        for (WorkflowStep step : plan.getSteps()) {
            log.info("Executing step {}/{}: agent={}",
                    step.getOrder(), plan.getSteps().size(), step.getAgent());

            // Build input: original task + previous step results
            StringBuilder input = new StringBuilder();
            input.append("用户需求: ").append(userTask).append("\n");

            if (step.getDependsOn() != null) {
                for (int depOrder : step.getDependsOn()) {
                    String depResult = results.get(depOrder);
                    if (depResult != null) {
                        input.append("\n前序步骤[").append(depOrder).append("]的结果:\n");
                        input.append(depResult).append("\n");
                    }
                }
            }

            input.append("\n当前任务: ").append(step.getTask());

            // Look up agent card
            String cardJson = discoveryClient.getAgent(step.getAgent())
                    .orElse(null);
            if (cardJson == null) {
                String err = "Agent not found: " + step.getAgent();
                log.warn(err);
                results.put(step.getOrder(), err);
                finalResult.append("【步骤").append(step.getOrder())
                        .append(" - ").append(step.getAgent()).append("】: ")
                        .append(err).append("\n\n");
                continue;
            }

            // Invoke agent via AgentInvoker (type-based dispatch)
            try {
                String output = agentInvoker.invoke(cardJson, input.toString());
                results.put(step.getOrder(), output);
                finalResult.append("【步骤").append(step.getOrder())
                        .append(" - ").append(step.getAgent()).append("】:\n")
                        .append(output).append("\n\n");
                log.info("Step {} completed, output length={}", step.getOrder(), output.length());
            } catch (Exception e) {
                String err = "Error calling " + step.getAgent() + ": " + e.getMessage();
                log.error("Step {} failed: {}", step.getOrder(), err);
                results.put(step.getOrder(), err);
                finalResult.append("【步骤").append(step.getOrder())
                        .append(" - ").append(step.getAgent()).append("】: ")
                        .append(err).append("\n\n");
            }
        }

        return finalResult.toString().trim();
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
