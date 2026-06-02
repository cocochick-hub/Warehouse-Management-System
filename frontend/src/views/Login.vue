<template>
  <div class="login-container">
    <!-- 背景动画：行驶的小车 -->
    <div class="bg-animation">
      <!-- 小车 1（远景，小尺寸，慢速） -->
      <div class="truck truck-1">
        <div class="truck-body">
          <div class="truck-cargo">
            <div class="cargo-box box-1"></div>
            <div class="cargo-box box-2"></div>
            <div class="cargo-box box-3"></div>
          </div>
        </div>
        <div class="truck-cab"></div>
        <div class="wheel wheel-left"></div>
        <div class="wheel wheel-right"></div>
      </div>

      <!-- 小车 2（中景） -->
      <div class="truck truck-2">
        <div class="truck-body">
          <div class="truck-cargo">
            <div class="cargo-box box-1"></div>
            <div class="cargo-box box-2"></div>
          </div>
        </div>
        <div class="truck-cab"></div>
        <div class="wheel wheel-left"></div>
        <div class="wheel wheel-right"></div>
      </div>

      <!-- 小车 3（近景，大尺寸，快速） -->
      <div class="truck truck-3">
        <div class="truck-body">
          <div class="truck-cargo">
            <div class="cargo-box box-1"></div>
            <div class="cargo-box box-2"></div>
            <div class="cargo-box box-3"></div>
            <div class="cargo-box box-4"></div>
          </div>
        </div>
        <div class="truck-cab"></div>
        <div class="wheel wheel-left"></div>
        <div class="wheel wheel-right"></div>
      </div>

      <!-- 漂浮的货物箱（散落在背景中） -->
      <div class="floating-box fb-1"></div>
      <div class="floating-box fb-2"></div>
      <div class="floating-box fb-3"></div>
      <div class="floating-box fb-4"></div>
      <div class="floating-box fb-5"></div>
      <div class="floating-box fb-6"></div>

      <!-- 仓库货架剪影 -->
      <div class="shelf shelf-left"></div>
      <div class="shelf shelf-right"></div>
    </div>

    <!-- 背景装饰 -->
    <div class="login-bg"></div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- Logo 与标题 -->
      <div class="login-header">
        <el-icon class="login-logo" :size="48" color="#409EFF">
          <Box />
        </el-icon>
        <h1 class="login-title">WMS 仓库管理系统</h1>
        <p class="login-subtitle">汽车物流仓储管理平台</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            type="password"
            show-password
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部提示 -->
      <div class="login-footer">
        <span>预置账号：admin / operator / manager</span>
        <span>密码统一为：admin123</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Box } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

// 登录表单数据
const form = reactive({
  username: '',
  password: ''
})

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 50, message: '密码长度为 4-50 个字符', trigger: 'blur' }
  ]
}

/**
 * 处理登录
 */
async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #1a2a6c 0%, #2d4379 25%, #667eea 50%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

/* ==================== 背景装饰圆 ==================== */
.login-bg {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
  top: -200px;
  right: -200px;
  z-index: 0;
}

.login-bg::after {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.06) 0%, transparent 70%);
  bottom: -350px;
  left: -250px;
}

