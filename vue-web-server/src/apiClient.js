export async function apiRequest(path, options = {}) {
  const resp = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  })

  const contentType = resp.headers.get('content-type') || ''

  // 如果不是 JSON，读取文本并抛出更清晰的错误，避免 JSON.parse 报错
  if (!contentType.includes('application/json')) {
    const text = await resp.text()
    let hint = ''
    if (resp.status === 502 || resp.status === 503) {
      hint =
        '（多为 Nginx 反代连不上后端：Java 正在重启、未监听 8082，或与宝塔 Java 项目重复启动冲突；可稍后重试，或在服务器执行 curl http://127.0.0.1:8082/api/hello）'
    }
    throw new Error(
      `服务返回的不是 JSON${hint}。状态码 ${resp.status}，返回内容片段：` + text.slice(0, 120)
    )
  }

  const json = await resp.json()
  return { resp, json }
}

