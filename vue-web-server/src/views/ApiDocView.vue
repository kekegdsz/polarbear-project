<template>
  <div class="api-doc">
    <div class="doc-card">
      <h1 class="title">客户端 API 接入文档</h1>
      <p class="subtitle">苍穹之下 - 客户端接入说明</p>

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
        <h2>4. 其他公开接口</h2>
        <h3>Hello 测试</h3>
        <pre><code>GET {{ apiBase }}/hello</code></pre>
        <p>返回 <code>{"code":0,"message":"success","data":"Hello World"}</code></p>
      </section>

      <section class="section">
        <h2>5. 错误码</h2>
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
import { computed } from 'vue'

const apiBase = computed(() => {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return `${origin}/api`
})
</script>

<style scoped>
.api-doc {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.doc-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e5e5ea;
  padding: 2rem 2.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
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
</style>