/* ==================== 背景动画容器 ==================== */
.bg-animation {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* ==================== 小车样式 ==================== */
.truck {
  position: absolute;
  display: flex;
  align-items: flex-end;
  animation: drive linear infinite;
}

/* 车厢 */
.truck-body {
  width: 70%;
  height: 100%;
  background: linear-gradient(180deg, #5dade2 0%, #2e86c1 100%);
  border-radius: 3px 0 0 3px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 车厢条纹装饰 */
.truck-body::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(255,255,255,0.3);
}

.truck-body::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: rgba(0,0,0,0.15);
}

/* 车厢内的货物 */
.truck-cargo {
  display: flex;
  gap: 2px;
  align-items: flex-end;
  padding: 2px;
  width: 100%;
  height: 100%;
  justify-content: center;
}

.cargo-box {
  background: linear-gradient(135deg, #f39c12, #e67e22);
  border-radius: 1px;
  border: 1px solid rgba(0,0,0,0.1);
}

/* 驾驶室 */
.truck-cab {
  width: 25%;
  height: 65%;
  background: linear-gradient(180deg, #34495e 0%, #2c3e50 100%);
  border-radius: 0 4px 4px 0;
  position: relative;
}

/* 驾驶室车窗 */
.truck-cab::before {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  right: 3px;
  height: 35%;
  background: linear-gradient(180deg, #aed6f1, #85c1e9);
  border-radius: 1px;
  border: 1px solid rgba(0,0,0,0.15);
}

/* 车轮 */
.wheel {
  position: absolute;
  bottom: -4px;
  width: 12%;
  height: 8px;
  background: #2c3e50;
  border-radius: 50%;
  border: 1px solid #1a252f;
}

.wheel::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 3px;
  height: 3px;
  background: #7f8c8d;
  border-radius: 50%;
}

.wheel-left { left: 18%; }
.wheel-right { right: 8%; }

/* 小车 1 —— 远景（小，慢速） */
.truck-1 {
  width: 80px;
  height: 28px;
  top: 12%;
  animation-duration: 22s;
  animation-delay: -5s;
  opacity: 0.35;
}

.truck-1 .cargo-box { height: 60%; }
.truck-1 .box-1 { width: 25%; height: 70%; }
.truck-1 .box-2 { width: 20%; height: 85%; }
.truck-1 .box-3 { width: 22%; height: 65%; }

/* 小车 2 —— 中景 */
.truck-2 {
  width: 110px;
  height: 36px;
  top: 38%;
  animation-duration: 16s;
  animation-delay: -12s;
  opacity: 0.5;
}

.truck-2 .cargo-box { height: 65%; }
.truck-2 .box-1 { width: 28%; height: 75%; }
.truck-2 .box-2 { width: 30%; height: 90%; }

/* 小车 3 —— 近景（大，快速） */
.truck-3 {
  width: 150px;
  height: 48px;
  top: 68%;
  animation-duration: 12s;
  animation-delay: -3s;
  opacity: 0.7;
}

.truck-3 .cargo-box { height: 68%; }
.truck-3 .box-1 { width: 20%; height: 70%; }
.truck-3 .box-2 { width: 18%; height: 85%; }
.truck-3 .box-3 { width: 20%; height: 60%; }
.truck-3 .box-4 { width: 16%; height: 90%; }

/* 小车行驶动画 */
@keyframes drive {
  0% { transform: translateX(-200px); }
  100% { transform: translateX(calc(100vw + 200px)); }
}

/* ==================== 漂浮的货物箱 ==================== */
.floating-box {
  position: absolute;
  background: linear-gradient(135deg, rgba(243,156,18,0.25), rgba(230,126,34,0.2));
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 3px;
  animation: floatBox ease-in-out infinite;
}

.fb-1 {
  width: 35px;
  height: 35px;
  top: 15%;
  left: 10%;
  animation-duration: 4s;
  animation-delay: 0s;
  transform: rotate(15deg);
}

.fb-2 {
  width: 25px;
  height: 30px;
  top: 25%;
  right: 15%;
  animation-duration: 5s;
  animation-delay: -1.5s;
  transform: rotate(-10deg);
}

.fb-3 {
  width: 45px;
  height: 35px;
  top: 55%;
  left: 5%;
  animation-duration: 4.5s;
  animation-delay: -3s;
  transform: rotate(25deg);
}

.fb-4 {
  width: 20px;
  height: 25px;
  top: 70%;
  right: 10%;
  animation-duration: 3.8s;
  animation-delay: -2s;
  transform: rotate(-20deg);
}

.fb-5 {
  width: 30px;
  height: 20px;
  top: 40%;
  left: 20%;
  animation-duration: 5.5s;
  animation-delay: -4s;
  transform: rotate(8deg);
}

.fb-6 {
  width: 28px;
  height: 32px;
  top: 10%;
  right: 25%;
  animation-duration: 4.2s;
  animation-delay: -0.5s;
  transform: rotate(-5deg);
}

/* 货物箱子上加标签条纹 */
.fb-1::before, .fb-2::before, .fb-3::before,
.fb-4::before, .fb-5::before, .fb-6::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 10%;
  right: 10%;
  height: 2px;
  background: rgba(255,255,255,0.2);
  transform: translateY(-50%);
}

.fb-1::after, .fb-2::after, .fb-3::after,
.fb-4::after, .fb-5::after, .fb-6::after {
  content: '';
  position: absolute;
  top: 30%;
  bottom: 30%;
  left: 50%;
  width: 2px;
  background: rgba(255,255,255,0.15);
  transform: translateX(-50%);
}

/* 漂浮动画 */
@keyframes floatBox {
  0%, 100% { transform: translateY(0px) rotate(var(--rot, 15deg)); }
  50% { transform: translateY(-12px) rotate(var(--rot, 15deg)); }
}

.fb-1 { --rot: 15deg; }
.fb-2 { --rot: -10deg; }
.fb-3 { --rot: 25deg; }
.fb-4 { --rot: -20deg; }
.fb-5 { --rot: 8deg; }
.fb-6 { --rot: -5deg; }

/* ==================== 仓库货架剪影 ==================== */
.shelf {
  position: absolute;
  bottom: 0;
  width: 120px;
  height: 200px;
  opacity: 0.08;
  z-index: 0;
}

.shelf-left {
  left: 0;
}

.shelf-right {
  right: 0;
}

/* 货架横梁 */
.shelf::before {
  content: '';
  position: absolute;
  left: 10%;
  right: 10%;
  height: 3px;
  background: #fff;
  box-shadow:
    0 40px 0 #fff,
    0 80px 0 #fff,
    0 120px 0 #fff,
    0 160px 0 #fff;
}

/* 货架竖柱 */
.shelf::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 15%;
  width: 3px;
  background: #fff;
  box-shadow:
    50px 0 0 #fff,
    85px 0 0 #fff;
}

/* 货架上的货物方块 */
.shelf-left::before {
  box-shadow:
    0 40px 0 #fff,
    0 80px 0 #fff,
    0 120px 0 #fff,
    0 160px 0 #fff,
    /* 层 1 的货物 */
    22px 10px 0 8px rgba(255,255,255,0.5),
    42px 15px 0 6px rgba(255,255,255,0.4),
    62px 8px 0 10px rgba(255,255,255,0.45),
    /* 层 2 的货物 */
    25px 50px 0 6px rgba(255,255,255,0.4),
    45px 48px 0 8px rgba(255,255,255,0.5),
    65px 52px 0 5px rgba(255,255,255,0.35),
    /* 层 3 的货物 */
    20px 90px 0 10px rgba(255,255,255,0.45),
    50px 92px 0 7px rgba(255,255,255,0.4),
    /* 层 4 的货物 */
    28px 130px 0 8px rgba(255,255,255,0.5),
    55px 128px 0 6px rgba(255,255,255,0.35),
    72px 132px 0 5px rgba(255,255,255,0.4);
}

.shelf-right::before {
  box-shadow:
    0 40px 0 #fff,
    0 80px 0 #fff,
    0 120px 0 #fff,
    0 160px 0 #fff,
    20px 12px 0 8px rgba(255,255,255,0.5),
    45px 8px 0 6px rgba(255,255,255,0.4),
    20px 50px 0 7px rgba(255,255,255,0.45),
    48px 52px 0 9px rgba(255,255,255,0.35),
    25px 90px 0 6px rgba(255,255,255,0.5),
    55px 88px 0 8px rgba(255,255,255,0.4),
    22px 130px 0 9px rgba(255,255,255,0.45),
    52px 132px 0 5px rgba(255,255,255,0.35);
}

/* ==================== 登录卡片 ==================== */
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  margin-bottom: 12px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #909399;
}

.login-form {
  margin-top: 8px;
}

.login-btn {
  width: 100%;
  font-size: 16px;
  letter-spacing: 4px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.8;
}
</style>