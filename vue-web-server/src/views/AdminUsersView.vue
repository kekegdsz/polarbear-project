<template>
  <div>
    <header class="content-header">
      <div>
        <h1>用户</h1>
        <p class="subtitle">查看注册用户统计和列表</p>
      </div>
    </header>

    <section class="stats" v-if="stats">
      <div class="stat-card">
        <div class="stat-label">用户总数</div>
        <div class="stat-value">{{ stats.totalUsers }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日新增</div>
        <div class="stat-value">{{ stats.todayUsers }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">付费用户</div>
        <div class="stat-value">{{ stats.vipUsers }}</div>
      </div>
    </section>

    <section class="list-section">
      <div class="list-header">
        <h2>用户列表</h2>
        <div class="actions">
          <input
            v-model="keyword"
            type="search"
            placeholder="按用户名或手机号搜索"
            @keyup.enter="fetchList(1)"
          />
          <button class="btn-small" @click="fetchList(1)">搜索</button>
        </div>
      </div>

      <div v-if="error" class="error">{{ error }}</div>

      <table class="table" v-if="list.length">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>手机号</th>
            <th>设备ID</th>
            <th>VIP</th>
            <th>角色</th>
            <th>注册时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in list" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username || '-' }}</td>
            <td>{{ u.mobile || '-' }}</td>
            <td>{{ u.deviceUuid || u.deviceId || u.device_id || '-' }}</td>
            <td>{{ u.vip }}</td>
            <td>{{ u.role }}</td>
            <td>{{ formatDate(u.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">暂无数据</p>

      <div class="pager">
        <button class="btn-small" :disabled="page === 1" @click="changePage(page - 1)">
          上一页
        </button>
        <span>第 {{ page }} 页</span>
        <button
          class="btn-small"
          :disabled="page * size >= total"
          @click="changePage(page + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const router = useRouter()
const stats = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const error = ref('')

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || ''
})

const fetchStats = async () => {
  try {
    const { resp, json } = await apiRequest('/api/admin/users/stats', {
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '获取统计失败')
    stats.value = json.data
  } catch (e) {
    error.value = e.message || '获取统计失败'
  }
}

const fetchList = async (toPage = page.value) => {
  page.value = toPage
  try {
    const params = new URLSearchParams({
      page: String(page.value),
      size: String(size.value)
    })
    if (keyword.value) params.append('keyword', keyword.value)
    const { resp, json } = await apiRequest(`/api/admin/users?${params.toString()}`, {
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '获取列表失败')
    total.value = json.data.total || 0
    list.value = json.data.list || []
  } catch (e) {
    error.value = e.message || '获取列表失败'
  }
}

const changePage = (p) => fetchList(p)

const formatDate = (v) => {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString()
  } catch {
    return String(v)
  }
}

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin' } })
}

onMounted(() => {
  fetchStats()
  fetchList(1)
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

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  padding: 1rem 1.25rem;
  border-radius: var(--radius);
  border: 1px solid #e5e5ea;
  background: #ffffff;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-bottom: 0.25rem;
}

.stat-value {
  font-size: 1.4rem;
  font-weight: 600;
}

.list-section {
  border-radius: var(--radius);
  border: 1px solid #e5e5ea;
  background: #ffffff;
  padding: 1.25rem 1.5rem 1rem;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.actions input {
  padding: 0.4rem 0.6rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  background: #f5f5f7;
  color: #1d1d1f;
  width: 220px;
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
</style>
