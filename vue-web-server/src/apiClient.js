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
    throw new Error(
      `服务返回的不是 JSON（可能是错误页或重定向）。状态码 ${resp.status}，返回内容片段：` +
        text.slice(0, 120)
    )
  }

  const json = await resp.json()
  return { resp, json }
}

