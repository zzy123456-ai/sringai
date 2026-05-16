# SpringAI — 故事秀 (Story Shower)

基于 Spring Boot + Spring AI 的小说阅读平台，整合 DeepSeek 大模型实现 AI 智能交互与 RAG 检索增强生成。

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.13 | 基础框架 |
| Java | 17 | 运行环境 |
| Spring AI | 1.1.2 | AI 模型集成框架 |
| Spring AI Alibaba | 1.1.2.2 | Agent/Workflow/多智能体框架（后期使用） |
| MyBatis | 3.0.4 | ORM 数据库访问 |
| MySQL | — | 业务数据库 |
| DeepSeek | v4-pro | AI 大模型（当前直连官网） |
| Apache POI | 5.3.0 | Word/Excel 文档解析 |
| FastJSON | — | JSON 处理 |

---

## 项目结构

```
src/main/java/com/springai/
├── SpringAiApplication.java          # 启动类
├── common/config/
│   └── AiConfig.java                 # AI 配置（ChatClient Bean）
├── controller/
│   └── TestController.java           # 测试控制器（基础对话 + RAG 接口）
├── entity/
│   ├── KnowledgeGroup.java           # 知识分组实体
│   ├── TextChunk.java                # 文档分块实体
│   └── RetrievedChunk.java           # 检索结果实体
├── mapper/
│   └── CategoryMapper.java           # 分类表 Mapper
├── pojo/
│   └── Category.java                 # 分类实体
└── service/
    ├── CategoryToolService.java      # 分类工具服务接口
    ├── KnowledgeGroupService.java    # 知识分组服务接口
    ├── DocumentParserService.java    # 文档解析服务接口
    ├── RagService.java               # RAG 检索生成服务接口
    └── impl/
        ├── CategoryToolServiceImpl.java  # 分类工具服务实现（@Tool）
        ├── KnowledgeGroupServiceImpl.java # 知识分组管理（JSON 持久化）
        ├── DocumentParserServiceImpl.java # Word/Excel 解析与分块
        └── RagServiceImpl.java           # 关键词检索 + LLM 增强生成

src/main/resources/
└── application.yaml                  # 应用配置

knowledge-base/                       # 知识库文件目录
└── groups.json                       # 分组配置（自动生成）
```

---

## 数据库设计

### 数据库名: `story_shower`

### 表结构

#### 1. `user` — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | VARCHAR | 用户 ID（主键） |
| username | VARCHAR | 用户名 |
| password | VARCHAR | 密码 |
| email | VARCHAR | 邮箱 |
| created_date | DATETIME | 创建时间 |
| updated_date | DATETIME | 更新时间 |

#### 2. `category` — 分类表

| 字段 | 类型 | 说明 |
|------|------|------|
| category_id | VARCHAR | 分类 ID（主键） |
| category_name | VARCHAR | 分类名称 |
| sort_num | INT | 排序号 |
| created_date | DATETIME | 创建时间 |
| updated_date | DATETIME | 更新时间 |

#### 3. `novel` — 小说表

| 字段 | 类型 | 说明 |
|------|------|------|
| novel_id | VARCHAR | 小说 ID（主键） |
| user_id | VARCHAR | 作者 ID（外键 → user.user_id） |
| category_id | VARCHAR | 分类 ID（外键 → category.category_id） |
| title | VARCHAR | 小说标题 |
| summary | VARCHAR | 小说简介 |
| cover_name | VARCHAR | 封面原文件名 |
| cover_type | VARCHAR | 封面文件类型（如 image/jpeg） |
| cover_data | BLOB | 封面二进制数据 |
| status | INT | 状态：0=连载中，1=已完结，2=下架 |
| created_date | DATETIME | 创建时间 |
| updated_date | DATETIME | 更新时间 |

#### 4. `chapter` — 章节表

| 字段 | 类型 | 说明 |
|------|------|------|
| chapter_id | VARCHAR | 章节 ID（主键） |
| novel_id | VARCHAR | 小说 ID（外键 → novel.novel_id） |
| chapter_num | INT | 章节序号 |
| title | VARCHAR | 章节标题 |
| content | TEXT | 章节正文 |
| word_count | INT | 章节字数 |
| status | INT | 状态：0=草稿，1=已发布，2=隐藏 |
| view_count | BIGINT | 阅读量 |
| created_date | DATETIME | 创建时间 |
| updated_date | DATETIME | 更新时间 |

#### 5. `bookshelf` — 书架表

