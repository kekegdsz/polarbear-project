<template>
  <div>
    <header class="content-header">
      <div>
        <h1>群管理</h1>
        <p class="subtitle">查看全部群聊与人数，进入二级页发系统消息</p>
      </div>
    </header>

    <section class="list-section">
      <div v-if="error" class="error">{{ error }}</div>

      <table class="table" v-if="list.length">
        <thead>
          <tr>
            <th>群ID</th>
            <th>群名称</th>
            <th>群主用户ID</th>
            <th>人数</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="g in list" :key="g.id">
            <td>{{ g.id }}</td>
            <td>{{ g.name || '-' }}</td>
            <td>{{ g.ownerUserId }}</td>
            <td><strong>{{ g.memberCount }}</strong></td>
            <td>{{ formatDate(g.createdAt) }}</td>
            <td>
              <div class="row-actions">
                <router-link class="link" :to="`/admin/groups/${g.id}`">管理</router-link>
                <button
                  type="button"
                  class="btn-danger"
                  :disabled="dismissingId === g.id"
                  @click="dismissGroup(g)"
                >
                  {{ dismissingId === g.id ? '解散中…' : '解散群' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">{{ loaded ? '暂无群' : '加载中…' }}</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const router = useRouter()
const list = ref([])
const error = ref('')
const loaded = ref(false)
const dismissingId = ref(null)

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || '',
  'Content-Type': 'application/json'
})

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin/groups' } })
}

const fetchList = async () => {
  try {
    const { resp, json } = await apiRequest('/api/admin/im/groups', { headers: getAdminHeaders() })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '加载失败')
    list.value = json.data || []
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loaded.value = true
  }
}

const dismissGroup = async (g) => {
  const ok = window.confirm(
    `确认解散该群？\n群ID=${g.id}\n群名=${g.name || '-'}\n\n解散后群成员关系与群消息都会被删除。`
  )
  if (!ok) return
  dismissingId.value = g.id
  try {
    const { resp, json } = await apiRequest(`/api/admin/im/groups/${g.id}/dismiss`, {
      method: 'POST',
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '解散失败')
    await fetchList()
  } catch (e) {
    error.value = e.message || '解散失败'
  } finally {
    dismissingId.value = null
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

onMounted(fetchList)
</script>

<style scoped>
.content-header {
  margin-bottom: 1.5rem;
}
.subtitle {
  color: var(--color-text-muted);
}
.list-section {
  border-radius: var(--radius);
  border: 1px solid #e5e5ea;
  background: #ffffff;
  padding: 1.25rem 1.5rem;
}
.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}
.table th,
.table td {
  padding: 0.55rem 0.5rem;
  border-bottom: 1px solid #f0f0f5;
  text-align: left;
}
.table th {
  color: var(--color-text-muted);
  background: #fafafa;
}
.link {
  color: var(--color-accent);
  text-decoration: none;
  font-weight: 500;
}
.link:hover {
  text-decoration: underline;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.btn-danger {
  border: none;
  border-radius: 8px;
  padding: 0.3rem 0.55rem;
  font-size: 0.76rem;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #ef4444, #f97316);
}

.btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.empty {
  color: var(--color-text-muted);
}
.error {
  color: #ff6b6b;
  margin-bottom: 0.75rem;
}
</style>
