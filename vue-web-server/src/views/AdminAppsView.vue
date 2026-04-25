<template>
  <div>
    <header class="content-header">
      <div>
        <h1>应用管理</h1>
        <p class="subtitle">创建应用生成 AppId，并以卡片形式展示</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showCreate = true">创建应用</button>
        <button class="btn btn-ghost" @click="fetchApps" :disabled="loading">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>

    <section v-if="apps.length" class="cards">
      <article class="card" v-for="a in apps" :key="a.id">
        <div class="card-top">
          <div class="card-title">
            <span class="name">{{ a.name }}</span>
            <span class="time">{{ formatDate(a.createdAt) }}</span>
          </div>
          <div class="card-actions">
            <button class="btn-mini" @click="copy(a.appId)">复制 AppId</button>
          </div>
        </div>

        <div class="appid">
          <span class="label">AppId</span>
          <code class="code">{{ a.appId }}</code>
        </div>

        <p class="remark" v-if="a.remark">{{ a.remark }}</p>
        <p class="remark muted" v-else>无备注</p>
      </article>
    </section>
    <p v-else class="empty">{{ loading ? '加载中...' : '暂无应用，请先创建' }}</p>

    <div v-if="toast.show" class="toast" :class="toast.type">
      {{ toast.message }}
    </div>

    <!-- 创建应用弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="closeCreate">
      <div class="modal">
        <h3>创建应用</h3>
        <form @submit.prevent="submitCreate" class="form">
          <div class="form-group">
            <label>应用名称 <span class="required">*</span></label>
            <input v-model="form.name" required placeholder="例如：安卓端/IOS端/后台任务" />
          </div>
          <div class="form-group">
            <label>备注</label>
            <input v-model="form.remark" placeholder="可选" />
          </div>
          <p v-if="createError" class="form-error">{{ createError }}</p>
          <div v-if="createdAppId" class="created">
            <span class="created-label">已生成 AppId：</span>
            <code class="created-code">{{ createdAppId }}</code>
            <button type="button" class="btn-copy" @click="copy(createdAppId)">复制 AppId</button>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-ghost" @click="closeCreate">关闭</button>
            <button type="submit" class="btn btn-primary" :disabled="creating">
              {{ creating ? '创建中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const router = useRouter()
const apps = ref([])
const loading = ref(false)
const error = ref('')

const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const createdAppId = ref('')
const form = reactive({ name: '', remark: '' })
const toast = reactive({ show: false, message: '', type: 'success', timer: null })

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || ''
})

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin/apps' } })
}

const fetchApps = async () => {
  loading.value = true
  error.value = ''
  try {
    const { resp, json } = await apiRequest('/api/admin/apps/list', {
      method: 'POST',
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '获取应用列表失败')
    apps.value = json.data || []
  } catch (e) {
    error.value = e.message || '获取应用列表失败'
  } finally {
    loading.value = false
  }
}

const closeCreate = () => {
  showCreate.value = false
  creating.value = false
  createError.value = ''
  createdAppId.value = ''
  form.name = ''
  form.remark = ''
}

const submitCreate = async () => {
  creating.value = true
  createError.value = ''
  createdAppId.value = ''
  try {
    const { resp, json } = await apiRequest('/api/admin/apps', {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify({ name: form.name, remark: form.remark || null })
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '创建失败')
    createdAppId.value = json.data?.appId || ''
    await fetchApps()
  } catch (e) {
    createError.value = e.message || '创建失败'
  } finally {
    creating.value = false
  }
}

const copy = async (text) => {
  if (!text) return
  try {
    // 优先使用 Clipboard API（https/localhost 常可用）
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      showToast('已复制 AppId', 'success')
      return
    }
  } catch {
    // ignore and fallback
  }

  try {
    // 兼容 HTTP 场景的降级复制
    const input = document.createElement('textarea')
    input.value = text
    input.setAttribute('readonly', '')
    input.style.position = 'fixed'
    input.style.opacity = '0'
    input.style.pointerEvents = 'none'
    document.body.appendChild(input)
    input.focus()
    input.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(input)
    if (ok) {
      showToast('已复制 AppId', 'success')
      return
    }
  } catch {
    // ignore
  }

  showToast('复制失败，请长按手动复制', 'error')
}

const showToast = (message, type = 'success') => {
  toast.message = message
  toast.type = type
  toast.show = true
  if (toast.timer) clearTimeout(toast.timer)
  toast.timer = setTimeout(() => {
    toast.show = false
    toast.timer = null
  }, 1500)
}

const formatDate = (v) => {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString()
  } catch {
    return String(v)
  }
}

onMounted(() => {
  fetchApps()
})
</script>

<style scoped>
.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.subtitle {
  color: var(--color-text-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  border: none;
  cursor: pointer;
}

.btn-primary {
  background: var(--color-accent);
  color: #fff;
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
}

.card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e5e5ea;
  padding: 1rem 1.1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: flex-start;
  margin-bottom: 0.85rem;
}

.card-title {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.name {
  font-weight: 600;
  font-size: 1rem;
  color: #1d1d1f;
}

.time {
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.btn-mini {
  padding: 0.35rem 0.6rem;
  border-radius: 8px;
  border: 1px solid #d2d2d7;
  background: #fff;
  font-size: 0.82rem;
  cursor: pointer;
  color: #4b5563;
  white-space: nowrap;
}

.appid {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.65rem;
}

.label {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

code {
  font-size: 0.85em;
  background: #f0f0f5;
  padding: 0.15em 0.4em;
  border-radius: 4px;
}

.remark {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.45;
  color: #1d1d1f;
  word-break: break-word;
}

.muted {
  color: var(--color-text-muted);
}

.empty {
  padding: 1rem 0;
  color: var(--color-text-muted);
}

.error {
  margin-bottom: 0.5rem;
  color: #ff6b6b;
  font-size: 0.85rem;
}

.toast {
  position: fixed;
  left: 50%;
  top: 22%;
  transform: translate(-50%, -50%);
  z-index: 1200;
  padding: 0.75rem 1.1rem;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(17, 24, 39, 0.92);
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.25);
  color: #ffffff;
  backdrop-filter: blur(10px);
  min-width: 220px;
  text-align: center;
}

.toast.success {
  outline: 2px solid rgba(46, 125, 50, 0.35);
}

.toast.error {
  outline: 2px solid rgba(255, 59, 48, 0.35);
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
  border-radius: var(--radius);
  padding: 1.5rem 2rem;
  min-width: 460px;
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
  color: var(--color-text-muted);
  margin-bottom: 0.35rem;
}

.form-group .required {
  color: #ff6b6b;
}

.form-group input {
  width: 100%;
  padding: 0.5rem 0.65rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  font-size: 0.9rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.form-error {
  color: #ff6b6b;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.created {
  background: #f5f5f7;
  border: 1px dashed #d2d2d7;
  border-radius: var(--radius);
  padding: 0.65rem 0.75rem;
  margin: 0.75rem 0 0.25rem;
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.created-label {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.created-code {
  font-size: 0.9rem;
}

.btn-copy {
  padding: 0.2rem 0.55rem;
  border: 1px solid #d2d2d7;
  background: #fff;
  border-radius: 6px;
  font-size: 0.8rem;
  color: #4b5563;
  cursor: pointer;
}
</style>

