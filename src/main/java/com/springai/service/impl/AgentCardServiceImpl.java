package com.springai.service.impl;

import com.alibaba.fastjson.JSON;
import com.springai.mapper.AgentCardMapper;
import com.springai.pojo.AgentCard;
import com.springai.service.AgentCardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentCardServiceImpl implements AgentCardService {

    @Resource
    private AgentCardMapper agentCardMapper;

    @Override
    public void register(String agentName, String agentUrl, String cardJson) {
        AgentCard existing = agentCardMapper.findByAgentName(agentName);
        if (existing == null) {
            AgentCard record = new AgentCard();
            record.setAgentName(agentName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.insert(record);
        } else {
            AgentCard record = new AgentCard();
            record.setAgentName(agentName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.updateCard(record);
        }
    }

    @Override
    public void registerMcpTool(String toolName, String agentUrl, String cardJson) {
        AgentCard existing = agentCardMapper.findByAgentName(toolName);
        if (existing == null) {
            AgentCard record = new AgentCard();
            record.setAgentName(toolName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.insert(record);
        } else {
            AgentCard record = new AgentCard();
            record.setAgentName(toolName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.updateCard(record);
        }
    }

    @Override
    public void registerRestApi(String apiName, String agentUrl, String cardJson) {
        AgentCard existing = agentCardMapper.findByAgentName(apiName);
        if (existing == null) {
            AgentCard record = new AgentCard();
            record.setAgentName(apiName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.insert(record);
        } else {
            AgentCard record = new AgentCard();
            record.setAgentName(apiName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.updateCard(record);
        }
    }

    @Override
    public void registerRagKb(String kbName, String agentUrl, String cardJson) {
        AgentCard existing = agentCardMapper.findByAgentName(kbName);
        if (existing == null) {
            AgentCard record = new AgentCard();
            record.setAgentName(kbName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.insert(record);
        } else {
            AgentCard record = new AgentCard();
            record.setAgentName(kbName);
            record.setAgentUrl(agentUrl);
            record.setCardJson(cardJson);
            agentCardMapper.updateCard(record);
        }
    }

    @Override
    public void unregister(String agentName) {
        agentCardMapper.disableByAgentName(agentName);
    }

    @Override
    public List<String> discover(String capability) {
        List<AgentCard> cards;
        if (capability != null && !capability.isBlank()) {
            cards = agentCardMapper.findEnabledByCapability(capability);
        } else {
            cards = agentCardMapper.findAllEnabled();
        }
        if (cards == null) {
            return Collections.emptyList();
        }
        return cards.stream().map(AgentCard::getCardJson).collect(Collectors.toList());
    }
}
