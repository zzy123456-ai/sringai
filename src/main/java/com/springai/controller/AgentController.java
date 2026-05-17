package com.springai.controller;

import com.alibaba.fastjson.JSONObject;
import com.springai.service.AgentCardService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RequestMapping("/agents")
@RestController
public class AgentController {

    @Resource
    private AgentCardService agentCardService;

    @PutMapping("/{agentName}")
    public Map<String, Object> register(@PathVariable String agentName, @RequestBody JSONObject body) {
        String cardJson = body.toJSONString();
        String agentUrl = body.getString("url");
        agentCardService.register(agentName, agentUrl, cardJson);
        return Map.of("code", 0, "message", "registered");
    }

    @DeleteMapping("/{agentName}")
    public Map<String, Object> unregister(@PathVariable String agentName) {
        agentCardService.unregister(agentName);
        return Map.of("code", 0, "message", "unregistered");
    }

    @GetMapping
    public List<String> discover(@RequestParam(required = false) String capability) {
        return agentCardService.discover(capability);
    }

    /** 注册 MCP 工具 */
    @PutMapping("/mcp/{toolName}")
    public Map<String, Object> registerMcpTool(@PathVariable String toolName, @RequestBody JSONObject body) {
        body.put("type", "MCP_TOOL");
        body.putIfAbsent("name", toolName);
        String agentUrl = body.getString("mcpServerUrl");
        if (agentUrl == null) agentUrl = "";
        String cardJson = body.toJSONString();
        agentCardService.registerMcpTool(toolName, agentUrl, cardJson);
        return Map.of("code", 0, "message", "registered");
    }

    /** 注册 REST 接口 */
    @PutMapping("/rest/{apiName}")
    public Map<String, Object> registerRestApi(@PathVariable String apiName, @RequestBody JSONObject body) {
        body.put("type", "REST_API");
        body.putIfAbsent("name", apiName);
        String agentUrl = body.getString("url");
        if (agentUrl == null) agentUrl = "";
        String cardJson = body.toJSONString();
        agentCardService.registerRestApi(apiName, agentUrl, cardJson);
        return Map.of("code", 0, "message", "registered");
    }

    /** 注册 RAG 知识库 */
    @PutMapping("/rag/{kbName}")
    public Map<String, Object> registerRagKb(@PathVariable String kbName, @RequestBody JSONObject body) {
        body.put("type", "RAG_KB");
        body.putIfAbsent("name", kbName);
        String agentUrl = body.getString("groupId");
        if (agentUrl == null) agentUrl = "";
        String cardJson = body.toJSONString();
        agentCardService.registerRagKb(kbName, agentUrl, cardJson);
        return Map.of("code", 0, "message", "registered");
    }
}
