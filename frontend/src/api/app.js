import { request } from './request.js'
const idPath = id => encodeURIComponent(String(id))
export const createApp = body => request('/app/add', { method: 'POST', body })
export const listMyApps = body => request('/app/list/my', { method: 'POST', body })
export const getApp = id => request(`/app/${idPath(id)}`)
export const getAppHistory = (id, body) => request(`/app/${idPath(id)}/history`, { method: 'POST', body })
export const getAppFiles = id => request(`/app/${idPath(id)}/files`)
export const readAppFile = (id, name) => request(`/app/${idPath(id)}/file?fileName=${encodeURIComponent(name)}`)
// 每轮模型超时 120 秒，最多 10 轮工具调用；普通接口仍使用 10 秒超时。
export const generateApp = body => request('/app/generate', { method: 'POST', body, timeoutMs: 30 * 60 * 1000 })
export const appStatus = {
  CREATED: { label: '待生成', color: 'default' }, GENERATING: { label: '生成中', color: 'processing' },
  READY: { label: '已生成', color: 'success' }, FAILED: { label: '生成失败', color: 'error' },
}
