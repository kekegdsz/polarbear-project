<template>
  <div>
    <header class="content-header">
      <div>
        <h1>版本管理</h1>
        <p class="subtitle">查看版本列表、筛选、发布新版本</p>
      </div>
      <button class="btn btn-primary" @click="showPublish = true">发布版本</button>
    </header>

    <section class="list-section">
      <div class="list-header">
        <h2>版本列表</h2>
        <div class="filters">
          <select v-model="channel" @change="fetchList(1)">
            <option value="">全部渠道</option>
            <option value="android">Android</option>
            <option value="ios">iOS</option>
            <option value="ohos">OHOS</option>
          </select>
          <input
            v-model="keyword"
            type="search"
            placeholder="按版本号或更新说明搜索"
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
            <th>版本号</th>
            <th>版本码</th>
            <th>渠道</th>
            <th>下载链接</th>
            <th>状态</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in list" :key="v.id">
            <td>{{ v.id }}</td>
            <td>{{ v.versionName }}</td>
            <td>{{ v.versionCode }}</td>
            <td>{{ channelLabel(v.channel) }}</td>
            <td>
              <a v-if="v.downloadUrl" :href="v.downloadUrl" target="_blank" rel="noopener" class="link">
                {{ v.downloadUrl.length > 45 ? v.downloadUrl.slice(0, 45) + '...' : v.downloadUrl }}
              </a>
              <span v-else>-</span>
            </td>
            <td>
              <span :class="['badge', v.published ? 'badge-success' : 'badge-muted']">
                {{ v.published ? '已发布' : '已停止' }}
              </span>
            </td>
            <td>{{ formatDate(v.createdAt) }}</td>
            <td>
              <div class="actions">
                <button
                  v-if="v.published"
                  class="btn-action btn-stop"
                  @click="doStop(v.id)"
                  :disabled="actioning === v.id"
                >
                  {{ actioning === v.id ? '...' : '停止' }}
                </button>
                <button
                  v-else
                  class="btn-action btn-publish"
                  @click="doRepublish(v.id)"
                  :disabled="actioning === v.id"
                >
                  {{ actioning === v.id ? '...' : '重新发布' }}
                </button>
                <button
                  class="btn-action btn-delete"
                  @click="doDelete(v)"
                  :disabled="actioning === v.id"
                >
                  删除
                </button>
              </div>
            </td>
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

    <!-- 发布弹窗 -->
    <div v-if="showPublish" class="modal-overlay" @click.self="showPublish = false">
      <div class="modal">
        <h3>发布新版本</h3>
        <form @submit.prevent="submitPublish" class="form">
          <div class="form-group">
            <label>渠道 <span class="required">*</span></label>
            <select v-model="form.channel" required>
              <option value="android">Android</option>
              <option value="ios">iOS</option>
              <option value="ohos">OHOS</option>
            </select>
          </div>
          <div class="form-group">
            <label>版本号 <span class="required">*</span></label>
            <input v-model="form.versionName" placeholder="如 1.0.0" required />
          </div>
          <div class="form-group">
            <label>版本码 <span class="required">*</span></label>
            <input v-model.number="form.versionCode" type="number" placeholder="如 100" required />
          </div>
          <div class="form-group">
            <label>下载链接</label>
            <input v-model="form.downloadUrl" placeholder="APK/IPA/HAP 下载地址" />
          </div>
          <div class="form-group">
            <label>更新说明</label>
            <textarea v-model="form.releaseNotes" rows="3" placeholder="更新内容"></textarea>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-ghost" @click="showPublish = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="publishing">
              {{ publishing ? '发布中...' : '发布' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const channel = ref('')
const keyword = ref('')
const error = ref('')
const showPublish = ref(false)
const publishing = ref(false)
const actioning = ref(null)

const form = reactive({
  channel: 'android',
  versionName: '',
  versionCode: null,
  downloadUrl: '',
  releaseNotes: ''
})

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || ''
})

const channelLabel = (ch) => {
  const map = { android: 'Android', ios: 'iOS', ohos: 'OHOS' }
  return map[ch] || ch
}

const fetchList = async (toPage = page.value) => {
  page.value = toPage
  try {
    const params = new URLSearchParams({
      page: String(page.value),
      size: String(size.value)
    })
    if (channel.value) params.append('channel', channel.value)
    if (keyword.value) params.append('keyword', keyword.value)
    const { resp, json } = await apiRequest(`/api/admin/versions?${params.toString()}`, {
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

const doStop = async (id) => {
  actioning.value = id
  error.value = ''
  try {
    const { resp, json } = await apiRequest(`/api/admin/versions/${id}/stop`, {
      method: 'PATCH',
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '停止失败')
    fetchList(page.value)
  } catch (e) {
    error.value = e.message || '停止失败'
  } finally {
    actioning.value = null
  }
}

const doRepublish = async (id) => {
  actioning.value = id
  error.value = ''
  try {
    const { resp, json } = await apiRequest(`/api/admin/versions/${id}/publish`, {
      method: 'PATCH',
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '重新发布失败')
    fetchList(page.value)
  } catch (e) {
    error.value = e.message || '重新发布失败'
  } finally {
    actioning.value = null
  }
}

const doDelete = async (v) => {
  if (!confirm(`确定删除版本 ${v.versionName}（${channelLabel(v.channel)}）？`)) return
  actioning.value = v.id
  error.value = ''
  try {
    const { resp, json } = await apiRequest(`/api/admin/versions/${v.id}`, {
      method: 'DELETE',
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '删除失败')
    fetchList(page.value)
  } catch (e) {
    error.value = e.message || '删除失败'
  } finally {
    actioning.value = null
  }
}

const submitPublish = async () => {
  if (!form.versionName || form.versionCode == null) {
    error.value = '请填写版本号和版本码'
    return
  }
  publishing.value = true
  error.value = ''
  try {
    const { resp, json } = await apiRequest('/api/admin/versions', {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify(form)
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '发布失败')
    showPublish.value = false
    form.versionName = ''
    form.versionCode = null
    form.downloadUrl = ''
    form.releaseNotes = ''
    fetchList(1)
  } catch (e) {
    error.value = e.message || '发布失败'
  } finally {
    publishing.value = false
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

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin' } })
}

onMounted(() => fetchList(1))
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

.filters {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.filters select {
  padding: 0.4rem 0.6rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  background: #f5f5f7;
  font-size: 0.9rem;
}

.filters input {
  padding: 0.4rem 0.6rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  background: #f5f5f7;
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

.link {
  color: var(--color-accent);
  text-decoration: none;
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

.badge-muted {
  background: #f5f5f5;
  color: #757575;
}

.actions {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
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

.btn-stop {
  background: #fff3e0;
  color: #e65100;
}

.btn-publish {
  background: #e8f5e9;
  color: #2e7d32;
}

.btn-delete {
  background: #ffebee;
  color: #c62828;
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
  min-width: 400px;
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

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.5rem 0.65rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  font-size: 0.9rem;
}

.form-group textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}
</style>
