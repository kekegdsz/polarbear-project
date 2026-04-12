<template>
  <div>
    <header class="content-header">
      <div>
        <h1>用户</h1>
        <p class="subtitle">
          全字段列表与 IM 在线状态；系统私聊由固定账号 ID {{ SYSTEM_USER_ID }}（系统通知）发出，该账号已从本列表隐藏。
          <span v-if="lastSynced" class="sync-hint">· {{ lastSynced }} 已同步</span>
        </p>
        <p class="live-hint">在线人数与列表每 5 秒自动刷新。点击「系统私聊」向该用户发送一条官方私聊。</p>
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
      <div class="stat-card highlight">
        <div class="stat-label">IM 在线人数</div>
        <div class="stat-value">{{ stats.onlineImUsers ?? 0 }}</div>
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

      <div class="table-wrap" v-if="list.length">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>设备UUID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>手机号</th>
              <th>有Token</th>
              <th>Token(脱敏)</th>
              <th>Token过期</th>
              <th>VIP</th>
              <th>VIP过期</th>
              <th>角色</th>
              <th>注册时间</th>
              <th>更新时间</th>
              <th>IM状态</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in list" :key="u.id">
              <td>{{ u.id }}</td>
              <td class="mono">{{ u.deviceUuid || '-' }}</td>
              <td>{{ u.username || '-' }}</td>
              <td>{{ u.nickname || '-' }}</td>
              <td>{{ u.mobile || '-' }}</td>
              <td>{{ u.hasToken ? '是' : '否' }}</td>
              <td class="mono">{{ u.tokenMasked || '-' }}</td>
              <td>{{ formatDate(u.tokenExpireTime) }}</td>
              <td>{{ u.vip || '-' }}</td>
              <td>{{ formatDate(u.vipExpireTime) }}</td>
              <td>{{ u.role || '-' }}</td>
              <td>{{ formatDate(u.createdAt) }}</td>
              <td>{{ formatDate(u.updatedAt) }}</td>
              <td>
                <span :class="['badge', u.online ? 'on' : 'off']">
                  {{ u.online ? '在线' : '离线' }}
                </span>
              </td>
              <td class="col-action">
                <button type="button" class="btn-dm" @click="openDm(u)">系统私聊</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
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

    <!-- 单聊发送：系统通知 → 选中用户 -->
    <Teleport to="body">
      <div v-if="dmOpen" class="dm-backdrop" @click.self="closeDm">
        <div class="dm-sheet" role="dialog" aria-modal="true" aria-labelledby="dm-title">
          <div class="dm-head">
            <div class="dm-head-icon">系</div>
            <div class="dm-head-text">
              <div id="dm-title" class="dm-title">系统私聊</div>
              <div class="dm-sub">
                以「系统通知」身份发送至
                <strong>{{ dmPeerLabel }}</strong>
              </div>
            </div>
            <button type="button" class="dm-close" aria-label="关闭" @click="closeDm">×</button>
          </div>

          <div class="dm-body">
            <div class="dm-preview">
              <div class="dm-bubble dm-bubble-system">
                <span class="dm-bubble-name">系统通知</span>
                <p class="dm-bubble-placeholder">
                  {{ dmBody.trim() ? dmBody : '在下方输入要发送的正文…' }}
                </p>
              </div>
              <div class="dm-bubble dm-bubble-user">
                <span class="dm-bubble-name">{{ dmPeerShort }}</span>
                <p class="dm-bubble-hint">对方 App 内将收到此条私聊</p>
              </div>
            </div>

            <label class="dm-label">消息内容</label>
            <textarea
              v-model="dmBody"
              class="dm-input"
              rows="5"
              placeholder="输入要发送给用户的内容…"
            />
            <p v-if="dmError" class="dm-err">{{ dmError }}</p>
            <p v-if="dmOk" class="dm-ok">{{ dmOk }}</p>
          </div>

          <div class="dm-foot">
            <button type="button" class="btn-ghost" :disabled="dmSending" @click="closeDm">取消</button>
            <button type="button" class="btn-send" :disabled="dmSending" @click="submitDm">
              {{ dmSending ? '发送中…' : '发送' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const SYSTEM_USER_ID = 999
const POLL_MS = 5000

const router = useRouter()
const stats = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const error = ref('')
const lastSynced = ref('')

const dmOpen = ref(false)
const dmUser = ref(null)
const dmBody = ref('')
const dmSending = ref(false)
const dmError = ref('')
const dmOk = ref('')

let pollTimer = null

const dmPeerLabel = computed(() => {
  const u = dmUser.value
  if (!u) return ''
  const nick = u.nickname?.trim()
  const name = u.username?.trim()
  return nick || name || `用户 ${u.id}`
})

const dmPeerShort = computed(() => {
  const u = dmUser.value
  if (!u) return ''
  return u.nickname?.trim() || u.username?.trim() || `ID ${u.id}`
})

const touchSynced = () => {
  try {
    lastSynced.value = new Date().toLocaleTimeString()
  } catch {
    lastSynced.value = ''
  }
}

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || '',
  'Content-Type': 'application/json'
})

