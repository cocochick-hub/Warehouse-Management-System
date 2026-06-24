<template>
  <div class="profile-container">
    <el-row :gutter="24">
      <el-col :span="10">
        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">个人信息</span>
          </template>
          <div class="profile-header">
            <el-avatar :size="96" :src="userInfo?.avatar || ''" class="profile-avatar">
              {{ displayName.charAt(0) }}
            </el-avatar>
            <div class="profile-meta">
              <h2 class="profile-name">{{ userInfo?.realName || userStore.username }}</h2>
              <el-tag :type="roleTagType" size="default" effect="dark" class="profile-role">
                {{ roleLabel }}
              </el-tag>
            </div>
          </div>
          <el-divider />
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">用户名：</span>
              <span class="info-value">{{ userStore.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">角色：</span>
              <span class="info-value">{{ roleLabel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话：</span>
              <div class="info-value editable-field">
                <el-input
                  v-if="phoneEditing"
                  ref="phoneInputRef"
                  v-model="phoneValue"
                  placeholder="输入联系电话"
                  size="small"
                  style="width: 180px"
                  @keyup.enter="handleSavePhone"
                  @blur="handleSavePhone"
                />
                <template v-else>
                  <span>{{ userInfo?.phone || '未设置' }}</span>
                  <el-button link type="primary" size="small" @click="startEditPhone" class="edit-btn">
                    <el-icon><Edit /></el-icon>修改
                  </el-button>
                </template>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never" class="password-card">
          <template #header>
            <span class="card-title">修改密码</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="password-form">
            <el-form-item label="用户名">
              <el-input :model-value="userStore.username" disabled />
            </el-form-item>
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原始密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleChangePassword" class="submit-btn">
                {{ loading ? '修改中...' : '确定修改' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { changePasswordApi, updateUserInfoApi } from '@/api/auth'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const phoneEditing = ref(false)
const phoneInputRef = ref(null)
const phoneValue = ref('')

const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => userInfo.value?.realName || userStore.username || 'U')

const roleTagType = computed(() => {
  const map = { admin: 'danger', manager: 'warning', operator: 'info' }
  return map[userStore.role] || 'info'
})

const roleLabel = computed(() => {
  const map = { admin: '系统管理员', manager: '仓库经理', operator: '操作员' }
  return map[userStore.role] || userStore.role
})

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [
    { required: true, message: '请输入原始密码', trigger: 'blur' },
    { min: 4, max: 50, message: '密码长度为 4-50 个字符', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 4, max: 50, message: '密码长度为 4-50 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

function startEditPhone() {
  phoneValue.value = userInfo.value?.phone || ''
  phoneEditing.value = true
  nextTick(() => {
    phoneInputRef.value?.focus()
  })
}

async function handleSavePhone() {
  const newPhone = (phoneValue.value || '').trim()
  if (newPhone === (userInfo.value?.phone || '')) {
    phoneEditing.value = false
    return
  }
  try {
    const { data } = await updateUserInfoApi({ phone: newPhone })
    userStore.setUserInfo(data)
    ElMessage.success('联系电话更新成功')
    phoneEditing.value = false
  } catch {
    phoneEditing.value = false
  }
}

async function handleChangePassword() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await changePasswordApi(form.oldPassword, form.newPassword)
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value.resetFields()
  } catch (err) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-container { padding: 20px; max-width: 960px; margin: 0 auto; }
.info-card, .password-card { border-radius: 8px; }
.card-title { font-size: 16px; font-weight: 600; color: #303133; }
.profile-header { display: flex; align-items: center; gap: 20px; padding: 8px 0; }
.profile-avatar { flex-shrink: 0; background: #409eff; font-size: 36px; color: #fff; }
.profile-meta { flex: 1; }
.profile-name { font-size: 22px; font-weight: 600; color: #303133; margin: 0 0 8px 0; }
.profile-role { font-size: 13px; }
.info-list { padding: 0 4px; }
.info-item { display: flex; padding: 12px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.info-item:last-child { border-bottom: none; }
.info-label { color: #909399; width: 100px; flex-shrink: 0; }
.info-value { color: #303133; font-weight: 500; }
.editable-field { display: flex; align-items: center; gap: 8px; }
.edit-btn { font-size: 12px; }
.password-form { max-width: 420px; padding: 12px 0; }
.submit-btn { width: 100%; margin-top: 8px; }
</style>
