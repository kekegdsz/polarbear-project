<template>
  <div>
    <header class="content-header">
      <div>
        <router-link to="/admin/groups" class="back">← 返回群列表</router-link>
        <h1>群：{{ detail?.name || '…' }}</h1>
        <p class="subtitle">
          群ID {{ groupId }} · 共 <strong>{{ detail?.memberCount ?? 0 }}</strong> 人 · IM 在线
          <strong>{{ onlineInGroup }}</strong> 人
          <span v-if="lastSynced" class="sync-hint">（{{ lastSynced }} 已刷新）</span>
        </p>
        <p class="live-hint">成员在线状态每 5 秒自动刷新。</p>
        <p class="link-hint">
          需要向某位成员发<strong>系统私聊</strong>？请到
          <router-link to="/admin/users">用户管理</router-link>
          ，在列表中点击对应用户的「系统私聊」。
        </p>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>

    <section class="panel" v-if="detail">
      <h2>成员与在线状态</h2>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>用户ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>手机</th>
              <th>角色</th>
              <th>入群时间</th>
              <th>IM</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in detail.members" :key="m.userId">
              <td>{{ m.userId }}</td>
              <td>{{ m.username || '-' }}</td>
              <td>{{ m.nickname || '-' }}</td>
              <td>{{ m.mobile || '-' }}</td>
              <td>{{ m.role }}</td>
              <td>{{ formatDate(m.joinedAt) }}</td>
              <td>
                <span :class="['badge', m.online ? 'on' : 'off']">
                  {{ m.online ? '在线' : '离线' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel panel-broadcast">
      <h2>向群内广播（群消息）</h2>
      <p class="hint">
        由系统账号（ID 999 · 系统通知）发送一条 <strong>GROUP</strong> 消息；客户端会话中显示为该账号发送。
      </p>
      <textarea v-model="broadcastText" rows="3" placeholder="输入要发送到群里的文字…" />
      <button class="btn" :disabled="sendingBroadcast" @click="doBroadcast">
        {{ sendingBroadcast ? '发送中…' : '发送到群' }}
      </button>
      <p v-if="broadcastOk" class="ok">{{ broadcastOk }}</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiRequest } from '../apiClient'

const POLL_MS = 5000

const route = useRoute()
const router = useRouter()
const groupId = computed(() => route.params.id)

const detail = ref(null)
const error = ref('')
const broadcastText = ref('')
const sendingBroadcast = ref(false)
const broadcastOk = ref('')
const lastSynced = ref('')

let pollTimer = null

const onlineInGroup = computed(() => {
  const m = detail.value?.members
  if (!m?.length) return 0
  return m.filter((x) => x.online).length
})

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || '',
  'Content-Type': 'application/json'
})

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  router.push({ path: '/admin/login' })
}

const touchSynced = () => {
  try {
    lastSynced.value = new Date().toLocaleTimeString()
  } catch {
    lastSynced.value = ''
  }
}

const load = async () => {
  error.value = ''
  try {
    const { resp, json } = await apiRequest(`/api/admin/im/groups/${groupId.value}`, {
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '加载失败')
    detail.value = json.data
    error.value = ''
    touchSynced()
  } catch (e) {
    error.value = e.message || '加载失败'
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

const doBroadcast = async () => {
  broadcastOk.value = ''
  const body = broadcastText.value.trim()
  if (!body) {
    error.value = '请输入广播内容'
    return
  }
  sendingBroadcast.value = true
  error.value = ''
  try {
    const { resp, json } = await apiRequest(`/api/admin/im/groups/${groupId.value}/broadcast`, {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify({ body })
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '发送失败')
    broadcastOk.value = `已发送，msgId=${json.data?.msgId ?? ''}`
    broadcastText.value = ''
    await load()
  } catch (e) {
    error.value = e.message || '发送失败'
  } finally {
    sendingBroadcast.value = false
  }
}

onMounted(() => {
  load()
  pollTimer = setInterval(() => {
    load()
  }, POLL_MS)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.back {
  display: inline-block;
  margin-bottom: 0.5rem;
  color: var(--color-accent);
  text-decoration: none;
  font-size: 0.9rem;
}
.content-header h1 {
  margin: 0.25rem 0;
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
.link-hint {
  margin: 0.5rem 0 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}
.link-hint a {
  color: var(--color-accent);
  font-weight: 500;
}
.panel {
  margin-bottom: 1.5rem;
  padding: 1.25rem 1.5rem;
  border-radius: var(--radius);
  border: 1px solid #e5e5ea;
  background: #fff;
}
.panel-broadcast {
  border-left: 4px solid var(--color-accent);
}
.panel h2 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
}
.table-wrap {
  overflow-x: auto;
}
.table {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 0.8rem;
}
.table th,
.table td {
  padding: 0.4rem 0.35rem;
  border-bottom: 1px solid #f0f0f5;
  text-align: left;
  white-space: nowrap;
}
.table th {
  background: #fafafa;
  color: var(--color-text-muted);
}
textarea {
  width: 100%;
  max-width: 560px;
  margin-bottom: 0.6rem;
  padding: 0.5rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  font-family: inherit;
}
.btn {
  padding: 0.45rem 1rem;
  border-radius: var(--radius);
  background: var(--color-accent);
  color: #fff;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.hint {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
}
.badge {
  display: inline-block;
  padding: 0.12rem 0.4rem;
  border-radius: 6px;
  font-size: 0.72rem;
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
.error {
  color: #ff6b6b;
  margin-bottom: 0.75rem;
}
.ok {
  color: #059669;
  font-size: 0.85rem;
  margin-top: 0.5rem;
}
</style>
