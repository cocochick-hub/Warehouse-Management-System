<template>
  <PageContainer title="用户权限管理">
    <template #actions>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>新增用户
      </el-button>
    </template>

    <el-alert
      class="permission-tip"
      type="info"
      show-icon
      :closable="false"
      title="权限说明"
      description="admin 可维护用户和权限，manager 可查看管理类数据，operator 负责日常扫码、入库、出库、转包等操作。"
    />

    <el-table v-loading="loading" :data="users" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="realName" label="姓名" min-width="120">
        <template #default="{ row }">{{ row.realName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="190">
        <template #default="{ row }">
          <el-select
            v-model="row.role"
            size="small"
            :disabled="row.username === currentUsername"
            @change="(role) => handleRoleChange(row, role)"
          >
            <el-option label="管理员 admin" value="admin" />
            <el-option label="仓库经理 manager" value="manager" />
            <el-option label="操作员 operator" value="operator" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
            inline-prompt
            :disabled="row.username === currentUsername"
            @change="(status) => handleStatusChange(row, status)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增用户" width="460px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录账号" clearable />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员 admin" value="admin" />
            <el-option label="仓库经理 manager" value="manager" />
            <el-option label="操作员 operator" value="operator" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="可选" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { createUserApi, listUsersApi, updateUserRoleApi, updateUserStatusApi } from '@/api/adminUser'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentUsername = computed(() => userStore.username)

const loading = ref(false)
const submitting = ref(false)
const users = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'operator',
  phone: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(fetchUsers)

async function fetchUsers() {
  loading.value = true
  try {
    const { data } = await listUsersApi()
    users.value = data || []
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  form.username = ''
  form.password = ''
  form.realName = ''
  form.role = 'operator'
  form.phone = ''
  dialogVisible.value = true
}

async function handleCreate() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await createUserApi({
      username: form.username,
      password: form.password,
      realName: form.realName || undefined,
      role: form.role,
      phone: form.phone || undefined
    })
    ElMessage.success('用户创建成功')
    dialogVisible.value = false
    fetchUsers()
  } finally {
    submitting.value = false
  }
}

async function handleRoleChange(row, role) {
  try {
    await ElMessageBox.confirm(`确认将 ${row.username} 的角色改为 ${role}？`, '权限变更确认', { type: 'warning' })
    await updateUserRoleApi(row.id, role)
    ElMessage.success('角色已更新')
  } catch {
    fetchUsers()
  }
}

async function handleStatusChange(row, status) {
  try {
    await ElMessageBox.confirm(`确认${status === 1 ? '启用' : '禁用'}账号 ${row.username}？`, '账号状态确认', { type: 'warning' })
    await updateUserStatusApi(row.id, status)
    ElMessage.success('状态已更新')
  } catch {
    fetchUsers()
  }
}

function formatDateTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
</script>

<style scoped>
.permission-tip {
  margin-bottom: 16px;
}
</style>
