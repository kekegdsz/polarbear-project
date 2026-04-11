<template>
  <div>
    <header class="content-header">
      <div>
        <h1>订单管理</h1>
        <p class="subtitle">订单列表、付款统计、支持微信/支付宝</p>
      </div>
      <button class="btn btn-primary" @click="showCreate = true">新建订单</button>
    </header>

    <section class="stats-section">
      <div class="stat-card">
        <span class="stat-label">总订单数</span>
        <span class="stat-value">{{ stats.totalCount }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">待付款</span>
        <span class="stat-value">{{ stats.pendingCount }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">已下单</span>
        <span class="stat-value">{{ stats.orderedCount }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">已付款</span>
        <span class="stat-value">{{ stats.paidCount }}</span>
      </div>
      <div class="stat-card highlight">
        <span class="stat-label">付款金额</span>
        <span class="stat-value">¥{{ formatMoney(stats.paidAmount) }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">订单总额</span>
        <span class="stat-value">¥{{ formatMoney(stats.totalAmount) }}</span>
      </div>
    </section>

    <section class="list-section">
      <div class="list-header">
        <h2>订单列表</h2>
        <div class="filters">
          <select v-model="status" @change="fetchList(1)">
            <option value="">全部状态</option>
            <option value="pending">待付款</option>
            <option value="ordered">已下单</option>
            <option value="paid">已付款</option>
          </select>
          <select v-model="paymentType" @change="fetchList(1)">
            <option value="">全部支付方式</option>
            <option value="wechat">微信</option>
            <option value="alipay">支付宝</option>
          </select>
          <input
            v-model="keyword"
            type="search"
            placeholder="订单号/支付流水号"
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
            <th>订单号</th>
            <th>用户/设备</th>
            <th>金额</th>
            <th>状态</th>
            <th>支付方式</th>
            <th>支付流水号</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in list" :key="o.id">
            <td>{{ o.id }}</td>
            <td><code>{{ o.orderNo }}</code></td>
            <td>{{ o.deviceUuid || o.userId || '-' }}</td>
            <td>¥{{ formatMoney(o.amount) }}</td>
            <td>
              <span :class="['badge', statusBadgeClass(o.status)]">
                {{ o.statusLabel }}
              </span>
            </td>
            <td>{{ o.paymentTypeLabel }}</td>
            <td>{{ o.paymentTradeNo || '-' }}</td>
            <td>{{ formatDate(o.createdAt) }}</td>
            <td>
              <div class="actions">
                <button
                  v-if="o.status !== 'paid'"
                  class="btn-action btn-publish"
                  @click="markPaid(o)"
                  :disabled="actioning === o.id"
                >
                  {{ actioning === o.id ? '...' : '标记已付款' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">暂无订单</p>

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

    <!-- 新建订单弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3>新建订单</h3>
        <form @submit.prevent="submitCreate" class="form">
          <div class="form-group">
            <label>用户 ID（可选）</label>
            <input v-model.number="form.userId" type="number" placeholder="留空则无关联用户" />
          </div>
          <div class="form-group">
            <label>金额 <span class="required">*</span></label>
            <input v-model="form.amount" type="number" step="0.01" required placeholder="0.00" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="form.status">
              <option value="pending">待付款</option>
              <option value="ordered">已下单</option>
              <option value="paid">已付款</option>
            </select>
          </div>
          <div class="form-group">
            <label>支付方式</label>
            <select v-model="form.paymentType">
              <option value="">未选择</option>
              <option value="wechat">微信</option>
              <option value="alipay">支付宝</option>
            </select>
          </div>
          <div class="form-group">
            <label>备注</label>
            <input v-model="form.remark" placeholder="可选" />
          </div>
          <p v-if="createError" class="form-error">{{ createError }}</p>
          <div class="form-actions">
            <button type="button" class="btn btn-ghost" @click="showCreate = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="creating">
              {{ creating ? '提交中...' : '创建' }}
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
const status = ref('')
const paymentType = ref('')
const keyword = ref('')
const error = ref('')
const stats = ref({
  totalCount: 0,
  pendingCount: 0,
  orderedCount: 0,
  paidCount: 0,
  totalAmount: 0,
  paidAmount: 0
})
const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const actioning = ref(null)

const form = reactive({
  userId: null,
  amount: '',
  status: 'pending',
  paymentType: '',
  remark: ''
})

const getAdminHeaders = () => ({
  'X-Auth-Token': localStorage.getItem('admin_token') || ''
})

const handleAuthFailed = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_role')
  router.push({ path: '/admin/login', query: { redirect: '/admin/orders' } })
}

const fetchStats = async () => {
  try {
    const { resp, json } = await apiRequest('/api/admin/orders/stats', {
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code === 0 && json.data) stats.value = json.data
  } catch {
    // ignore
  }
}

const fetchList = async (toPage = page.value) => {
  page.value = toPage
  error.value = ''
  try {
    const params = new URLSearchParams({
      page: String(page.value),
      size: String(size.value)
    })
    if (status.value) params.append('status', status.value)
    if (paymentType.value) params.append('paymentType', paymentType.value)
    if (keyword.value) params.append('keyword', keyword.value)
    const { resp, json } = await apiRequest(`/api/admin/orders?${params.toString()}`, {
      headers: getAdminHeaders()
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '获取列表失败')
    total.value = json.data.total || 0
    list.value = json.data.list || []
    fetchStats()
  } catch (e) {
    error.value = e.message || '获取列表失败'
  }
}

const changePage = (p) => fetchList(p)

const markPaid = async (o) => {
  actioning.value = o.id
  error.value = ''
  try {
    const { resp, json } = await apiRequest(`/api/admin/orders/${o.id}/status`, {
      method: 'PATCH',
      headers: getAdminHeaders(),
      body: JSON.stringify({ status: 'paid' })
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '操作失败')
    fetchList(page.value)
    fetchStats()
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    actioning.value = null
  }
}

const submitCreate = async () => {
  creating.value = true
  createError.value = ''
  try {
    const payload = {
      amount: parseFloat(form.amount) || 0,
      status: form.status,
      paymentType: form.paymentType || null,
      remark: form.remark || null
    }
    if (form.userId) payload.userId = form.userId
    const { resp, json } = await apiRequest('/api/admin/orders', {
      method: 'POST',
      headers: getAdminHeaders(),
      body: JSON.stringify(payload)
    })
    if (resp.status === 401 || resp.status === 403) return handleAuthFailed()
    if (json.code !== 0) throw new Error(json.message || '创建失败')
    showCreate.value = false
    form.userId = null
    form.amount = ''
    form.status = 'pending'
    form.paymentType = ''
    form.remark = ''
    fetchList(1)
    fetchStats()
  } catch (e) {
    createError.value = e.message || '创建失败'
  } finally {
    creating.value = false
  }
}

const formatMoney = (v) => {
  if (v == null || v === undefined) return '0.00'
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
}

const formatDate = (v) => {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString()
  } catch {
    return String(v)
  }
}

const statusBadgeClass = (s) => {
  if (s === 'paid') return 'badge-success'
  if (s === 'ordered') return 'badge-info'
  return 'badge-muted'
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.stat-card.highlight {
  border-left: 4px solid #ff9f43;
}

.stat-label {
  display: block;
  font-size: 0.8rem;
  color: #6e6e73;
  margin-bottom: 0.35rem;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1d1d1f;
}

code {
  font-size: 0.85em;
  background: #f0f0f5;
  padding: 0.15em 0.4em;
  border-radius: 4px;
}

.badge-info {
  background: #e3f2fd;
  color: #1976d2;
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.subtitle { color: var(--color-text-muted); }

.btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  border: none;
  cursor: pointer;
}

.btn-primary { background: var(--color-accent); color: #fff; }

.btn-ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

.list-section {
  border-radius: var(--radius);
  border: 1px solid #e5e5ea;
  background: #fff;
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

.filters select,
.filters input {
  padding: 0.4rem 0.6rem;
  border-radius: var(--radius);
  border: 1px solid #d2d2d7;
  background: #f5f5f7;
  font-size: 0.9rem;
}

.filters input { width: 180px; }

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

.table tbody tr:nth-child(even) { background: #fafafa; }

.badge {
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
}

.badge-success { background: #e8f5e9; color: #2e7d32; }

.badge-muted { background: #f5f5f5; color: #757575; }

.actions { display: flex; gap: 0.4rem; flex-wrap: wrap; }

.btn-action {
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
  border: none;
  cursor: pointer;
}

.btn-action[disabled] { opacity: 0.6; cursor: default; }

.btn-publish { background: #e8f5e9; color: #2e7d32; }

.empty { padding: 1rem 0; color: var(--color-text-muted); }

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

.btn-small[disabled] { opacity: 0.5; }

.error { margin-bottom: 0.5rem; color: #ff6b6b; font-size: 0.85rem; }

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

.modal h3 { margin-bottom: 1.25rem; font-size: 1.1rem; }

.form-group { margin-bottom: 1rem; }

.form-group label {
  display: block;
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-bottom: 0.35rem;
}

.form-group .required { color: #ff6b6b; }

.form-group input,
.form-group select {
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
  margin-top: 1.5rem;
}

.form-error { color: #ff6b6b; font-size: 0.875rem; margin-bottom: 0.5rem; }
</style>
