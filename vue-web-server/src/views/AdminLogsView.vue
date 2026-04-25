<template>
  <div>
    <header class="content-header">
      <div>
        <h1>日志管理</h1>
        <p class="subtitle">按应用区分上传日志（appId），倒序展示，支持标记已读</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showCreateApp = true">创建应用</button>
        <button class="btn btn-ghost" @click="fetchList" :disabled="loading">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </header>

    <section class="apps-section">
      <div class="apps-row">
        <label class="apps-label">应用</label>
        <select v-model="selectedAppId" class="apps-select" @change="onAppChange">
          <option value="">请选择应用</option>
          <option v-for="a in apps" :key="a.id" :value="a.appId">
            {{ a.name }}（{{ a.appId }}）
          </option>
        </select>
        <button class="btn btn-ghost" @click="fetchApps" :disabled="appsLoading">
          {{ appsLoading ? '加载中...' : '刷新应用列表' }}
        </button>
      </div>
      <p class="apps-hint">
        说明：日志接口需要携带 appId；这里选择应用后再拉取日志/标记已读。
      </p>
    </section>

    <section class="list-section">
      <div v-if="error" class="error">{{ error }}</div>

      <table class="table" v-if="list.length">
        <thead>
          <tr>
            <th style="width: 90px">ID</th>
            <th style="width: 220px">AppId</th>
            <th>内容</th>
            <th style="width: 120px">状态</th>
            <th style="width: 190px">创建时间</th>
            <th style="width: 140px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td><code>{{ row.appId || '-' }}</code></td>
            <td class="content-cell">{{ row.content }}</td>
            <td>
              <span :class="['badge', row.ack ? 'badge-success' : 'badge-warn']">
                {{ row.ack ? '已读' : '未读' }}
              </span>
            </td>
            <td>{{ formatDate(row.createdAt) }}</td>
            <td>
              <button
                v-if="!row.ack"
                class="btn-action btn-ack"
                @click="ack(row)"
                :disabled="actioning === row.id"
              >
                {{ actioning === row.id ? '...' : '标记已读' }}
              </button>
              <span v-else class="muted">-</span>
            </td>
          </tr>
        </tbody>
      </table>

      <p v-else class="empty">{{ loading ? '加载中...' : '暂无日志' }}</p>

      <div class="pager">
        <button class="btn-small" :disabled="page === 1 || loading" @click="changePage(page - 1)">
          上一页
        </button>
        <span>第 {{ page }} 页 / 共 {{ totalPage }} 页（{{ total }} 条）</span>
        <button class="btn-small" :disabled="page >= totalPage || loading" @click="changePage(page + 1)">
          下一页
        </button>
      </div>
    </section>

    <!-- 创建应用弹窗 -->
    <div v-if="showCreateApp" class="modal-overlay" @click.self="closeCreateApp">
      <div class="modal">
        <h3>创建应用</h3>
        <form @submit.prevent="submitCreateApp" class="form">
          <div class="form-group">
            <label>应用名称 <span class="required">*</span></label>
            <input v-model="appForm.name" required placeholder="例如：安卓端/IOS端/后台任务" />
          </div>
          <div class="form-group">
            <label>备注</label>
            <input v-model="appForm.remark" placeholder="可选" />
          </div>
          <p v-if="createAppError" class="form-error">{{ createAppError }}</p>
          <div v-if="createdAppId" class="created">
            <span class="created-label">已生成 AppId：</span>
            <code class="created-code">{{ createdAppId }}</code>
            <button type="button" class="btn-copy" @click="copyCreatedAppId">复制 AppId</button>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-ghost" @click="closeCreateApp">关闭</button>
            <button type="submit" class="btn btn-primary" :disabled="creatingApp">
              {{ creatingApp ? '创建中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const router = useRouter()
const list = ref([])
const apps = ref([])
const selectedAppId = ref('')
const loading = ref(false)
const error = ref('')
const actioning = ref(null)
const appsLoading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const showCreateApp = ref(false)
const creatingApp = ref(false)
const createAppError = ref('')
const createdAppId = ref('')
const appForm = reactive({ name: '', remark: '' })

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || ''
})

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin/orders/logs' } })
}