const fetchStats = async () => {
  try {
    const { resp, json } = await apiRequest('/api/admin/users/stats', {
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '获取统计失败')
    stats.value = json.data
    error.value = ''
    touchSynced()
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
    error.value = ''
    touchSynced()
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

const refreshAll = async () => {
  await fetchStats()
  await fetchList(page.value)
}

const openDm = (u) => {
  if (u.id === SYSTEM_USER_ID) return
  dmUser.value = u
  dmBody.value = ''
  dmError.value = ''
  dmOk.value = ''
  dmOpen.value = true
}

const closeDm = () => {
  dmOpen.value = false
  dmUser.value = null
}

const submitDm = async () => {
  const u = dmUser.value
  if (!u) return
  const text = dmBody.value.trim()
  if (!text) {
    dmError.value = '请输入消息内容'
    return
  }
  dmError.value = ''
  dmOk.value = ''
  dmSending.value = true
  try {
    const { resp, json } = await apiRequest(`/api/admin/users/${u.id}/system-p2p`, {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify({ body: text })
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '发送失败')
    dmOk.value = `已送达，msgId=${json.data?.msgId ?? ''}`
    dmBody.value = ''
    await refreshAll()
  } catch (e) {
    dmError.value = e.message || '发送失败'
  } finally {
    dmSending.value = false
  }
}

onMounted(() => {
  refreshAll()
  pollTimer = setInterval(refreshAll, POLL_MS)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
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
.sync-hint {
  font-weight: normal;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
.live-hint {
  margin: 0.35rem 0 0;
  font-size: 0.8rem;
  color: #059669;
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

.stat-card.highlight {
  border-color: #34c759;
  background: #f0fff4;
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

.table-wrap {
  overflow-x: auto;
  max-width: 100%;
}

.table {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 0.8rem;
}

.table th,
.table td {
  padding: 0.45rem 0.35rem;
  border-bottom: 1px solid #f0f0f5;
  white-space: nowrap;
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

.col-action {
  position: sticky;
  right: 0;
  background: inherit;
  box-shadow: -6px 0 8px -6px rgba(0, 0, 0, 0.08);
}

.mono {
  font-family: ui-monospace, monospace;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge.on {
  background: #d1fae5;
  color: #065f46;
}

.badge.off {
  background: #f3f4f6;
  color: #6b7280;
}

.btn-dm {
  padding: 0.25rem 0.55rem;
  border-radius: 999px;
  border: none;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  white-space: nowrap;
}

.btn-dm:hover {
  filter: brightness(1.05);
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

/* 单聊弹层 */
.dm-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.dm-sheet {
  width: 100%;
  max-width: 440px;
  max-height: min(92vh, 640px);
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  overflow: hidden;
  background: #f8fafc;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.08);
}

.dm-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1rem 0.85rem;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 55%, #9333ea 100%);
  color: #fff;
}

.dm-head-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.1rem;
}

.dm-head-text {
  flex: 1;
  min-width: 0;
}

.dm-title {
  font-size: 1.05rem;
  font-weight: 700;
}

.dm-sub {
  font-size: 0.78rem;
  opacity: 0.92;
  margin-top: 0.15rem;
}

.dm-close {
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 1.35rem;
  line-height: 1;
  cursor: pointer;
}

.dm-close:hover {
  background: rgba(255, 255, 255, 0.28);
}

.dm-body {
  padding: 1rem 1.1rem 0.5rem;
  overflow-y: auto;
}

.dm-preview {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  margin-bottom: 1rem;
}

.dm-bubble {
  max-width: 92%;
  padding: 0.65rem 0.85rem;
  border-radius: 16px;
  font-size: 0.82rem;
  line-height: 1.45;
}

.dm-bubble-system {
  align-self: flex-start;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.dm-bubble-user {
  align-self: flex-end;
  background: linear-gradient(135deg, #dbeafe, #e0e7ff);
  border: 1px solid #c7d2fe;
}

.dm-bubble-name {
  display: block;
  font-size: 0.68rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.25rem;
}

.dm-bubble-placeholder {
  margin: 0;
  color: #0f172a;
  white-space: pre-wrap;
  word-break: break-word;
}

.dm-bubble-hint {
  margin: 0;
  color: #475569;
  font-size: 0.75rem;
}

.dm-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.35rem;
}

.dm-input {
  width: 100%;
  box-sizing: border-box;
  border-radius: 14px;
  border: 1px solid #cbd5e1;
  padding: 0.65rem 0.85rem;
  font-family: inherit;
  font-size: 0.88rem;
  resize: vertical;
  min-height: 120px;
  background: #fff;
}

.dm-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.dm-err {
  color: #dc2626;
  font-size: 0.8rem;
  margin: 0.4rem 0 0;
}

.dm-ok {
  color: #059669;
  font-size: 0.8rem;
  margin: 0.4rem 0 0;
}

.dm-foot {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.75rem 1rem 1rem;
  background: #f1f5f9;
  border-top: 1px solid #e2e8f0;
}

.btn-ghost {
  padding: 0.45rem 1rem;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
  font-size: 0.88rem;
}

.btn-send {
  padding: 0.45rem 1.25rem;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.88rem;
}

.btn-send:disabled,
.btn-ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
