package com.springai.service;

import java.util.List;

public interface AgentCardService {

    void register(String agentName, String agentUrl, String cardJson);

    /** 注册 MCP 工具 */
    void registerMcpTool(String toolName, String agentUrl, String cardJson);

    /** 注册 REST 接口 */
    void registerRestApi(String apiName, String agentUrl, String cardJson);

    /** 注册 RAG 知识库 */
    void registerRagKb(String kbName, String agentUrl, String cardJson);

    void unregister(String agentName);

    List<String> discover(String capability);
}
