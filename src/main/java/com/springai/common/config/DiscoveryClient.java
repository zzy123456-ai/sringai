package com.springai.common.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.springai.service.AgentCardService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class DiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryClient.class);

    @Value("${a2a.registry.server-url}")
    private String registryServerUrl;

    @Value("${a2a.cacheTtlMillis:30000}")
    private long cacheTtlMillis;

    private final RestClient restClient;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private volatile long lastRefreshTime = 0;

    private final ReentrantLock refreshLock = new ReentrantLock();

    public DiscoveryClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<String> getAllAgents() {
        if (isCacheExpired()) {
            refreshCache();
        }
        return new ArrayList<>(cache.values());
    }

    public Optional<String> getAgent(String agentName) {
        if (cache.containsKey(agentName)) {
            return Optional.of(cache.get(agentName));
        }
        refreshCache();
        return Optional.ofNullable(cache.get(agentName));
    }

    private void refreshCache() {
        if (refreshLock.tryLock()) {
            try {
                if (isCacheExpired()) {
                    List<String> cardJsonList = fetchFromRegistry();
                    cache.clear();
                    for (String cardJson : cardJsonList) {
                        String name = extractAgentName(cardJson);
                        if (name != null) {
                            cache.put(name, cardJson);
                        }
                    }
                    lastRefreshTime = System.currentTimeMillis();
                    log.info("Cache refreshed, {} agents loaded", cache.size());
                }
            } finally {
                refreshLock.unlock();
            }
        }
    }
    @Resource
    private AgentCardService agentCardService;
    private List<String> fetchFromRegistry() {
        String url = registryServerUrl + "/agents/discover";
        try {
            List<String> body= agentCardService.discover("");
            return body;
        } catch (Exception e) {
            log.warn("Failed to fetch agents from registry: {}", e.getMessage());
            if (!cache.isEmpty()) {
                log.info("Degrading to local cache, {} agents available", cache.size());
                return new ArrayList<>(cache.values());
            }
            throw new RegistryException("Failed to fetch agents and no cache available", e);
        }
    }

    private String extractAgentName(String cardJson) {
        try {
            return JSON.parseObject(cardJson).getString("name");
        } catch (Exception e) {
            log.warn("Failed to extract agent name from card_json: {}", e.getMessage());
            return null;
        }
    }

    private boolean isCacheExpired() {
        return System.currentTimeMillis() - lastRefreshTime > cacheTtlMillis;
    }
}
