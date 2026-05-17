<template>
  <div class="panel">
    <el-card header="注册 REST 接口" class="form-card">
      <el-form :model="form" label-width="100px" @submit.prevent="handleRegister">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="接口唯一名称" />
        </el-form-item>
        <el-form-item label="地址" required>
          <el-input v-model="form.url" placeholder="http://host:port/api/resource" />
        </el-form-item>
        <el-form-item label="请求方法">
          <el-radio-group v-model="form.method">
            <el-radio value="GET">GET</el-radio>
            <el-radio value="POST">POST</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3"
            placeholder="接口描述，供大模型进行参数提取" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading">
            注册 REST 接口
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card header="已注册的 REST 接口" class="table-card">
      <el-table :data="agents" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="url" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="method" label="请求方法" width="90" align="center" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.name)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && agents.length === 0" description="暂无已注册的 REST 接口" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { fetchAgents, registerRest, unregisterAgent } from '../api/agent'
import { ElMessage, ElMessageBox } from 'element-plus'

const agents = ref([])
const loading = ref(false)

const form = reactive({
  name: '',
  url: '',
  method: 'GET',
  description: ''
})

function buildBody() {
  return {
    url: form.url,
    method: form.method,
    description: form.description
  }
}

async function loadAgents() {
  loading.value = true
  try {
    const all = await fetchAgents()
    agents.value = all.filter(a => a.type === 'REST_API')
  } catch (e) {
    ElMessage.error('加载接口列表失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!form.name.trim() || !form.url.trim()) {
    ElMessage.warning('请填写名称和地址')
    return
  }
  loading.value = true
  try {
    await registerRest(form.name.trim(), buildBody())
    ElMessage.success('注册成功：' + form.name)
    form.name = ''
    form.url = ''
    form.method = 'GET'
    form.description = ''
    await loadAgents()
  } catch (e) {
    ElMessage.error('注册失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleDelete(name) {
  try {
    await ElMessageBox.confirm(`确认删除接口"${name}"？`, '确认删除', { type: 'warning' })
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
