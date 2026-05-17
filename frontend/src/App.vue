<template>
  <div class="app-container">
    <header class="app-header">
      <h1>SpringAI 智能体管理平台</h1>
      <p>注册和管理远端智能体、MCP工具、RAG知识库和REST接口</p>
    </header>

    <el-tabs v-model="activeTab" type="border-card" class="main-tabs">
      <el-tab-pane label="远端智能体" name="remote">
        <RemoteAgentPanel />
      </el-tab-pane>
      <el-tab-pane label="MCP工具" name="mcp">
        <McpToolPanel />
      </el-tab-pane>
      <el-tab-pane label="RAG知识库" name="rag">
        <RagKbPanel />
      </el-tab-pane>
      <el-tab-pane label="REST接口" name="rest">
        <RestApiPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import RemoteAgentPanel from './components/RemoteAgentPanel.vue'
import McpToolPanel from './components/McpToolPanel.vue'
import RagKbPanel from './components/RagKbPanel.vue'
import RestApiPanel from './components/RestApiPanel.vue'

const activeTab = ref('remote')
</script>
