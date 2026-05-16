# A2A 路由设计

## 1. 架构概览

SpringAI 在整个 A2A 体系中同时承担两个角色：

- **注册中心（Registry）**：外部 Agent 通过 `PUT /agents/{agentName}` 注册自己的 AgentCard（含 name、description、url、skills），持久化到 MySQL `agent_card` 表
- **路由器（Router）**：接收用户对话请求，根据 Agent 的 description 和 skills 自动匹配最合适的 Agent，转发用户消息

```
用户消息
  → TestController./ai/agent-chat
    → AgentRouter.execute(userTask)
      → DiscoveryClient.getAllAgents()     // 从缓存/DB 获取所有已注册 Agent
      → matchAgent(userTask, cards)        // LLM 或关键词匹配，选出最佳 Agent
      → invokeAgent(decision, userTask)    // POST 用户文字到 Agent.url
```

## 2. Agent 选择策略

### 2.1 核心原则

**Skills 信息仅用于选择 Agent，不用于构造调用 URL。**

- 选 Agent 时：读取每个 Agent 的 `description` 和 `skills[]`，综合判断谁最适合处理用户请求
- 调 Agent 时：只调 Agent 的基础 URL，发送用户原始文字，让 Agent 内部自行理解意图并路由到 Skill

### 2.2 LLM 路由（主策略）

将 Agent 列表（名称、描述、Skills）拼入 Prompt → DeepSeek 返回 JSON：

```json
{"agent":"AgentName","params":{"key":"value"}}
```

`params` 是 LLM 从用户任务中提取的参数（如主题、字数等），随消息文本一起发送给 Agent。

### 2.3 关键词回退路由（备用策略）

当 `a2a.router.llm.enabled=false` 时自动降级：

- 对用户任务与每个 Agent 的名称（权重 ×3）和描述进行 token 重叠度评分
- 取最高分 Agent；若所有 Agent 均 0 分，默认选第一个

```java
int score = countKeywords(taskLower, name) * 3 + countKeywords(taskLower, desc);
```

## 3. 远程调用方式

### 3.1 实现

使用 Spring 官方推荐的 `RestClient`（Spring Framework 6.1+ 同步 HTTP 客户端）：

```java
return restClient.post()
        .uri(baseUrl)           // Agent 的 url，不拼任何 skill 路径
        .body(userTask)         // 用户原始输入文字
        .retrieve()
        .body(String.class);    // 返回 Agent 的处理结果
```

### 3.2 选型理由

| 理由 | 说明 |
|------|------|
| 已有现成 Bean | `RestClient.Builder` 已通过构造函数注入，零额外配置 |
| 连接池复用 | Spring Boot 自动配置底层连接池，无需手动管理 |
| 代码简洁 | fluent API 一行链式调用 |
| 风格统一 | 与 Spring 生态其他 HTTP 调用一致 |

### 3.3 请求格式

- Method: `POST`
- URL: AgentCard 中注册的 `url` 字段（如 `http://localhost:9001/`）
- Body: 用户输入文字（纯文本 `Content-Type: text/plain`）
- 不使用 JSON-RPC 协议

## 4. 服务端接口规范

### 4.1 服务端 Controller

被调用的 Agent 需要提供一个接收纯文本 POST 的接口：

```java
@RestController
public class AgentServerController {

    @PostMapping("/")
    public String handleMessage(@RequestBody String userMessage) {
        // 1. 接收用户输入
        // 2. Agent 内部理解意图，自行路由到合适的 Skill
        // 3. 执行 Skill 逻辑
        // 4. 返回结果
        return processWithInternalSkills(userMessage);
    }
}
```

### 4.2 AgentCard 注册示例

```json
{
  "name": "novel-writer",
  "description": "小说创作 Agent，擅长玄幻、都市、科幻等题材",
  "url": "http://localhost:9001/",
  "capabilities": ["text-generation", "story-writing"],
  "skills": [
    {
      "id": "write-chapter",
      "name": "章节创作",
      "description": "根据大纲和人物设定创作小说章节"
    },
    {
      "id": "character-design",
      "name": "人物设计",
      "description": "设计小说角色的性格、背景和外貌"
    }
  ]
}
```

### 4.3 调用关系

```
SpringAI (调用方)                      Agent (被调用方)
─────────────────                      ─────────────────
AgentRouter.invokeAgent()
  → POST http://localhost:9001/        → handleMessage(@RequestBody String)
     body: "帮我写一个玄幻小说开头"           → 内部判断：这是章节创作任务
                                         → 调用 write-chapter skill
                                         → 返回章节内容
  ← "第一章：星辰陨落\n..."
```

## 5. 修改记录

### 2026-05-16 — 简化 Agent 路由

**修改文件**：`src/main/java/com/springai/common/config/AgentRouter.java`

| 变更项 | 修改前 | 修改后 |
|--------|--------|--------|
| A2A SDK 依赖 | 引入 `SendMessageRequest`、`MessageSendParams`、`TextPart` 等 | 全部移除 |
| LLM 响应格式 | `{"agent":"...","skill_id":"...","params":{...}}` | `{"agent":"...","params":{...}}` |
| 关键词匹配 | 遍历 Agent + Skill 组合打分 | 仅按 Agent 名称+描述打分 |
| URL 构造 | `baseUrl + "/agent/" + skillId` | 直接用 `baseUrl` |
| 请求体 | `SendMessageRequest` JSON-RPC | 纯文本 `userTask` 字符串 |

**设计决策**：
- 不再直接调用 Skill 端点，Skill 路由权交给 Agent 内部
- 不采用 JSON-RPC 协议，用最简单的纯文本 POST
