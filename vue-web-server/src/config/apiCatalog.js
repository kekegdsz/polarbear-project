/**
 * 官网 API 文档的数据源（所有服务器集中维护在这里）
 *
 * - 新增服务器：往 servers 里追加一项
 * - 新增接口：往 server.endpoints 里追加一项
 *
 * 约定：
 * - baseUrl 写成完整 URL（建议带 /api），例如: https://example.com/api
 * - path 只写接口路径（不含 baseUrl），例如: /versions/latest
 */

export const apiCatalog = {
  servers: [
    {
      id: 'tianyi-prod',
      name: '天翼云（生产）',
      host: '183.56.251.215',
      publicSite: 'http://183.56.251.215:8081',
      baseUrl: 'http://183.56.251.215:8081/api',
      notes: ['Nginx 对外端口 8081', 'Spring 内网 8082，仅供反代', 'WebSocket：/api/im/ws'],
      endpoints: [
        {
          tag: '版本',
          method: 'GET',
          path: '/versions/latest?channel=android',
          title: '获取最新版本',
          desc: 'channel 可选 android/ios/ohos'
        },
        { tag: '公开', method: 'GET', path: '/hello', title: 'Hello 测试' },
        { tag: 'IM', method: 'WS', path: '/im/ws', title: 'IM WebSocket（同源反代）' }
      ]
    }
  ]
}

