package com.springai;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "a2a.cacheTtlMillis=0")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultiTypeAgentRoutingTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // ==================== agent-chat 单Agent路由测试 ====================

    @Test
    @Order(1)
    void testAgentChat_remoteAgentEcho() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-chat?message=请帮我回显这段文字：你好世界", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        String answer = (String) response.getBody().get("answer");
        assertThat(answer).isNotNull().isNotEmpty();
    }

    @Test
    @Order(2)
    void testAgentChat_mcpWeatherQuery() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-chat?message=帮我查询北京的天气", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("answer")).isNotNull();
    }

    @Test
    @Order(3)
    void testAgentChat_restApiResourceQuery() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-chat?message=帮我查询一条资源信息", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        String answer = (String) response.getBody().get("answer");
        assertThat(answer).isNotNull().isNotEmpty();
    }

    @Test
    @Order(4)
    void testAgentChat_ragKbCompanyInfo() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-chat?message=测试科技公司的主营业务是什么", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        String answer = (String) response.getBody().get("answer");
        assertThat(answer).isNotNull().isNotEmpty();
    }

    @Test
    @Order(5)
    void testAgentChat_allAgentsAvailable() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-chat?message=你好", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("answer")).isNotNull();
    }

    // ==================== agent-workflow 工作流测试 ====================

    @Test
    @Order(6)
    void testAgentWorkflow_echoTask() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-workflow?message=帮我回显一段文字内容", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String answer = (String) response.getBody().get("answer");
        assertThat(answer).isNotNull().isNotEmpty();
    }

    @Test
    @Order(7)
    void testAgentWorkflow_weatherAndReport() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-workflow?message=帮我查询北京天气，然后根据天气生成出行报告", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String answer = (String) response.getBody().get("answer");
        assertThat(answer).isNotNull().isNotEmpty();
        assertThat(answer).contains("【步骤");
    }

    @Test
    @Order(8)
    void testAgentWorkflow_companyAnalysis() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-workflow?message=查询测试科技公司信息然后做一份分析报告", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String answer = (String) response.getBody().get("answer");
        assertThat(answer).isNotNull().isNotEmpty();
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(9)
    void testAgentChat_defaultMessage() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-chat", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("question")).isNotNull();
        assertThat(response.getBody().get("answer")).isNotNull();
    }

    @Test
    @Order(10)
    void testAgentWorkflow_defaultMessage() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ai/agent-workflow", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("question")).isNotNull();
        assertThat(response.getBody().get("answer")).isNotNull();
    }
}
