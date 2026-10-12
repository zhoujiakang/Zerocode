import test from 'node:test'
import assert from 'node:assert/strict'
import { createPinia } from 'pinia'
import { useAppStore } from '../src/stores/app.js'
import { createApp, generateApp, readAppFile } from '../src/api/app.js'

globalThis.window = new EventTarget()
const state = () => useAppStore(createPinia())
const app = (id, status = 'CREATED') => ({ id, status, appName: '测试应用', initPrompt: '创建个人主页', codeGenType: 'HTML' })
const reply = (data, status = 200, code = 0, message = 'ok') => new Response(JSON.stringify({ data, code, message }), { status })
const page = (records = [], total = records.length) => ({ current: 1, pageSize: 20, total, records })

test('application API retains string ids and sends generation with a separate long timeout', async t => {
  const timers = []
  const original = globalThis.setTimeout
  t.mock.method(globalThis, 'setTimeout', (callback, milliseconds) => { timers.push(milliseconds); return original(callback, milliseconds) })
  globalThis.fetch = async (url, options) => {
    assert.equal(options.credentials, 'include')
    if (url === '/api/app/add') return reply('9007199254740993')
    if (url.includes('/file?')) { assert.ok(url.endsWith('fileName=index.html')); return reply('<html></html>') }
    assert.equal(JSON.parse(options.body).appId, '9007199254740993'); return reply(app('9007199254740993', 'READY'))
  }
  assert.equal(await createApp({}), '9007199254740993')
  await generateApp({ appId: '9007199254740993' }); await readAppFile('9007199254740993', 'index.html')
  assert.deepEqual(timers, [10000, 1800000, 10000])
})
test('ready applications load persisted history and all three source files', async () => {
  globalThis.fetch = async url => {
    if (url.endsWith('/history')) return reply(page([{ id: '1', role: 'user', content: '需求' }]))
    if (url.endsWith('/files')) return reply(['index.html', 'style.css', 'script.js'])
    if (url.includes('/file?')) return reply(new URL(url, 'http://test').searchParams.get('fileName'))
    return reply({ ...app('123', 'READY'), codeGenType: 'MULTI_FILE' })
  }
  const store = state(); await store.load('123')
  assert.deepEqual(Object.keys(store.files), ['index.html', 'style.css', 'script.js'])
  assert.equal(store.history[0].content, '需求'); assert.equal(store.busy, false)
})
test('first generation uses stored prompt and followups send the new description', async () => {
  let serverApp = app('1'), history = [], bodies = []
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/generate')) {
      const body = JSON.parse(options.body); bodies.push(body); serverApp = app('1', 'READY')
      history.push({ id: String(history.length + 1), role: 'user', content: body.message || '创建个人主页' })
      return reply(serverApp)
    }
    if (url.endsWith('/history')) return reply(page(history))
    if (url.endsWith('/files')) return reply(['index.html'])
    if (url.includes('/file?')) return reply('<html>最新代码</html>')
    return reply(serverApp)
  }
  const store = state(); await store.load('1')
  assert.equal(await store.generate(), true); assert.equal(await store.generate(' 改成中文 '), true)
  assert.deepEqual(bodies, [{ appId: '1' }, { appId: '1', message: '改成中文' }])
  assert.equal(store.history.length, 2); assert.equal(store.app.status, 'READY')
})
test('unconfigured model preserves the application and provides a retryable error', async () => {
  globalThis.fetch = async url => {
    if (url.endsWith('/generate')) return reply(null, 503, 50300, 'AI 模型尚未配置')
    if (url.endsWith('/history')) return reply(page())
    return reply(app('1'))
  }
  const store = state(); await store.load('1'); assert.equal(await store.generate(), false)
  assert.equal(store.app.status, 'CREATED'); assert.equal(store.error, 'AI 模型尚未配置'); assert.equal(store.busy, false)
})
test('unknown server state prevents duplicate generation until refresh confirms it', async () => {
  const store = state()
  globalThis.fetch = async url => url.endsWith('/history') ? reply(page()) : reply(app('1'))
  await store.load('1')
  globalThis.fetch = async () => { throw new Error('network lost') }
  assert.equal(await store.generate(), false); assert.equal(store.busy, true)
  let calls = 0
  globalThis.fetch = async url => { calls++; return url.endsWith('/history') ? reply(page()) : reply(app('1', 'FAILED')) }
  assert.equal(await store.generate('再次尝试'), false); assert.equal(calls, 0)
  await store.refresh(); assert.equal(store.busy, false); assert.equal(store.app.status, 'FAILED')
})
test('late detail response cannot overwrite a different application', async () => {
  let resolveOld
  globalThis.fetch = async url => {
    if (url === '/api/app/1') return new Promise(resolve => { resolveOld = resolve })
    if (url.endsWith('/history')) return reply(page())
    return reply(app('2'))
  }
  const store = state(), old = store.load('1'); await store.load('2')
  resolveOld(reply(app('1'))); await old
  assert.equal(store.app.id, '2'); assert.equal(store.loading, false)
})
test('late generation response after navigation cannot overwrite the new workspace', async () => {
  let resolveGeneration
  globalThis.fetch = async url => {
    if (url.endsWith('/generate')) return new Promise(resolve => { resolveGeneration = resolve })
    if (url.endsWith('/history')) return reply(page())
    return reply(app(url.endsWith('/2') ? '2' : '1'))
  }
  const store = state(); await store.load('1'); const pending = store.generate()
  assert.equal(store.generating, true); await store.load('2'); resolveGeneration(reply(app('1', 'READY')))
  assert.equal(await pending, false); assert.equal(store.app.id, '2'); assert.equal(store.generating, false)
})
test('history pages append in order without losing earlier conversations', async () => {
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/history')) { const current = JSON.parse(options.body).current; return reply(page([{ id: String(current), content: `第${current}页` }], 2)) }
    return reply(app('1'))
  }
  const store = state(); await store.load('1'); await store.loadHistory(true)
  assert.deepEqual(store.history.map(item => item.id), ['1', '2']); assert.equal(store.historyTotal, 2)
})
