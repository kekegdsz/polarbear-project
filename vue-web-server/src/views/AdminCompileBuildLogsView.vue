<template>
  <div>
    <header class="content-header">
      <div>
        <h1>编译日志管理</h1>
        <p class="subtitle">Gradle 构建上报：耗时、机器、起止时间（需在 Android 工程配置 compileReportUrl / compileReportAppId）</p>
      </div>
      <div class="header-actions">
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
      <div class="apps-row employee-row">
        <label class="apps-label">时间</label>
        <input v-model="startDate" type="date" class="employee-input date-input" @change="onFilterChange" />
        <span class="range-sep">至</span>
        <input v-model="endDate" type="date" class="employee-input date-input" @change="onFilterChange" />
      </div>
      <p class="apps-hint">
        上报接口（无需登录）：<code>POST /api/admin/compile-logs/report</code>，body 含 appId、durationMs、startedAtMs、endedAtMs、machine、osUser、tasks、success 等。
      </p>
    </section>

    <section class="list-section">
      <div v-if="error" class="error">{{ error }}</div>

      <table class="table" v-if="list.length">
        <thead>
          <tr>
            <th style="width: 72px">ID</th>
            <th style="width: 88px">结果</th>
            <th style="width: 100px">耗时</th>
            <th style="width: 120px">机器 / 用户</th>
            <th style="width: 170px">编译开始</th>
            <th style="width: 170px">编译结束</th>
            <th style="max-width: 420px">项目 · 任务</th>
            <th style="width: 160px">上报时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <span :class="['badge', row.success ? 'badge-success' : 'badge-warn']">
                {{ row.success ? '成功' : '失败' }}
              </span>
            </td>
            <td>{{ formatDuration(row.durationMs) }}</td>
            <td class="cell-tight">
              <div class="mono">{{ row.machine || '-' }}</div>
              <div class="muted small">{{ row.osUser || '-' }}</div>
            </td>
            <td>{{ formatDate(row.startedAt) }}</td>
            <td>{{ formatDate(row.endedAt) }}</td>
            <td class="cell-project-tasks" :title="projectTasksLine(row)">
              {{ projectTasksLine(row) }}
            </td>
            <td>{{ formatDate(row.createdAt) }}</td>
          </tr>
        </tbody>
      </table>

      <p v-else class="empty">{{ loading ? '加载中...' : '暂无编译记录' }}</p>

      <div class="pager">
        <button class="btn-small" :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</button>
        <span>第 {{ page }} 页 / 共 {{ totalPage }} 页（{{ total }} 条）</span>
        <button class="btn-small" :disabled="page >= totalPage || loading" @click="changePage(page + 1)">
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const router = useRouter()
const list = ref([])
const apps = ref([])
const selectedAppId = ref('')
const loading = ref(false)
const appsLoading = ref(false)
const error = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const startDate = ref('')
const endDate = ref('')

const dateStringToStartMs = (dateStr) => {
  if (!dateStr) return null
  const d = new Date(`${dateStr}T00:00:00`)
  return Number.isNaN(d.getTime()) ? null : d.getTime()
}

const dateStringToEndExclusiveMs = (dateStr) => {
  if (!dateStr) return null
  const d = new Date(`${dateStr}T00:00:00`)
  if (Number.isNaN(d.getTime())) return null
  d.setDate(d.getDate() + 1)
  return d.getTime()
}

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || ''
})

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin/compile-logs' } })
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
    const startMs = dateStringToStartMs(startDate.value)
    const endMs = dateStringToEndExclusiveMs(endDate.value)
    if (startDate.value && startMs === null) throw new Error('开始日期格式无效')
    if (endDate.value && endMs === null) throw new Error('结束日期格式无效')
    if (startMs !== null && endMs !== null && startMs >= endMs) {
      throw new Error('结束日期需大于等于开始日期')
    }
    const { resp, json } = await apiRequest('/api/admin/compile-logs/list', {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify({
        appId: selectedAppId.value,
        createdStartMs: startMs,
        createdEndMs: endMs,
        page: page.value,
        size: size.value
      })
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '获取编译日志失败')
    total.value = json.data?.total || 0
    const raw = json.data?.list || []
    list.value = raw.map((row) => ({
      ...row,
      success: row.success === 1 || row.success === true
    }))
  } catch (e) {
    error.value = e.message || '获取编译日志失败'
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

const onFilterChange = () => {
  page.value = 1
  fetchList()
}

const formatDate = (v) => {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString()
  } catch {
    return String(v)
  }
}

const formatDuration = (ms) => {
  if (ms == null || !Number.isFinite(Number(ms))) return '-'
  const n = Number(ms)
  if (n < 1000) return `${n} ms`
  const sec = n / 1000
  if (sec < 60) return `${sec.toFixed(1)} s`
  const m = Math.floor(sec / 60)
  const s = Math.round(sec % 60)
  return `${m} m ${s} s`
}

const projectTasksLine = (row) => {
  const p = row.projectKey?.trim() || '-'
  const t = row.tasks?.trim() || '-'
  return `${p} · ${t}`
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
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  border: none;
  cursor: pointer;
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

.employee-row {
  margin-top: 0.6rem;
}

.apps-label {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.apps-select {
  min-width: 280px;
  padding: 0.45rem 0.6rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  background: #f5f5f7;
  font-size: 0.9rem;
}

.employee-input {
  min-width: 220px;
  padding: 0.45rem 0.6rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  background: #f5f5f7;
  font-size: 0.9rem;
}

.date-input {
  min-width: 170px;
}

.range-sep {
  color: var(--color-text-muted);
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
  word-break: break-word;
}

.cell-project-tasks {
  max-width: 420px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.85rem;
  vertical-align: top;
}

.cell-tight {
  max-width: 140px;
}

.mono {
  font-family: ui-monospace, monospace;
  font-size: 0.8rem;
  word-break: break-word;
}

.small {
  font-size: 0.8rem;
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
</style>
