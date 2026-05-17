package com.springai.mock;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MockAgentEndpointController {

    @PostMapping("/mock/agent/echo")
    public String echo(@RequestBody String body) {
        return "[MOCK_REMOTE_AGENT] received: " + body;
    }

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/mock/mcp/jsonrpc", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> mcpToolCall(@RequestBody Map<String, Object> request) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = params != null ? (String) params.get("name") : "unknown";
        Map<String, Object> arguments = params != null
                ? (Map<String, Object>) params.get("arguments") : Map.of();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", request.getOrDefault("id", "mock-id"));
        response.put("result", Map.of("content", List.of(
                Map.of("type", "text", "text",
                        "[MOCK_MCP_TOOL] executed: " + toolName + " with args: " + arguments))));
        return response;
    }

    @GetMapping("/mock/rest/resource")
    public Map<String, Object> getResource() {
        return Map.of("source", "[MOCK_REST_API]", "status", "ok", "data",
                Map.of("id", 1, "name", "test-resource"));
    }

    @GetMapping("/mock/rest/resource/{id}")
    public Map<String, Object> getResourceById(@PathVariable String id) {
        return Map.of("source", "[MOCK_REST_API]", "id", id, "result", "found");
    }
}
