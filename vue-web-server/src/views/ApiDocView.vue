<template>
  <div class="api-doc">
    <aside v-if="tocItems.length" class="toc" aria-label="文档目录">
      <div class="toc-title">目录</div>
      <nav class="toc-nav">
        <button
          v-for="item in tocItems"
          :key="item.id"
          class="toc-item"
          :class="{ active: item.id === activeTocId, h3: item.level === 3 }"
          type="button"
          @click="scrollToId(item.id)"
        >
          <span class="toc-text">{{ item.text }}</span>
        </button>
      </nav>
    </aside>

    <div ref="docRef" class="doc-card">
      <h1 class="title">客户端 API 接入文档</h1>
      <p class="subtitle">苍穹之下 - 客户端接入说明</p>

      <section class="section">
        <h2>0. 服务器 API 清单（官网维护）</h2>

        <div class="srv-toolbar">
          <label class="srv-field">
            <span class="srv-label">服务器</span>
            <select v-model="activeServerId" class="srv-select">
              <option v-for="s in servers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </label>

          <label class="srv-field srv-field-grow">
            <span class="srv-label">搜索</span>
            <input v-model.trim="q" class="srv-search" placeholder="按标题/路径/标签搜索…" />
          </label>
        </div>

        <div v-if="activeServer" class="srv-card">
          <div class="srv-head">
            <div class="srv-name">{{ activeServer.name }}</div>
            <div class="srv-meta">
              <span v-if="activeServer.host" class="srv-chip">host: {{ activeServer.host }}</span>
              <span v-if="activeServer.publicSite" class="srv-chip">site: {{ activeServer.publicSite }}</span>
            </div>
          </div>

          <div class="srv-base">
            <div class="srv-base-label">Base URL</div>
            <code class="srv-base-code">{{ activeServer.baseUrl }}</code>
            <button class="srv-copy" type="button" @click="copyText(activeServer.baseUrl)">复制</button>
          </div>

          <ul v-if="activeServer.notes?.length" class="srv-notes">
            <li v-for="(n, idx) in activeServer.notes" :key="idx">{{ n }}</li>
          </ul>

          <table class="doc-table srv-table">
            <thead>
              <tr>
                <th style="width: 96px">方法</th>
                <th>接口</th>
                <th style="width: 150px">分类</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(e, idx) in filteredEndpoints" :key="idx">
                <td><code>{{ e.method }}</code></td>
                <td class="srv-endpoint">
                  <div class="srv-endpoint-title">
                    <strong>{{ e.title }}</strong>
                    <span v-if="e.desc" class="srv-endpoint-desc">- {{ e.desc }}</span>
                  </div>
                  <div class="srv-endpoint-path">
                    <code>{{ e.path }}</code>
                    <button
                      class="srv-copy"
                      type="button"
                      @click="copyText(joinUrl(activeServer.baseUrl, e.path, e.method))"
                    >
                      复制完整地址
                    </button>
                  </div>
                </td>
                <td>
                  <code v-if="e.tag">{{ e.tag }}</code>
                </td>
              </tr>
              <tr v-if="filteredEndpoints.length === 0">
                <td colspan="3" class="srv-empty">没有匹配的接口</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="section">
        <h2>1. 接口基础</h2>
        <p><strong>Base URL：</strong><code>{{ apiBase }}</code></p>
        <p>所有接口返回 JSON，格式：<code>{"code": 0, "message": "success", "data": ...}</code></p>
        <p><code>code === 0</code> 表示成功，<code>data</code> 为业务数据。</p>
      </section>

      <section class="section">
        <h2>2. 版本检查（公开接口）</h2>
        <h3>获取最新版本</h3>
        <pre><code>GET {{ apiBase }}/versions/latest?channel=android</code></pre>
        <p><strong>参数：</strong></p>
        <ul>
          <li><code>channel</code>：渠道，可选 <code>android</code>、<code>ios</code>、<code>ohos</code></li>
        </ul>
        <p><strong>响应示例：</strong></p>
        <pre class="code-block">{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "versionName": "1.0.0",
    "versionCode": 1,
    "channel": "android",
    "downloadUrl": "https://...",
    "releaseNotes": "更新说明",
    "published": true,
    "createdAt": "2025-01-01T00:00:00"
  }
}</pre>
        <p>若 <code>data</code> 为 <code>null</code>，表示该渠道暂无已发布版本。</p>
      </section>

      <section class="section">
        <h2>3. 支付接入（微信/支付宝）</h2>

        <h3>3.1 订单状态说明</h3>
        <table class="doc-table">
          <thead>
            <tr><th>状态值</th><th>含义</th></tr>
          </thead>
          <tbody>
            <tr><td><code>pending</code></td><td>待付款：订单已创建，等待用户完成支付</td></tr>
            <tr><td><code>ordered</code></td><td>已下单：订单已确认，支付处理中</td></tr>
            <tr><td><code>paid</code></td><td>已付款：支付成功，订单完成</td></tr>
          </tbody>
        </table>

        <h3>3.2 支付方式</h3>
        <table class="doc-table">
          <thead>
            <tr><th>类型值</th><th>说明</th></tr>
          </thead>
          <tbody>
            <tr><td><code>wechat</code></td><td>微信支付（App 内调起微信客户端）</td></tr>
            <tr><td><code>alipay</code></td><td>支付宝（App 内调起支付宝客户端）</td></tr>
          </tbody>
        </table>

        <h3>3.3 客户端支付流程（通用）</h3>
        <ol class="step-list">
          <li><strong>创建订单</strong>：客户端向服务端发起下单请求，服务端生成订单号、金额，返回订单信息（含 orderNo、amount 等）</li>
          <li><strong>调起支付</strong>：客户端根据支付方式（微信/支付宝）调用对应 SDK，传入订单号、金额、商品描述等</li>
          <li><strong>用户支付</strong>：用户在微信/支付宝内完成付款</li>
          <li><strong>支付结果</strong>：SDK 通过回调/同步返回支付结果，客户端收到后通知服务端更新订单状态</li>
          <li><strong>服务端校验</strong>：服务端应通过微信/支付宝服务端接口校验支付真实性，再更新订单为 <code>paid</code></li>
        </ol>

        <h3>3.4 微信支付接入</h3>
        <p><strong>前置条件：</strong></p>
        <ul>
          <li>已开通微信支付商户号</li>
          <li>已创建移动应用并获取 AppID</li>
          <li>在微信开放平台完成应用与商户号关联</li>
        </ul>
        <p><strong>客户端步骤：</strong></p>
        <ol class="step-list">
          <li>集成微信开放平台 SDK（如 Android 使用 <code>com.tencent.mm.opensdk</code>）</li>
          <li>向服务端请求预支付，获取 <code>prepayId</code>、<code>partnerId</code>、<code>nonceStr</code>、<code>timeStamp</code>、<code>sign</code></li>
          <li>调用 <code>IWXAPI.sendReq()</code> 调起微信支付，传入 <code>PayReq</code></li>
          <li>在 <code>WXPayEntryActivity</code> 中接收支付结果（<code>onResp</code>），根据 errCode 判断成功/失败</li>
          <li>支付成功后调用服务端接口上报支付流水号（<code>transaction_id</code>），服务端校验并更新订单</li>
        </ol>
        <p><strong>服务端需实现：</strong></p>
        <ul>
          <li>统一下单接口：调用微信 <code>pay/unifiedorder</code> 获取 prepayId</li>
          <li>签名算法：MD5 或 HMAC-SHA256，按微信文档拼接参数后签名</li>
          <li>异步通知：接收微信支付结果通知，验签后更新订单，并返回 <code>&lt;xml&gt;&lt;return_code&gt;SUCCESS&lt;/return_code&gt;&lt;/xml&gt;</code></li>
        </ul>

        <h3>3.5 支付宝接入</h3>
        <p><strong>前置条件：</strong></p>
        <ul>
          <li>已注册支付宝开放平台开发者账号</li>
          <li>已创建应用并完成签约（App 支付产品）</li>
          <li>已配置应用公钥，获取支付宝公钥</li>
        </ul>
        <p><strong>客户端步骤：</strong></p>
        <ol class="step-list">
          <li>集成支付宝 SDK（如 Android 使用 <code>com.alipay.sdk</code>）</li>
          <li>向服务端请求订单信息，服务端生成 <code>orderInfo</code> 字符串（包含订单号、金额、商品描述等，已签名）</li>
          <li>调用 <code>PayTask.payV2(orderInfo, isShowPayLoading)</code> 调起支付宝</li>
          <li>在回调中解析 <code>PayResult</code>，根据 <code>resultStatus</code> 判断（9000 为成功）</li>
          <li>支付成功后调用服务端接口上报 <code>tradeNo</code>，服务端通过支付宝查询接口校验并更新订单</li>
        </ol>
        <p><strong>服务端需实现：</strong></p>
        <ul>
          <li>生成签名订单串：按支付宝文档拼接 biz_content、sign 等参数</li>
          <li>使用应用私钥对订单串签名（RSA2）</li>
          <li>异步通知：接收支付宝 <code>notify_url</code> 回调，验签后更新订单，返回 <code>success</code></li>
        </ul>

        <h3>3.6 安全建议</h3>
        <ul>
          <li>金额、订单号等关键参数由服务端生成，客户端不可篡改</li>
          <li>支付结果必须以服务端异步通知或主动查询为准，不可仅依赖客户端回调</li>
          <li>密钥、证书等敏感信息仅保存在服务端，不得打包进客户端</li>
          <li>生产环境必须使用 HTTPS</li>
        </ul>
      </section>

      <section class="section">
        <h2>4. IM 即时通讯对接</h2>
        <p>
          IM 使用 <strong>WebSocket</strong>，与上文的 HTTP Base URL <strong>同主机、同站点路径前缀</strong>，便于走 HTTPS/WSS 与反向代理。
          每条消息为 <strong>一行 JSON 文本</strong>（UTF-8），字段 <code>type</code> 表示指令类型。
        </p>

        <h3>4.1 连接地址</h3>
        <p><strong>推荐（与官网同源）：</strong></p>
        <pre><code>{{ imWsUrl }}</code></pre>
        <p>
          与当前页面同源：浏览器打开官网为 <code>https</code> 时请使用 <code>wss://</code>（上表已按当前协议生成）。
          本地开发若前端为 Vite、后端为 Spring，请在 Vite 中对 <code>/api</code> 开启 WebSocket 代理（仓库已配置 <code>ws: true</code>）。
        </p>
        <p><strong>可选（直连 Netty，需服务端放行端口）：</strong></p>
        <pre><code>{{ imWsNettyUrl }}</code></pre>
        <p>一般仅内网调试或专用客户端使用；公网部署更推荐只暴露 443，由 Nginx 将 <code>/api/</code>（含 WebSocket 升级）反代到 Spring。</p>

        <h3>4.2 接入流程</h3>
        <ol class="step-list">
          <li>建立 WebSocket 连接到上述地址。</li>
          <li>连接成功后<strong>必须先发送鉴权</strong>（见下节 <code>AUTH</code>），否则其他指令会返回 <code>ERROR</code>。</li>
          <li>鉴权成功后即可收发单聊、群聊、拉历史、拉会话列表等。</li>
          <li>同一 <code>userId</code> 仅保留<strong>一条在线连接</strong>：新连接建立后，旧连接会被服务端关闭。</li>
        </ol>

        <h3>4.3 客户端 → 服务端</h3>
        <table class="doc-table">
          <thead>
            <tr><th>type</th><th>说明</th><th>主要字段</th></tr>
          </thead>
          <tbody>
            <tr>
              <td><code>AUTH</code></td>
              <td>登录绑定（必发）</td>
              <td><code>userId</code>（数字，须为已存在用户）</td>
            </tr>
            <tr>
              <td><code>PRIVATE_SEND</code></td>
              <td>单聊发送</td>
              <td><code>toUserId</code>、<code>body</code></td>
            </tr>
            <tr>
              <td><code>GROUP_SEND</code></td>
              <td>群聊发送</td>
              <td><code>groupId</code>、<code>body</code>（须已在群内）</td>
            </tr>
            <tr>
              <td><code>GROUP_CREATE</code></td>
              <td>建群</td>
              <td><code>name</code>（群名）；创建者自动入群</td>
            </tr>
            <tr>
              <td><code>GROUP_JOIN</code></td>
              <td>加入群</td>
              <td><code>groupId</code></td>
            </tr>
            <tr>
              <td><code>HISTORY</code></td>
              <td>历史消息</td>
              <td><code>mode</code>：<code>P2P</code> 时带 <code>peerUserId</code>；<code>GROUP</code> 时带 <code>groupId</code>；可选 <code>beforeId</code>、<code>limit</code></td>
            </tr>
            <tr>
              <td><code>CONVERSATIONS</code></td>
              <td>最近会话列表</td>
              <td>无额外字段</td>
            </tr>
            <tr>
              <td><code>USER_INFO</code></td>
              <td>查询用户资料</td>
              <td><code>userId</code></td>
            </tr>
            <tr>
              <td><code>GROUP_INFO</code></td>
              <td>查询群资料与成员</td>
              <td><code>groupId</code></td>
            </tr>
          </tbody>
        </table>

        <h3>4.4 服务端 → 客户端（常见）</h3>
        <table class="doc-table">
          <thead>
            <tr><th>type</th><th>含义</th></tr>
          </thead>
          <tbody>
            <tr><td><code>AUTH_OK</code></td><td>鉴权成功，含 <code>userId</code></td></tr>
            <tr><td><code>PRIVATE_MESSAGE</code></td><td>单聊下行（含 <code>msgId</code>、<code>fromUserId</code>、<code>toUserId</code>、<code>body</code>、<code>createdAt</code>）</td></tr>
            <tr><td><code>GROUP_MESSAGE</code></td><td>群聊下行</td></tr>
            <tr><td><code>GROUP_CREATED</code></td><td>建群成功，含 <code>groupId</code>、<code>name</code></td></tr>
            <tr><td><code>GROUP_JOIN_OK</code></td><td>入群结果</td></tr>
            <tr><td><code>GROUP_SYSTEM</code></td><td>群系统提示（如成员加入）</td></tr>
            <tr><td><code>HISTORY_RESULT</code></td><td>历史列表，<code>messages</code> 为数组</td></tr>
            <tr><td><code>CONVERSATIONS_RESULT</code></td><td>最近会话，<code>items</code> 为数组</td></tr>
            <tr><td><code>USER_INFO_RESULT</code></td><td>用户资料（不含密码）</td></tr>
            <tr><td><code>GROUP_INFO_RESULT</code></td><td>群信息与 <code>memberIds</code></td></tr>
            <tr><td><code>ERROR</code></td><td>失败，<code>message</code> 为原因</td></tr>
          </tbody>
        </table>

        <h3>4.5 示例</h3>
        <p><strong>鉴权：</strong></p>
        <pre class="code-block">{"type":"AUTH","userId":101}</pre>
        <p><strong>单聊：</strong></p>
        <pre class="code-block">{"type":"PRIVATE_SEND","toUserId":102,"body":"你好"}</pre>
        <p><strong>建群并发消息：</strong></p>
        <pre class="code-block">{"type":"GROUP_CREATE","name":"产品讨论组"}
{"type":"GROUP_SEND","groupId":1,"body":"大家好"}</pre>
        <p>
          联调可使用站内
          <a class="doc-inline-link" href="/im-chat.html" target="_blank" rel="noopener">IM 测试页</a>
          （静态资源 <code>/im-chat.html</code>，与官网同源部署即可访问）。
        </p>
      </section>

      <section class="section">
        <h2>5. 其他公开接口</h2>
        <h3>Hello 测试</h3>
        <pre><code>GET {{ apiBase }}/hello</code></pre>
        <p>返回 <code>{"code":0,"message":"success","data":"Hello World"}</code></p>
      </section>

      <section class="section">
        <h2>6. 错误码</h2>
        <ul>
          <li><code>code: 0</code> - 成功</li>
          <li><code>code: 非 0</code> - 失败，<code>message</code> 为错误描述</li>
        </ul>
      </section>

      <div class="back-link">
        <router-link to="/">← 返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { apiCatalog } from '../config/apiCatalog'

