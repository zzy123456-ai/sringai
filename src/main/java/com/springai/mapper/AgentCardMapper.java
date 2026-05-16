package com.springai.mapper;

import com.springai.pojo.AgentCard;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AgentCardMapper {

    @Select("SELECT id, agent_name, agent_url, card_json, enabled, created_at, updated_at FROM agent_card WHERE agent_name = #{agentName}")
    AgentCard findByAgentName(String agentName);

    @Insert("INSERT INTO agent_card (agent_name, agent_url, card_json, enabled, created_at, updated_at) VALUES (#{agentName}, #{agentUrl}, #{cardJson}, 1, now(), now())")
    int insert(AgentCard record);

    @Update("UPDATE agent_card SET card_json = #{cardJson}, agent_url = #{agentUrl}, enabled = 1, updated_at = now() WHERE agent_name = #{agentName}")
    int updateCard(AgentCard record);

    @Update("UPDATE agent_card SET enabled = 0, updated_at = now() WHERE agent_name = #{agentName}")
    int disableByAgentName(String agentName);

    @Select("SELECT id, agent_name, agent_url, card_json, enabled, created_at, updated_at FROM agent_card WHERE enabled = 1")
    List<AgentCard> findAllEnabled();

    @Select("SELECT id, agent_name, agent_url, card_json, enabled, created_at, updated_at FROM agent_card WHERE enabled = 1 AND JSON_CONTAINS(card_json, #{capability}, '$.capabilities')")
    List<AgentCard> findEnabledByCapability(String capability);
}
