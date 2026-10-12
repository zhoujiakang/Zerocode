export class ApiError extends Error {
  constructor(message, code, status) { super(message); this.code = code; this.status = status }
}
export async function request(path, { method = 'GET', body, silentUnauthorized = false } = {}) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 10000)
  try {
    const response = await fetch(`/api${path}`, {
      method, credentials: 'include', signal: controller.signal,
      headers: { Accept: 'application/json', ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    let result
    try { result = await response.json() } catch { throw new ApiError('服务暂时不可用，请稍后重试', undefined, response.status) }
    if (!response.ok || result.code !== 0) {
      if (response.status === 401 && !silentUnauthorized) window.dispatchEvent(new Event('zerocode:unauthorized'))
      throw new ApiError(result.message || '请求失败', result.code, response.status)
    }
    return result.data
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError(error.name === 'AbortError' ? '请求超时，请稍后重试' : '无法连接服务，请检查后端是否已启动')
  } finally { clearTimeout(timeout) }
}
