package com.springai.service;

import java.util.List;

public interface AgentCardService {

    void register(String agentName, String agentUrl, String cardJson);

    void unregister(String agentName);

    List<String> discover(String capability);
}
