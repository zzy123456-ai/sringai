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
}