const apiBase = computed(() => {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return `${origin}/api`
})

const docRef = ref(null)
const tocItems = ref([])
const activeTocId = ref('')
let onScroll = null

const servers = apiCatalog.servers || []
const activeServerId = ref(servers[0]?.id || '')
const q = ref('')

const activeServer = computed(() => servers.find((s) => s.id === activeServerId.value) || servers[0] || null)

const filteredEndpoints = computed(() => {
  const s = activeServer.value
  if (!s?.endpoints?.length) return []
  const key = (q.value || '').toLowerCase()
  if (!key) return s.endpoints
  return s.endpoints.filter((e) => {
    const hay =
      `${e.tag || ''} ${e.method || ''} ${e.title || ''} ${e.desc || ''} ${e.path || ''}`.toLowerCase()
    return hay.includes(key)
  })
})

function joinUrl(baseUrl, path, method) {
  // WS 的路径在文档里通常写成 /im/ws（同源），这里给出更直观的完整 URL
  if ((method || '').toUpperCase() === 'WS') {
    try {
      const u = new URL(baseUrl)
      const wsProto = u.protocol === 'https:' ? 'wss:' : 'ws:'
      return `${wsProto}//${u.host}${path.startsWith('/') ? path : `/${path}`}`
    } catch {
      return path
    }
  }
  const b = String(baseUrl || '').replace(/\/+$/, '')
  const p = String(path || '')
  if (!p) return b
  if (/^https?:\/\//i.test(p) || /^wss?:\/\//i.test(p)) return p
  return `${b}${p.startsWith('/') ? '' : '/'}${p}`
}

async function copyText(text) {
  const s = String(text || '').trim()
  if (!s) return
  try {
    await navigator.clipboard.writeText(s)
  } catch {
    // ignore: 某些浏览器无权限时不抛 UI
  }
}

/** 与 HTTP 同源：/api/im/ws（经 Nginx/Vite 反代到 Spring） */
const imWsUrl = computed(() => {
  if (typeof window === 'undefined') {
    return 'ws://localhost/api/im/ws'
  }
  const wsProto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${wsProto}//${window.location.host}/api/im/ws`
})

/** 直连 Netty（可选，需放行 19080） */
const imWsNettyUrl = computed(() => {
  if (typeof window === 'undefined') {
    return 'ws://localhost:19080/im/ws'
  }
  const wsProto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const h = window.location.hostname || 'localhost'
  return `${wsProto}//${h}:19080/im/ws`
})

function slugify(text) {
  return String(text || '')
    .trim()
    .toLowerCase()
    .replace(/[^\u4e00-\u9fa5a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 64)
}

function ensureHeadingIds(rootEl) {
  const map = new Map()
  const headings = Array.from(rootEl.querySelectorAll('h2, h3'))
  const items = []
  for (const h of headings) {
    const level = h.tagName === 'H2' ? 2 : 3
    const text = (h.textContent || '').trim()
    if (!text) continue

    let id = h.id?.trim()
    if (!id) id = slugify(text) || `sec-${level}`

    const used = map.get(id) || 0
    map.set(id, used + 1)
    if (used > 0) id = `${id}-${used + 1}`

    h.id = id
    items.push({ id, level, text })
  }
  return items
}

function computeActiveId(items) {
  if (!items.length) return ''
  const fromTop = 110
  for (let i = items.length - 1; i >= 0; i--) {
    const el = document.getElementById(items[i].id)
    if (!el) continue
    const rect = el.getBoundingClientRect()
    if (rect.top <= fromTop) return items[i].id
  }
  return items[0].id
}

function scrollToId(id) {
  const el = document.getElementById(id)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  history.replaceState(null, '', `#${encodeURIComponent(id)}`)
}

onMounted(() => {
  nextTick(() => {
    const root = docRef.value
    if (!root) return
    const items = ensureHeadingIds(root)
    tocItems.value = items
    activeTocId.value = computeActiveId(items)

    onScroll = () => {
      activeTocId.value = computeActiveId(tocItems.value)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
  })
})

onUnmounted(() => {
  if (onScroll) window.removeEventListener('scroll', onScroll)
  onScroll = null
})
</script>

<style scoped>
.api-doc {
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 2rem 1rem;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 18px;
}

.doc-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e5e5ea;
  padding: 2rem 2.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.toc {
  position: sticky;
  top: 86px;
  align-self: start;
  height: calc(100vh - 100px);
  overflow: auto;
  border: 1px solid #e5e5ea;
  border-radius: 14px;
  background: #fff;
  padding: 12px 12px 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.toc-title {
  font-weight: 700;
  color: #1d1d1f;
  margin-bottom: 10px;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.toc-item {
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 10px;
  padding: 7px 8px;
  cursor: pointer;
  color: #333;
  font-size: 0.92rem;
  line-height: 1.25;
}

.toc-item.h3 {
  padding-left: 16px;
  font-size: 0.9rem;
  color: #444;
}

.toc-item:hover {
  background: #f5f5f7;
}

.toc-item.active {
  border-color: rgba(0, 113, 227, 0.28);
  background: rgba(0, 113, 227, 0.08);
  color: #0b5cab;
}

.toc-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 980px) {
  .api-doc {
    grid-template-columns: 1fr;
  }
  .toc {
    display: none;
  }
}

.title {
  font-size: 1.75rem;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 0.35rem;
}

.subtitle {
  color: #6e6e73;
  font-size: 0.95rem;
  margin-bottom: 2rem;
}

.section {
  margin-bottom: 2rem;
}

.srv-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 0.75rem 0 1rem;
}

.srv-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
}

.srv-field-grow {
  flex: 1;
  min-width: 260px;
}

.srv-label {
  font-size: 0.85rem;
  color: #6e6e73;
}

.srv-select,
.srv-search {
  height: 38px;
  border-radius: 10px;
  border: 1px solid #e5e5ea;
  padding: 0 10px;
  outline: none;
}

.srv-select:focus,
.srv-search:focus {
  border-color: rgba(0, 113, 227, 0.55);
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.14);
}

.srv-card {
  border: 1px solid #e5e5ea;
  border-radius: 14px;
  padding: 14px;
  background: #fbfbfd;
}

.srv-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.srv-name {
  font-weight: 700;
  color: #1d1d1f;
}

.srv-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.srv-chip {
  font-size: 0.82rem;
  color: #1d1d1f;
  background: #f5f5f7;
  border: 1px solid #e5e5ea;
  border-radius: 999px;
  padding: 2px 10px;
}

.srv-base {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 10px 0 8px;
}

.srv-base-label {
  color: #6e6e73;
  font-size: 0.9rem;
}

.srv-base-code {
  display: inline-block;
}

.srv-copy {
  height: 30px;
  padding: 0 10px;
  border-radius: 10px;
  border: 1px solid #e5e5ea;
  background: #fff;
  cursor: pointer;
  font-size: 0.85rem;
}

.srv-copy:hover {
  border-color: rgba(0, 113, 227, 0.45);
}

.srv-notes {
  margin: 0.4rem 0 0.75rem;
  color: #444;
  font-size: 0.92rem;
  line-height: 1.55;
}

.srv-table {
  margin-top: 0.6rem;
}

.srv-endpoint-title {
  margin-bottom: 6px;
}

.srv-endpoint-desc {
  color: #6e6e73;
  font-weight: 400;
}

.srv-endpoint-path {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.srv-empty {
  color: #6e6e73;
  text-align: center;
  padding: 14px 10px;
}

.section h2 {
  font-size: 1.2rem;
  color: #1d1d1f;
  margin-bottom: 0.75rem;
  padding-bottom: 0.35rem;
  border-bottom: 1px solid #e5e5ea;
}

.section h3 {
  font-size: 1rem;
  color: #333;
  margin: 1rem 0 0.5rem;
}

.section p,
.section ul {
  color: #444;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 0.5rem;
}

.section ul {
  padding-left: 1.5rem;
}

.section li {
  margin-bottom: 0.25rem;
}

.doc-table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.75rem 0;
  font-size: 0.9rem;
}

.doc-table th,
.doc-table td {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e5ea;
  text-align: left;
}

.doc-table th {
  background: #f5f5f7;
  font-weight: 500;
  color: #333;
}

.doc-table td code {
  font-size: 0.85em;
}

.step-list {
  padding-left: 1.5rem;
  margin: 0.75rem 0;
}

.step-list li {
  margin-bottom: 0.6rem;
  line-height: 1.6;
}

code {
  background: #f5f5f7;
  padding: 0.15em 0.4em;
  border-radius: 6px;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 0.9em;
}

pre {
  margin: 0.75rem 0;
  padding: 1rem 1.25rem;
  background: #1d1d1f;
  color: #d4d4d4;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 0.85rem;
  line-height: 1.5;
}

pre code {
  background: none;
  padding: 0;
  color: inherit;
}

.code-block {
  white-space: pre-wrap;
  word-break: break-all;
}

.back-link {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e5ea;
}

.back-link a {
  color: #0071e3;
  text-decoration: none;
  font-size: 0.95rem;
}

.back-link a:hover {
  text-decoration: underline;
}

.doc-inline-link {
  color: #0071e3;
  text-decoration: none;
  font-weight: 500;
}

.doc-inline-link:hover {
  text-decoration: underline;
}
</style>
