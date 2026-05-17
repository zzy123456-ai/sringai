<template>
  <div class="panel">
    <el-card header="注册 RAG 知识库" class="form-card">
      <el-form :model="form" label-width="100px" @submit.prevent="handleRegister">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="知识库唯一名称" />
        </el-form-item>
        <el-form-item label="知识组ID" required>
          <el-input v-model="form.groupId" placeholder="知识组唯一标识" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3"
            placeholder="知识库内容描述，供大模型路由使用" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading">
            注册 RAG 知识库
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card header="已注册的 RAG 知识库" class="table-card">
      <el-table :data="agents" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="groupId" label="知识组ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.name)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && agents.length === 0" description="暂无已注册的 RAG 知识库" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { fetchAgents, registerRag, unregisterAgent } from '../api/agent'
import { ElMessage, ElMessageBox } from 'element-plus'

const agents = ref([])
const loading = ref(false)

const form = reactive({
  name: '',
  groupId: '',
  description: ''
})

function buildBody() {
  return {
    groupId: form.groupId,
    description: form.description
  }
}

async function loadAgents() {
  loading.value = true
  try {
    const all = await fetchAgents()
    agents.value = all.filter(a => a.type === 'RAG_KB')
  } catch (e) {
    ElMessage.error('加载知识库列表失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!form.name.trim() || !form.groupId.trim()) {
    ElMessage.warning('请填写名称和知识组ID')
    return
  }
  loading.value = true
  try {
    await registerRag(form.name.trim(), buildBody())
    ElMessage.success('注册成功：' + form.name)
    form.name = ''
    form.groupId = ''
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
    await ElMessageBox.confirm(`确认删除知识库"${name}"？`, '确认删除', { type: 'warning' })
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
