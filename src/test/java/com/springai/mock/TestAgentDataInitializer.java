package com.springai.mock;

import com.alibaba.fastjson.JSONObject;
import com.springai.entity.KnowledgeGroup;
import com.springai.service.AgentCardService;
import com.springai.service.KnowledgeGroupService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class TestAgentDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(TestAgentDataInitializer.class);

    private static final String REMOTE_AGENT = "test-remote-agent";
    private static final String MCP_TOOL = "test-mcp-tool";
    private static final String REST_API = "test-rest-api";
    private static final String RAG_KB = "test-rag-kb";

    private final AgentCardService agentCardService;
    private final KnowledgeGroupService knowledgeGroupService;
    private int actualPort = -1;

    public TestAgentDataInitializer(AgentCardService agentCardService,
                                     KnowledgeGroupService knowledgeGroupService) {
        this.agentCardService = agentCardService;
        this.knowledgeGroupService = knowledgeGroupService;
    }

    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        this.actualPort = event.getWebServer().getPort();
        log.info("=== Web server started on port {} ===", actualPort);
        try {
            cleanup();
            registerRemoteAgent(actualPort);
            registerMcpTool(actualPort);
            registerRestApi(actualPort);
            registerRagKb(actualPort);
            log.info("=== All 4 test agents registered on port {} ===", actualPort);
        } catch (Exception e) {
            log.error("Failed to seed test agents", e);
        }
    }

    private void cleanup() {
        for (String name : new String[]{REMOTE_AGENT, MCP_TOOL, REST_API, RAG_KB}) {
            try { agentCardService.unregister(name); } catch (Exception ignored) {}
        }
    }

    private void registerRemoteAgent(int port) {
        JSONObject card = new JSONObject();
        card.put("name", REMOTE_AGENT);
        card.put("description", "测试远程Agent，用于处理文本回显和一般任务，支持echo回显用户输入");
        card.put("url", "http://localhost:" + port + "/mock/agent/echo");
        card.put("type", "REMOTE_AGENT");
        card.put("skills", java.util.List.of(
                java.util.Map.of("id", "echo", "description", "回显用户输入并返回")
        ));
        agentCardService.register(REMOTE_AGENT, card.getString("url"), card.toJSONString());
        log.info("  Registered REMOTE_AGENT: {}", REMOTE_AGENT);
    }

    private void registerMcpTool(int port) {
        JSONObject card = new JSONObject();
        card.put("name", MCP_TOOL);
        card.put("description", "测试MCP工具，用于天气查询和城市信息查询，支持weather_query工具调用");
        card.put("mcpServerUrl", "http://localhost:" + port + "/mock/mcp/jsonrpc");
        card.put("toolName", "weather_query");
        card.put("inputSchema", java.util.Map.of(
                "type", "object",
                "properties", java.util.Map.of(
                        "city", java.util.Map.of("type", "string", "description", "城市名称")
                )
        ));
        card.put("type", "MCP_TOOL");
        agentCardService.registerMcpTool(MCP_TOOL, card.getString("mcpServerUrl"), card.toJSONString());
        log.info("  Registered MCP_TOOL: {}", MCP_TOOL);
    }

    private void registerRestApi(int port) {
        JSONObject card = new JSONObject();
        card.put("name", REST_API);
        card.put("description", "测试REST接口，用于资源查询和数据检索，支持GET请求获取资源信息");
        card.put("url", "http://localhost:" + port + "/mock/rest/resource");
        card.put("method", "GET");
        card.put("type", "REST_API");
        agentCardService.registerRestApi(REST_API, card.getString("url"), card.toJSONString());
        log.info("  Registered REST_API: {}", REST_API);
    }

    private void registerRagKb(int port) throws Exception {
        String docxPath = createTestDocx();
        String groupId = createRagGroup(docxPath);

        JSONObject card = new JSONObject();
        card.put("name", RAG_KB);
        card.put("description", "测试RAG知识库，用于回答关于测试科技公司的业务信息和内部数据");
        card.put("groupId", groupId);
        card.put("type", "RAG_KB");
        agentCardService.registerRagKb(RAG_KB, card.getString("groupId"), card.toJSONString());
        log.info("  Registered RAG_KB: {} (groupId={})", RAG_KB, groupId);
    }

    private String createTestDocx() throws Exception {
        Path kbDir = Paths.get("knowledge-base");
        Files.createDirectories(kbDir);
        String filePath = kbDir.resolve("test-sample.docx").toAbsolutePath().toString();

        if (Files.exists(Paths.get(filePath))) {
            log.info("  Test docx already exists: {}", filePath);
            return filePath;
        }

        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(filePath)) {

            String[] paragraphs = {
                "测试科技有限公司成立于2020年，总部位于北京海淀区中关村科技园。公司专注于人工智能和大数据技术的研发与应用。",
                "公司主营业务包括三大板块：人工智能平台研发、大数据分析服务和云计算解决方案。其中智能分析平台是公司的核心产品，已在50家企业客户中部署使用。",
                "公司现有员工500人，其中研发人员占比超过60%。公司年营收超过2亿元人民币，连续三年保持30%以上的增长率。",
                "公司创始人兼CEO李明华博士毕业于清华大学计算机系，曾在Google和微软亚洲研究院工作，拥有15年AI领域研发经验。",
                "公司文化倡导「技术驱动、客户至上、开放协作」。团队采用扁平化管理，鼓励技术创新和个人成长。"
            };

            for (String text : paragraphs) {
                XWPFParagraph para = doc.createParagraph();
                para.createRun().setText(text);
            }

            doc.write(out);
            log.info("  Created test docx: {}", filePath);
        }
        return filePath;
    }

    private String createRagGroup(String docxPath) {
        KnowledgeGroup group = knowledgeGroupService.createGroup(
                "测试知识库", "用于Agent路由测试的RAG知识库",
                "请根据提供的测试科技公司文档内容回答用户问题，如果文档中没有相关信息请如实告知");
        knowledgeGroupService.addFileToGroup(group.getGroupId(), docxPath);
        log.info("  Created RAG group: {} with file: {}", group.getGroupId(), docxPath);
        return group.getGroupId();
    }
}
