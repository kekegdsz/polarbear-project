<template>
  <div class="app">
    <header class="top-bar">
      <div class="top-left">
        <span v-if="isInAdminArea" class="hello-user">Hello，{{ profileUsername || '管理员' }}</span>
        <nav v-else class="top-nav">
          <router-link to="/api-doc" class="top-link">API 文档</router-link>
          <router-link to="/json-tool" class="top-link">JSON 工具</router-link>
          <router-link to="/icon-maker" class="top-link">App Icon 制作</router-link>
        </nav>
      </div>
      <div class="top-right">
        <router-link
          v-if="!isInAdminArea"
          to="/admin/login"
          class="login-link"
        >
          Login
        </router-link>
        <template v-else>
          <button class="top-btn" @click="showPasswordModal = true">修改密码</button>
          <button class="top-btn logout-btn" @click="handleLogout">
            退出登录
          </button>
        </template>
      </div>
    </header>

    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordModal" class="modal-overlay" @click.self="showPasswordModal = false">
      <div class="modal">
        <h3>修改密码</h3>
        <form @submit.prevent="submitPassword" class="form">
          <div class="form-group">
            <label>旧密码 <span class="required">*</span></label>
            <input v-model="passwordForm.oldPassword" type="password" required placeholder="请输入旧密码" />
          </div>
          <div class="form-group">
            <label>新密码 <span class="required">*</span></label>
            <input v-model="passwordForm.newPassword" type="password" required placeholder="8-32 位" />
          </div>
          <div class="form-group">
            <label>确认新密码 <span class="required">*</span></label>
            <input v-model="passwordForm.confirmPassword" type="password" required placeholder="再次输入新密码" />
          </div>
          <p v-if="passwordError" class="form-error">{{ passwordError }}</p>
          <div class="form-actions">
            <button type="button" class="btn btn-ghost" @click="closePasswordModal">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="passwordLoading">
              {{ passwordLoading ? '提交中...' : '确定' }}
            </button>
          </div>
        </form>
      </div>
    </div>
    <main class="main">
      <div class="main-inner">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiRequest } from './apiClient'

const route = useRoute()
const router = useRouter()

const profileUsername = ref('')
const showPasswordModal = ref(false)
const passwordLoading = ref(false)
const passwordError = ref('')

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isInAdminArea = computed(() => {
  return route.path.startsWith('/admin') && route.name !== 'AdminLogin'
})

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || ''
})

const fetchProfile = async () => {
  try {
    const { resp, json } = await apiRequest('/api/admin/profile', {
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) {
      profileUsername.value = ''
      return
    }
    if (json.code === 0 && json.data) {
      profileUsername.value = json.data.username || ''
    }
  } catch {
    profileUsername.value = ''
  }
}

watch(
  () => [route.path, route.name],
  () => {
    if (isInAdminArea.value) {
      fetchProfile()
    } else {
      profileUsername.value = ''
    }
  },
  { immediate: true }
)

const closePasswordModal = () => {
  showPasswordModal.value = false
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordError.value = ''
}

const submitPassword = async () => {
  passwordError.value = ''
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword || !newPassword || !confirmPassword) {
    passwordError.value = '请填写完整'
    return
  }
  if (newPassword.length < 8 || newPassword.length > 32) {
    passwordError.value = '新密码需 8-32 位'
    return
  }
  if (newPassword !== confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  passwordLoading.value = true
  try {
    const { resp, json } = await apiRequest('/api/admin/profile/password', {
      method: 'PUT',
      headers: getAdminHeaders(),
      body: JSON.stringify({ oldPassword, newPassword })
    })
    if (resp.status === 401 || resp.status === 403) {
      return handleAuthFailed()
    }
    if (json.code !== 0) {
      throw new Error(json.message || '修改失败')
    }
    closePasswordModal()
  } catch (e) {
    passwordError.value = e.message || '修改失败'
  } finally {
    passwordLoading.value = false
  }
}

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin' } })
}

const handleLogout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  profileUsername.value = ''
  router.push('/admin/login')
}
</script>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  padding: 0 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  backdrop-filter: blur(20px);
  background: rgba(245, 245, 247, 0.9);
  border-bottom: 1px solid rgba(210, 210, 215, 0.7);
  z-index: 10;
}

.top-left {
  min-width: 0;
}

.top-nav {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.top-link {
  font-size: 0.95rem;
  font-weight: 500;
  color: #1d1d1f;
  text-decoration: none;
}

.top-link.router-link-active {
  color: #0071e3;
}

.top-link:hover {
  color: #0071e3;
}

.hello-user {
  font-size: 0.95rem;
  color: #1d1d1f;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.top-btn {
  background: none;
  border: none;
  font-size: 0.9rem;
  font-weight: 500;
  color: #1d1d1f;
  cursor: pointer;
  padding: 0;
}

.top-btn:hover {
  color: #0071e3;
}

.login-link {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1d1d1f;
}

.login-link:hover {
  color: #0071e3;
}

.main {
  flex: 1;
  padding: 0 1.5rem 4rem;
  padding-top: 72px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-inner {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.main-inner > * {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem 2rem;
  min-width: 360px;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.15);
}

.modal h3 {
  margin-bottom: 1.25rem;
  font-size: 1.1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 0.875rem;
  color: #6e6e73;
  margin-bottom: 0.35rem;
}

.form-group .required {
  color: #ff6b6b;
}

.form-group input {
  width: 100%;
  padding: 0.5rem 0.65rem;
  border-radius: 10px;
  border: 1px solid #d2d2d7;
  font-size: 0.9rem;
}

.form-error {
  color: #ff3b30;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
  border: none;
  cursor: pointer;
}

.btn-primary {
  background: #0071e3;
  color: #fff;
}

.btn-ghost {
  background: transparent;
  border: 1px solid #d2d2d7;
  color: #6e6e73;
}
</style>
