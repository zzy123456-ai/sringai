# 多类型 Agent 测试指南

## 概述

当前系统支持 4 种 Agent 类型：`REMOTE_AGENT`、`MCP_TOOL`、`REST_API`、`RAG_KB`。本文档提供完整的测试方法，包括注册测试 Agent、调用 `/ai/agent-chat`（单 Agent 路由）和 `/ai/agent-workflow`（多步骤工作流编排）两个接口。

---

## 1. 启动服务

```bash
cd D:\code\SpringAI
mvn spring-boot:run
```

服务启动在 `http://localhost:9000`，需要确保 MySQL `story_shower` 数据库已启动且 `agent_card` 表存在。

### agent_card 表结构

```sql
CREATE TABLE IF NOT EXISTS `agent_card` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `agent_name` VARCHAR(255) NOT NULL,
    `agent_url` VARCHAR(512) DEFAULT '',
    `card_json` JSON,
    `enabled` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_agent_name` (`agent_name`)
);
```

---

## 2. 注册 4 种类型的测试 Agent

### 2.1 注册 REMOTE_AGENT（远程智能体）

需要一个可接收 POST 文本的 HTTP 端点。可以用 [httpbin.org/post](https://httpbin.org/post) 作为测试目标，它会回显请求内容。

```bash
curl -X PUT http://localhost:9000/agents/test-remote-agent \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-remote-agent",
    "description": "测试远程Agent，用于处理文本任务，擅长文本处理和内容生成",
    "url": "https://httpbin.org/post",
    "type": "REMOTE_AGENT",
    "skills": [
      {"id": "text-process", "description": "处理用户文本并返回结果"},
      {"id": "content-generate", "description": "根据输入生成内容"}
    ]
  }'
```

### 2.2 注册 MCP_TOOL（MCP 工具）

需要一个实现 MCP JSON-RPC `tools/call` 协议的端点。可以使用 [httpbin.org/post](https://httpbin.org/post) 作为测试目标（接收 JSON 并回显）。

```bash
curl -X PUT http://localhost:9000/agents/mcp/test-mcp-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-mcp-tool",
    "description": "测试MCP工具，用于天气查询和城市信息查询",
    "mcpServerUrl": "https://httpbin.org/post",
    "toolName": "weather_query",
    "inputSchema": {
      "type": "object",
      "properties": {
        "city": {"type": "string", "description": "城市名称"}
      }
    }
  }'
```

### 2.3 注册 REST_API（REST 接口）

使用公开的免费 API 作为测试目标，例如 `https://api.github.com`。

```bash
curl -X PUT http://localhost:9000/agents/rest/test-rest-api \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-rest-api",
    "description": "测试REST接口，用于查询资源和数据检索",
    "url": "https://api.github.com",
    "method": "GET"
  }'
```

### 2.4 注册 RAG_KB（RAG 知识库）

RAG 知识库依赖本地文档，需要先创建知识组并上传文档。

#### a) 创建知识组

```bash
curl -X POST http://localhost:9000/ai/rag/group \
  -H "Content-Type: application/json" \
  -d '{
    "groupName": "测试知识库",
    "description": "用于测试的RAG知识库",
    "queryHint": "请根据提供的测试文档内容回答用户问题，如果文档中没有相关信息请如实告知"
  }'
```

返回示例：`{"groupId": "a1b2c3d4", "groupName": "测试知识库", ...}`  记下返回的 `groupId`。

#### b) 准备测试文档

在 `knowledge-base/` 目录下准备一个 `.docx` 文件，例如 `test-sample.docx`，内容如下：

> 测试科技有限公司成立于2020年，总部位于北京海淀区。
> 公司主营业务包括人工智能平台研发、大数据分析服务和云计算解决方案。
> 公司现有员工500人，年营收超过2亿元人民币。
> 公司核心产品"智能分析平台"已在50家企业客户中部署使用。

#### c) 将文档添加到知识组

```bash
curl -X POST http://localhost:9000/ai/rag/group/{groupId}/file \
  -H "Content-Type: application/json" \
  -d '{"filePath": "D:\\code\\SpringAI\\knowledge-base\\test-sample.docx"}'
```

#### d) 注册 RAG Agent

```bash
curl -X PUT http://localhost:9000/agents/rag/test-rag-kb \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-rag-kb",
    "description": "测试RAG知识库，用于回答关于公司内部信息、业务数据和企业信息的问题",
    "groupId": "a1b2c3d4"
  }'
```

（将 `groupId` 替换为步骤 a 返回的实际值）

---

## 3. 验证注册结果

查看所有已注册的 Agent：

```bash
curl http://localhost:9000/agents
```

应返回包含 4 个 Agent Card JSON 的数组。

---

## 4. 测试 `/ai/agent-chat`（单 Agent 路由）

### 4.1 测试 REMOTE_AGENT 路由

```bash
curl "http://localhost:9000/ai/agent-chat?message=请帮我处理一段文本内容"
```

预期：LLM 识别到"文本处理"类任务，路由到 `test-remote-agent`，返回 httpbin 的 POST 回显内容。

### 4.2 测试 MCP_TOOL 路由

```bash
curl "http://localhost:9000/ai/agent-chat?message=帮我查询北京的天气"
```

预期：LLM 识别到"天气查询"任务，路由到 `test-mcp-tool`，返回 httpbin 回显的 MCP JSON-RPC 请求结果。

### 4.3 测试 REST_API 路由

```bash
curl "http://localhost:9000/ai/agent-chat?message=帮我查询一些资源信息"
```

预期：LLM 识别到"资源查询"任务，路由到 `test-rest-api`，返回 GitHub API 的首页 JSON。

### 4.4 测试 RAG_KB 路由

