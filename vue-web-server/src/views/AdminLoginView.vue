<template>
  <div class="admin-login">
    <div class="card">
      <h1 class="title">Sign in to Admin</h1>
      <p class="subtitle">Use your admin account to continue.</p>

      <form @submit.prevent="handleSubmit" class="form">
        <label class="field">
          <span>Username</span>
          <input v-model="form.username" type="text" autocomplete="username" required />
        </label>
        <label class="field">
          <span>Password</span>
          <input v-model="form.password" type="password" autocomplete="current-password" required />
        </label>

        <label class="remember">
          <input v-model="rememberPassword" type="checkbox" />
          <span>记住密码</span>
        </label>

        <button class="btn-primary" type="submit" :disabled="loading">
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>

        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { apiRequest } from '../apiClient'

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const error = ref('')
const rememberPassword = ref(false)

// 有 token 时先验证是否有效，无效则清除并留在登录页
onMounted(() => {
  const savedUser = localStorage.getItem('admin_remember_username')
  const savedPwd = localStorage.getItem('admin_remember_password')
  if (savedUser) form.username = savedUser
  if (savedPwd) {
    form.password = savedPwd
    rememberPassword.value = true
  }

  const token = localStorage.getItem('admin_token')
  const role = localStorage.getItem('admin_role')
  if (!token || role !== 'admin') return

  // 验证 token 是否有效
  apiRequest('/api/admin/users/stats', {
    headers: { 'X-Auth-Token': token }
  }).then(({ resp, json }) => {
    if (resp.status === 401 || resp.status === 403) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_role')
      return
    }
    if (json.code === 0) {
      router.push(route.query.redirect || '/admin')
    }
  }).catch(() => {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_role')
  })
})

const handleSubmit = async () => {
  error.value = ''
  loading.value = true
  try {
    const { json } = await apiRequest('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(form)
    })
    if (json.code !== 0) {
      throw new Error(json.message || '登录失败')
    }
    const data = json.data || {}
    if (data.role !== 'admin') {
      throw new Error('该账号不是管理员')
    }
    localStorage.setItem('admin_token', data.token || '')
    localStorage.setItem('admin_role', data.role || '')

    if (rememberPassword.value) {
      localStorage.setItem('admin_remember_username', form.username)
      localStorage.setItem('admin_remember_password', form.password)
    } else {
      localStorage.removeItem('admin_remember_username')
      localStorage.removeItem('admin_remember_password')
    }

    const redirect = route.query.redirect || '/admin'
    router.push(redirect)
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login {
  max-width: 420px;
  margin: 4rem auto 3rem;
}

.card {
  background: #ffffff;
  border-radius: 18px;
  border: 1px solid #d2d2d7;
  padding: 2.5rem 2.75rem 2.25rem;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.06);
}

.title {
  font-size: 1.7rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: #1d1d1f;
}

.subtitle {
  color: #6e6e73;
  margin-bottom: 1.6rem;
  font-size: 0.95rem;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  font-size: 0.875rem;
  color: #6e6e73;
}

input {
  padding: 0.7rem 0.75rem;
  border-radius: 10px;
  border: 1px solid #d2d2d7;
  font-size: 0.95rem;
  background: #f5f5f7;
  color: #1d1d1f;
}

input:focus {
  outline: none;
  border-color: #0071e3;
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
  background: #ffffff;
}

.remember {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #6e6e73;
  cursor: pointer;
}

.remember input[type="checkbox"] {
  width: auto;
  padding: 0;
  margin: 0;
}

.btn-primary {
  margin-top: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 999px;
  background: #0071e3;
  color: #ffffff;
  font-weight: 600;
  font-size: 0.95rem;
}

.btn-primary[disabled] {
  opacity: 0.6;
  cursor: default;
}

.error {
  margin-top: 0.75rem;
  color: #ff3b30;
  font-size: 0.875rem;
}

</style>

