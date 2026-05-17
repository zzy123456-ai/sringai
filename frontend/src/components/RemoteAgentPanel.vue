<template>
  <div class="panel">
    <el-card header="注册远端智能体" class="form-card">
      <el-form :model="form" label-width="100px" @submit.prevent="handleRegister">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="智能体唯一名称" />
        </el-form-item>
        <el-form-item label="地址" required>
          <el-input v-model="form.url" placeholder="http://host:port/path" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3"
            placeholder="智能体能力描述，供大模型路由使用" />
        </el-form-item>
        <el-form-item label="能力标签">
          <el-input v-model="form.capabilitiesStr" placeholder="逗号分隔，如：回显, 翻译, 摘要" />
        </el-form-item>
        <el-form-item label="技能列表">
          <div class="skills-list">
            <div v-for="(skill, idx) in form.skills" :key="idx" class="skill-row">
              <el-input v-model="skill.id" placeholder="技能标识" style="width: 180px" />
              <el-input v-model="skill.description" placeholder="技能描述" style="width: 300px; margin-left: 8px" />
              <el-button type="danger" :icon="Delete" circle size="small" @click="removeSkill(idx)" style="margin-left: 8px" />
            </div>
            <el-button type="primary" size="small" @click="addSkill">+ 添加技能</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading">
            注册远端智能体
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card header="已注册的远端智能体" class="table-card">
      <el-table :data="agents" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="url" label="地址" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="技能数" width="80" align="center">
          <template #default="{ row }">
            {{ row.skills?.length ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.name)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && agents.length === 0" description="暂无已注册的远端智能体" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { fetchAgents, registerRemote, unregisterAgent } from '../api/agent'
import { ElMessage, ElMessageBox } from 'element-plus'

const agents = ref([])
const loading = ref(false)

const form = reactive({
  name: '',
  url: '',
  description: '',
  capabilitiesStr: '',
  skills: []
})

function addSkill() {
  form.skills.push({ id: '', description: '' })
}

function removeSkill(idx) {
  form.skills.splice(idx, 1)
}

function buildBody() {
  const body = {
    url: form.url,
    description: form.description
  }
  if (form.capabilitiesStr.trim()) {
    body.capabilities = form.capabilitiesStr.split(',').map(s => s.trim()).filter(Boolean)
  }
  const validSkills = form.skills.filter(s => s.id.trim())
  if (validSkills.length > 0) {
    body.skills = validSkills
  }
  return body
}

async function loadAgents() {
  loading.value = true
  try {
    const all = await fetchAgents()
    agents.value = all.filter(a => a.type === 'REMOTE_AGENT')
  } catch (e) {
    ElMessage.error('加载智能体列表失败：' + e.message)
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
    await registerRemote(form.name.trim(), buildBody())
    ElMessage.success('注册成功：' + form.name)
    form.name = ''
    form.url = ''
    form.description = ''
    form.capabilitiesStr = ''
    form.skills = []
    await loadAgents()
  } catch (e) {
    ElMessage.error('注册失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleDelete(name) {
  try {
    await ElMessageBox.confirm(`确认删除智能体"${name}"？`, '确认删除', { type: 'warning' })
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