```bash
curl "http://localhost:9000/ai/agent-chat?message=测试科技有限公司的主营业务是什么"
```

预期：LLM 识别到涉及"公司业务"的知识查询，路由到 `test-rag-kb`，通过 RAG 检索文档内容后生成回答。

---

## 5. 测试 `/ai/agent-workflow`（多步骤工作流编排）

### 5.1 混合类型串联

```bash
curl "http://localhost:9000/ai/agent-workflow?message=查询北京天气，然后根据天气信息生成一份出行建议报告"
```

预期：LLM 拆解任务为多个步骤（例如：Step1 天气查询 → Step2 报告生成），返回带步骤标记的结果：

```
【步骤1 - test-mcp-tool】:
[天气查询结果...]

【步骤2 - test-remote-agent】:
[生成的出行建议报告...]
```

### 5.2 知识库 + Agent 串联

```bash
curl "http://localhost:9000/ai/agent-workflow?message=查询测试科技公司的主营业务，然后根据业务信息做一份SWOT分析报告"
```

预期：LLM 拆解为 Step1 RAG 检索业务信息 → Step2 Agent 生成 SWOT 报告。

### 5.3 复杂多步骤编排

```bash
curl "http://localhost:9000/ai/agent-workflow?message=帮我查询最新的GitHub趋势数据，并结合公司知识库的业务信息，分析哪些技术值得投入，最后生成一份技术投资建议报告"
```

预期：LLM 拆解为多个步骤（REST API 取数据 → RAG 查业务 → Agent 生成报告），串联执行。

---

## 6. 接口响应格式

### `/ai/agent-chat` 响应

```json
{
  "question": "用户输入的问题",
  "answer": "Agent 返回的结果文本"
}
```

### `/ai/agent-workflow` 响应

```json
{
  "question": "用户输入的问题",
  "answer": "【步骤1 - agent-name】:\n步骤1结果...\n\n【步骤2 - agent-name】:\n步骤2结果..."
}
```

---

## 7. 注意事项

| 注意点 | 说明 |
|--------|------|
| **LLM 路由非确定性** | LLM 可能选择不同的 Agent，响应内容会有差异，只要不报错即为正常 |
| **LLM API 消耗** | 每次 `/ai/agent-workflow` 调用会触发多次 LLM 请求（拆解 + 每步可能的参数提取），注意 API 费用 |
| **httpbin 限制** | httpbin.org 是公共服务，有频率限制，测试频繁时可能返回 503 |
| **RAG 依赖文档** | RAG_KB 类型的 Agent 需要知识组中至少有一个可解析的 .docx/.xlsx 文件 |
| **关键词路由模式** | 可在 `application.yaml` 中设置 `a2a.router.llm.enabled: false` 关闭 LLM 路由，使用关键词匹配（确定性） |
| **数据库** | 所有 Agent Card 持久化在 MySQL 中，服务重启后仍然存在 |

---

## 8. 清理测试数据

```bash
# 注销测试 Agent
curl -X DELETE http://localhost:9000/agents/test-remote-agent
curl -X DELETE http://localhost:9000/agents/test-mcp-tool
curl -X DELETE http://localhost:9000/agents/test-rest-api
curl -X DELETE http://localhost:9000/agents/test-rag-kb

# 删除知识组
curl -X DELETE http://localhost:9000/ai/rag/group/{groupId}
```

---

## 9. 一键测试脚本（Windows PowerShell）

将以下内容保存为 `test-agents.ps1`（将 `$GROUP_ID` 替换为实际值）：

```powershell
$BASE = "http://localhost:9000"

Write-Host "=== 1. 注册 Agent ===" -ForegroundColor Green

Write-Host "注册 REMOTE_AGENT..."
curl -X PUT "$BASE/agents/test-remote-agent" -H "Content-Type: application/json" -d '{"name":"test-remote-agent","description":"测试远程Agent，用于处理文本任务","url":"https://httpbin.org/post","type":"REMOTE_AGENT","skills":[{"id":"text-process","description":"处理用户文本"}]}'

Write-Host "注册 MCP_TOOL..."
curl -X PUT "$BASE/agents/mcp/test-mcp-tool" -H "Content-Type: application/json" -d '{"name":"test-mcp-tool","description":"测试MCP工具，用于天气查询","mcpServerUrl":"https://httpbin.org/post","toolName":"weather_query","inputSchema":{"type":"object","properties":{"city":{"type":"string","description":"城市名称"}}}}'

Write-Host "注册 REST_API..."
curl -X PUT "$BASE/agents/rest/test-rest-api" -H "Content-Type: application/json" -d '{"name":"test-rest-api","description":"测试REST接口，用于资源查询","url":"https://api.github.com","method":"GET"}'

Write-Host "注册 RAG_KB..."
curl -X PUT "$BASE/agents/rag/test-rag-kb" -H "Content-Type: application/json" -d '{"name":"test-rag-kb","description":"测试RAG知识库，用于回答公司信息","groupId":"YOUR_GROUP_ID"}'

Write-Host "`n=== 2. 验证注册 ===" -ForegroundColor Green
curl "$BASE/agents"

Write-Host "`n=== 3. 测试 agent-chat (远程Agent) ===" -ForegroundColor Green
curl "$BASE/ai/agent-chat?message=请帮我处理文本内容"

Write-Host "`n=== 4. 测试 agent-chat (MCP工具) ===" -ForegroundColor Green
curl "$BASE/ai/agent-chat?message=帮我查询北京天气"

Write-Host "`n=== 5. 测试 agent-workflow (多步骤) ===" -ForegroundColor Green
curl "$BASE/ai/agent-workflow?message=查询天气然后生成出行报告"

Write-Host "`n=== 完成 ===" -ForegroundColor Green
```
