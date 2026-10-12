import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getApp, getAppHistory, getAppFiles, readAppFile, generateApp } from '../api/app.js'

export const useAppStore = defineStore('app', () => {
  const app = ref(null), history = ref([]), historyTotal = ref(0), historyPage = ref(0)
  const files = ref({}), loading = ref(false), generating = ref(false), historyLoading = ref(false)
  const error = ref(''), filesError = ref(''), uncertain = ref(false)
  const busy = computed(() => generating.value || uncertain.value || app.value?.status === 'GENERATING')
  let revision = 0
  function clear() {
    revision++; app.value = null; history.value = []; historyTotal.value = 0; historyPage.value = 0
    files.value = {}; error.value = ''; filesError.value = ''; loading.value = false; generating.value = false; historyLoading.value = false; uncertain.value = false
  }
  async function loadHistory(append = false) {
    if (!app.value || historyLoading.value) return
    const token = revision, id = app.value.id, current = append ? historyPage.value + 1 : 1
    historyLoading.value = true
    try {
      const page = await getAppHistory(id, { current, pageSize: 20 })
      if (token !== revision) return
      history.value = append ? [...history.value, ...page.records] : page.records
      historyTotal.value = page.total; historyPage.value = current
    } finally { if (token === revision) historyLoading.value = false }
  }
  async function loadFiles() {
    const token = revision, id = app.value?.id
    if (!id || app.value.status !== 'READY') { files.value = {}; return }
    filesError.value = ''
    try {
      const names = await getAppFiles(id)
      const sources = await Promise.all(names.map(async name => [name, await readAppFile(id, name)]))
      if (token === revision) files.value = Object.fromEntries(sources)
    } catch (failure) { if (token === revision) { files.value = {}; filesError.value = failure.message } }
  }
  async function load(id) {
    clear(); const token = revision; loading.value = true
    try {
      const value = await getApp(id)
      if (token !== revision) return
      app.value = value
      await Promise.all([loadHistory(), loadFiles()])
    } catch (failure) { if (token === revision) error.value = failure.message }
    finally { if (token === revision) loading.value = false }
  }
  async function refresh(force = false) {
    if (!app.value || (generating.value && !force)) return
    const token = revision, previous = app.value.status
    const value = await getApp(app.value.id)
    if (token !== revision) return
    app.value = value; uncertain.value = false
    if (previous !== value.status || value.status !== 'GENERATING') await Promise.all([loadHistory(), loadFiles()])
  }
  async function generate(message) {
    if (!app.value || busy.value || loading.value) return false
    const token = ++revision, body = { appId: app.value.id }
    historyLoading.value = false
    if (message?.trim()) body.message = message.trim()
    generating.value = true; error.value = ''; files.value = {}
    try {
      const value = await generateApp(body)
      if (token !== revision) return false
      app.value = value
      await Promise.all([loadHistory(), loadFiles()])
      return true
    } catch (failure) {
      if (token !== revision) return false
      error.value = failure.message; uncertain.value = true
      // 请求断开不代表后端任务取消，重新查询实际状态后才允许重试。
      try { await refresh(true) } catch { error.value += '；暂时无法确认生成状态，请刷新页面后查看' }
      return false
    } finally { if (token === revision) generating.value = false }
  }
  return { app, history, historyTotal, historyPage, files, loading, generating, historyLoading, error, filesError, uncertain,
    busy, clear, load, refresh, loadHistory, loadFiles, generate }
})