const fetchApps = async () => {
  appsLoading.value = true
  try {
    const { resp, json } = await apiRequest('/api/admin/apps/list', {
      method: 'POST',
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '获取应用列表失败')
    apps.value = json.data || []
    if (!selectedAppId.value && apps.value.length) {
      selectedAppId.value = apps.value[0].appId
    }
  } catch (e) {
    error.value = e.message || '获取应用列表失败'
  } finally {
    appsLoading.value = false
  }
}

const fetchList = async () => {
  if (!selectedAppId.value) {
    list.value = []
    total.value = 0
    return
  }
  loading.value = true
  error.value = ''
  try {
    const { json } = await apiRequest('/api/admin/logs/unread', {
      method: 'POST',
      body: JSON.stringify({
        appId: selectedAppId.value,
        unreadOnly: false,
        page: page.value,
        size: size.value
      })
    })
    if (json.code !== 0) throw new Error(json.message || '获取日志失败')
    total.value = json.data?.total || 0
    list.value = json.data?.list || []
  } catch (e) {
    error.value = e.message || '获取日志失败'
  } finally {
    loading.value = false
  }
}

const totalPage = computed(() => {
  const t = Math.ceil(total.value / size.value)
  return t > 0 ? t : 1
})

const changePage = (p) => {
  if (p < 1 || p > totalPage.value) return
  page.value = p
  fetchList()
}

const onAppChange = () => {
  page.value = 1
  fetchList()
}

const ack = async (row) => {
  if (!selectedAppId.value) return
  actioning.value = row.id
  error.value = ''
  try {
    const { json } = await apiRequest(`/api/admin/logs/${row.id}/ack`, {
      method: 'POST',
      body: JSON.stringify({ appId: selectedAppId.value })
    })
    if (json.code !== 0) throw new Error(json.message || '操作失败')
    row.ack = 1
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    actioning.value = null
  }
}

const closeCreateApp = () => {
  showCreateApp.value = false
  creatingApp.value = false
  createAppError.value = ''
  createdAppId.value = ''
  appForm.name = ''
  appForm.remark = ''
}

const copyCreatedAppId = async () => {
  if (!createdAppId.value) return
  try {
    await navigator.clipboard.writeText(createdAppId.value)
  } catch {
    // fallback: fail silently to keep UX simple
  }
}

const submitCreateApp = async () => {
  creatingApp.value = true
  createAppError.value = ''
  createdAppId.value = ''
  try {
    const { resp, json } = await apiRequest('/api/admin/apps', {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify({ name: appForm.name, remark: appForm.remark || null })
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '创建失败')
    createdAppId.value = json.data?.appId || ''
    await fetchApps()
    if (createdAppId.value) {
      selectedAppId.value = createdAppId.value
      page.value = 1
      fetchList()
    }
  } catch (e) {
    createAppError.value = e.message || '创建失败'
  } finally {
    creatingApp.value = false
  }
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
  fetchApps().then(() => {
    page.value = 1
    fetchList()
  })
})
</script>

<style scoped>
.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
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

.apps-section {
  border-radius: var(--radius);
  border: 1px solid #e5e5ea;
  background: #fff;
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
}

.apps-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.apps-label {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.apps-select {
  min-width: 360px;
  padding: 0.45rem 0.6rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  background: #f5f5f7;
  font-size: 0.9rem;
}

.apps-hint {
  margin-top: 0.5rem;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.list-section {
  border-radius: var(--radius);
  border: 1px solid #e5e5ea;
  background: #fff;
  padding: 1.25rem 1.5rem 1rem;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.table th,
.table td {
  padding: 0.5rem 0.4rem;
  border-bottom: 1px solid #f0f0f5;
  vertical-align: top;
}

.table th {
  text-align: left;
  color: var(--color-text-muted);
  font-weight: 500;
  background: #fafafa;
}

.table tbody tr:nth-child(even) {
  background: #fafafa;
}

.content-cell {
  white-space: pre-wrap;
  word-break: break-word;
}

.badge {
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
}

.badge-success {
  background: #e8f5e9;
  color: #2e7d32;
}

.badge-warn {
  background: #fff3e0;
  color: #ef6c00;
}

.btn-action {
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
  border: none;
  cursor: pointer;
}

.btn-action[disabled] {
  opacity: 0.6;
  cursor: default;
}

.btn-ack {
  background: #e3f2fd;
  color: #1976d2;
}

.muted {
  color: var(--color-text-muted);
}

.empty {
  padding: 1rem 0;
  color: var(--color-text-muted);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.75rem;
  font-size: 0.875rem;
}

.btn-small {
  padding: 0.3rem 0.8rem;
  border-radius: var(--radius);
  background: var(--color-accent);
  color: #fff;
  font-size: 0.8rem;
  border: none;
  cursor: pointer;
}

.btn-small[disabled] {
  opacity: 0.5;
}

.error {
  margin-bottom: 0.5rem;
  color: #ff6b6b;
  font-size: 0.85rem;
}

code {
  font-size: 0.85em;
  background: #f0f0f5;
  padding: 0.15em 0.4em;
  border-radius: 4px;
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

