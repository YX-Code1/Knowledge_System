<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">知识库系统</h2>
      <p class="login-subtitle">重置密码</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱" prefix-icon="Message" size="large" />
        </el-form-item>
        <el-form-item prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="验证码" prefix-icon="Key" size="large" maxlength="6" />
            <el-button size="large" class="code-btn" :loading="sending" :disabled="countdown > 0" @click="handleSendCode">
              {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="新密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleReset">重置密码</el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>想起密码了？</span>
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendResetCode, resetPassword } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({ username: '', email: '', code: '', password: '', confirmPassword: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleSendCode() {
  const valid = await formRef.value.validateField(['username', 'email']).catch(() => false)
  if (!valid) return

  sending.value = true
  try {
    await sendResetCode({ username: form.username, email: form.email })
    ElMessage.success('验证码已发送，请查收邮件')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e) {
    // 账号与邮箱不匹配时，后端会提示"这个邮箱貌似不是您的呢喵~"，由拦截器弹出
  } finally {
    sending.value = false
  }
}

async function handleReset() {
  await formRef.value.validate()
  loading.value = true
  try {
    await resetPassword({
      username: form.username,
      email: form.email,
      code: form.code,
      password: form.password
    })
    ElMessage.success('密码重置成功，请登录')
    router.push('/login')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}
.login-subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 30px;
  font-size: 14px;
}
.login-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
}
.login-footer a {
  color: #667eea;
  margin-left: 4px;
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.code-row .el-input {
  flex: 1;
}
.code-row .code-btn {
  width: 120px;
  flex-shrink: 0;
}
</style>
