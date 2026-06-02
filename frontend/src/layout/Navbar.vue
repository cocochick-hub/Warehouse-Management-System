<template>
  <div class="navbar">
    <div class="navbar-left">
      <el-icon class="collapse-btn" :size="20" @click="emit('toggleCollapse')">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="breadcrumb">{{ breadcrumb }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="navbar-right">
      <el-badge :value="alertCount" :hidden="alertCount === 0" class="alert-badge">
        <el-icon :size="20" class="navbar-icon" @click="handleAlert">
          <WarningFilled />
        </el-icon>
      </el-badge>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="30" icon="UserFilled" />
          <span class="user-name">{{ userStore.userInfo?.realName || userStore.username }}</span>
          <el-tag :type="roleTagType" size="small" effect="dark" class="role-tag">
            {{ roleLabel }}
          </el-tag>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人信息
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Edit /></el-icon>修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

defineProps({
  isCollapse: Boolean
})

const emit = defineEmits(['toggleCollapse'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const breadcrumb = computed(() => route.meta?.title || '')

const alertCount = computed(() => 0)

const roleTagType = computed(() => {
  const map = { admin: 'danger', manager: 'warning', operator: 'info' }
  return map[userStore.role] || 'info'
})

const roleLabel = computed(() => {
  const map = { admin: '系统管理员', manager: '仓库经理', operator: '操作员' }
  return map[userStore.role] || userStore.role
})

function handleAlert() {
  router.push('/alert/threshold')
}

function handleCommand(command) {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    ElMessage.info('个人信息页面开发中')
  } else if (command === 'password') {
    ElMessage.info('修改密码功能开发中')
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {

  }
}
</script>

<style scoped>
.navbar {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #409eff;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.navbar-icon {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.navbar-icon:hover {
  color: #409eff;
}

.alert-badge :deep(.el-badge__content) {
  border: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.role-tag {
  margin-left: 4px;
}
</style>