| 字段 | 类型 | 说明 |
|------|------|------|
| bookshelf_id | BIGINT | 主键（自增） |
| user_id | VARCHAR | 用户 ID |
| novel_id | VARCHAR | 小说 ID |

---

## API 接口

### 基础对话

#### POST `/ai/test` — AI 对话测试

- **入参**: `{ "message": "用户消息" }`
- **出参**: `{ "message": "...", "answer": "AI回复" }`
- **说明**: 直接将用户消息发给 DeepSeek，返回 AI 回复。

#### GET `/ai/db-chat` — AI 数据库查询对话

- **入参**: `?message=有哪些分类？`（可选，默认值为"有哪些分类？"）
- **出参**: `{ "question": "...", "answer": "AI回复（含真实分类数据）" }`
- **说明**: AI 通过 `CategoryToolService.getAllCategories()` 工具查询数据库真实分类数据后回答。

---

### RAG 知识分组管理

#### POST `/ai/rag/group` — 创建知识分组

- **入参**: `{ "groupName": "部队信息", "description": "部队编制相关文档", "queryHint": "查询部队编制、人员信息" }`
- **出参**: `KnowledgeGroup` 对象（含自动生成的 groupId）

#### GET `/ai/rag/groups` — 查看所有分组

- **出参**: `List<KnowledgeGroup>`

#### GET `/ai/rag/group/{groupId}` — 查看单个分组

- **出参**: `KnowledgeGroup`

#### DELETE `/ai/rag/group/{groupId}` — 删除分组

- **出参**: 操作结果字符串

#### POST `/ai/rag/group/{groupId}/file` — 向分组添加文件

- **入参**: `{ "filePath": "/绝对路径/或/相对路径/文档.docx" }`
- **出参**: 操作结果字符串

#### DELETE `/ai/rag/group/{groupId}/file` — 从分组移除文件

- **入参**: `{ "filePath": "/绝对路径/或/相对路径/文档.docx" }`
- **出参**: 操作结果字符串

#### PUT `/ai/rag/group/{groupId}/hint` — 更新查询目标

- **入参**: `{ "queryHint": "新的查询目标描述" }`
- **出参**: 操作结果字符串

---

### RAG 检索与生成

#### GET `/ai/rag/search` — 纯检索（不调 LLM）

- **入参**: `?groupId=xxx&query=关键词&topK=3`
- **出参**: `List<RetrievedChunk>`（含来源文件名、内容片段、相关度评分）

#### GET `/ai/rag/generate` — 增强生成（检索 + LLM）

- **入参**: `?groupId=xxx&query=查询问题`
- **出参**: `{ "query": "...", "answer": "AI基于文档生成的回答（含来源引用）" }`

---

## RAG 设计

### 架构流程

```
用户请求 generate(groupId, query)
  → KnowledgeGroupService.getGroup(groupId)    # 获取分组配置（含文件列表和queryHint）
  → DocumentParserService.parseDocument()       # 逐个文件解析全文
  → DocumentParserService.chunkDocument()       # 将全文按 500 字符分块
  → RagService.search()                        # 关键词匹配打分，取 top-K
  → 拼接 SystemPrompt（queryHint + 检索到的文档片段）
  → ChatClient → DeepSeek 生成回答
  → 返回带来源引用的答案
```

### 文档解析

- **Word (.docx)**: 使用 Apache POI XWPFDocument，按段落提取文本
- **Excel (.xlsx)**: 使用 Apache POI XSSFWorkbook，按行提取单元格内容，多 Sheet 时附带 Sheet 名
- **分块策略**: 按段落累积至 500 字符为一块，保持段落完整性

### 关键词匹配打分

- 提取查询文本中的中文 2-4 字词组和英文单词作为 token
- 计算 token 在文档片段中的命中率（70% 权重）
- 精确子串匹配额外加分（30% 权重）
- 综合得分排序，取 top-K 结果

### 分组持久化

- 分组配置以 JSON 格式存储在 `knowledge-base/groups.json`
- 应用启动时自动加载，每次变更自动保存

---

## 待完善项

- **[ ] 无 MyBatis XML 映射文件**，配置了 `mapper-locations: classpath:mapper/*.xml` 但 resources 下无 mapper 目录
- **[ ] API Key 和数据库密码** 明文写在 `application.yaml` 中，建议迁移到环境变量或配置中心

---

## 启动方式

```bash
# 1. 确保本地 MySQL 运行，数据库 story_shower 已建表
# 2. 修改 application.yaml 中的数据库连接信息（如需要）
# 3. 启动项目
mvn spring-boot:run
```

服务启动后访问 `http://localhost:8080`。
