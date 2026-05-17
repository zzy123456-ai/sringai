# 从 DeepSeek 切换到本地千问3 32B 模型指南

## 概述

当前项目使用 DeepSeek 云端 API（`spring-ai-starter-model-deepseek`）。要切换到本地部署的千问3 32B，需要修改 Maven 依赖和配置文件，无需修改任何 Java 代码。

千问3 32B 通过 vLLM / Ollama / llama.cpp 等工具部署后，通常会暴露一个 OpenAI 兼容的 API 端点。Spring AI 的 `spring-ai-starter-model-openai` 可以直接对接。

## 前置条件

本地千问3 32B 已部署并运行，例如：

| 部署方式 | 默认端点 |
|---------|---------|
| vLLM | `http://localhost:8000/v1` |
| Ollama | `http://localhost:11434/v1` |
| llama.cpp server | `http://localhost:8080/v1` |

以下以 **vLLM 部署、模型名为 `Qwen/Qwen3-32B`** 为例。

## 修改步骤

### 1. 修改 pom.xml — 替换依赖

```xml
<!-- 删除或注释 DeepSeek 依赖 -->
<!--
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-deepseek</artifactId>
</dependency>
-->

<!-- 新增 OpenAI 依赖（兼容本地 OpenAI 式 API） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

### 2. 修改 application.yaml — 替换模型配置

```yaml
spring:
  ai:
    # 删除原有 deepseek 配置块，替换为 openai 配置
    openai:
      api-key: not-needed            # 本地模型通常不需要，但不能为空
      base-url: http://localhost:8000/v1   # vLLM 默认端点
      chat:
        options:
          model: Qwen/Qwen3-32B      # vLLM 中注册的模型名
          temperature: 0.7

# A2A 路由器的 LLM 配置也需要同步修改
a2a:
  router:
    llm:
      enabled: true
      base-url: http://localhost:8000/v1
      provider: openai               # 改为 openai
      api-key: not-needed
```

完整修改后的 `application.yaml`:

```yaml
server:
  port: 9000
spring:
  application:
    name: SpringAI
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/story_shower?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: weilin915081
    driver-class-name: com.mysql.cj.jdbc.Driver

  ai:
    openai:
      api-key: not-needed
      base-url: http://localhost:8000/v1
      chat:
        options:
          model: Qwen/Qwen3-32B
          temperature: 0.7

logging:
  level:
    root: info
    org.springframework.ai: debug

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.springai.pojo
  configuration:
    map-underscore-to-camel-case: true

a2a:
  registry:
    server-url: http://localhost:9000
  router:
    llm:
      enabled: true
      base-url: http://localhost:8000/v1
      provider: openai
      api-key: not-needed
  cacheTtlMillis: 30
```

### 3. 不同部署方式的配置差异

**Ollama 部署：**

```yaml
spring:
  ai:
    openai:
      base-url: http://localhost:11434/v1
      chat:
        options:
          model: qwen3:32b        # Ollama 中的模型名
```

**llama.cpp server 部署：**

```yaml
spring:
  ai:
    openai:
      base-url: http://localhost:8080/v1
      chat:
        options:
          model: qwen3-32b
```

### 4. 代码层面

**不需要任何改动。** `ChatClient` 通过 Spring AI 自动配置，会自动识别 `spring.ai.openai` 下的配置。项目中所有使用 `ChatClient` 的地方（`AgentRouter`、`AgentInvoker`、`TestController` 等）无需修改。

### 5. 验证切换是否生效

启动应用后，查看日志确认模型加载：

```
# 正常启动后会看到类似日志：
DEBUG o.s.ai.openai.OpenAiChatModel - ...
```

发送测试请求验证：

```bash
curl "http://localhost:9000/ai/agent-chat?message=你好"
```

返回正常回复即表示切换成功。

## 常见问题

**Q: `api-key` 需要填真实的吗？**

本地部署通常不需要，但 Spring AI 要求不能为空，填任意字符串即可（如 `not-needed`）。

**Q: 千问3 32B 用 `spring-ai-starter-model-openai` 而不是 DashScope 的吗？**

DashScope starter 走的是阿里云 API，不是本地模型。本地部署的千问暴露的是 OpenAI 兼容接口，所以用 OpenAI starter 对接。

**Q: 如何同时保留两套配置方便切换？**

可以创建 `application-deepseek.yaml` 和 `application-qwen3.yaml`，通过 `spring.profiles.active` 切换：

```bash
# 使用本地千问
java -jar app.jar --spring.profiles.active=qwen3

# 使用 DeepSeek
java -jar app.jar --spring.profiles.active=deepseek
```
