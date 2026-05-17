<template>
  <div class="panel">
    <el-card header="注册 MCP 工具" class="form-card">
      <el-form :model="form" label-width="140px" @submit.prevent="handleRegister">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="工具唯一名称" />
        </el-form-item>
        <el-form-item label="MCP服务端地址" required>
          <el-input v-model="form.mcpServerUrl" placeholder="http://host:port/mcp/jsonrpc" />
        </el-form-item>
        <el-form-item label="工具名称" required>
          <el-input v-model="form.toolName" placeholder="如：weather_query" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3"
            placeholder="工具描述，供大模型进行参数提取" />
        </el-form-item>
        <el-form-item label="输入参数定义(JSON)">
          <el-input v-model="form.inputSchemaStr" type="textarea" :rows="4"
            placeholder='{"type":"object","properties":{"city":{"type":"string","description":"城市名称"}}}' />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading">
            注册 MCP 工具
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card header="已注册的 MCP 工具" class="table-card">
      <el-table :data="agents" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="mcpServerUrl" label="MCP服务端地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="toolName" label="工具名称" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.name)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && agents.length === 0" description="暂无已注册的 MCP 工具" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { fetchAgents, registerMcp, unregisterAgent } from '../api/agent'
import { ElMessage, ElMessageBox } from 'element-plus'

const agents = ref([])
const loading = ref(false)

const form = reactive({
  name: '',
  mcpServerUrl: '',
  toolName: '',
  description: '',
  inputSchemaStr: ''
})

function buildBody() {
  const body = {
    mcpServerUrl: form.mcpServerUrl,
    toolName: form.toolName,
    description: form.description
  }
  if (form.inputSchemaStr.trim()) {
    try {
      body.inputSchema = JSON.parse(form.inputSchemaStr)
    } catch {
      ElMessage.warning('输入参数定义不是有效的 JSON 格式')
      throw new Error('Invalid JSON')
    }
  }
  return body
}

async function loadAgents() {
  loading.value = true
  try {
    const all = await fetchAgents()
    agents.value = all.filter(a => a.type === 'MCP_TOOL')
  } catch (e) {
    ElMessage.error('加载 MCP 工具列表失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!form.name.trim() || !form.mcpServerUrl.trim() || !form.toolName.trim()) {
    ElMessage.warning('请填写名称、MCP服务端地址和工具名称')
    return
  }
  loading.value = true
  try {
    await registerMcp(form.name.trim(), buildBody())
    ElMessage.success('注册成功：' + form.name)
    form.name = ''
    form.mcpServerUrl = ''
    form.toolName = ''
    form.description = ''
    form.inputSchemaStr = ''
    await loadAgents()
  } catch (e) {
    if (e.message !== 'Invalid JSON') {
      ElMessage.error('注册失败：' + e.message)
    }
  } finally {
    loading.value = false
  }
}

async function handleDelete(name) {
  try {
    await ElMessageBox.confirm(`确认删除工具"${name}"？`, '确认删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    await unregisterAgent(name)
    ElMessage.success('已删除：' + name)
    await loadAgents()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

onMounted(loadAgents)
</script>
