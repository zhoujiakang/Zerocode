import test from 'node:test'
import assert from 'node:assert/strict'
import { createPinia } from 'pinia'
import { useUserStore } from '../src/stores/user.js'
import { request } from '../src/api/request.js'
import { safeRedirect } from '../src/utils/navigation.js'
import { createRouter, createMemoryHistory } from 'vue-router'

globalThis.window = new EventTarget()
const user = { id: '9007199254740993', userAccount: 'alice', userName: 'Alice', userRole: 'user' }
const reply = (data, status = 200, code = 0, message = 'ok') => new Response(JSON.stringify({ code, data, message }), { status })
const store = () => useUserStore(createPinia())

test('restores current user once and retains string IDs', async () => {
  let calls = 0
  globalThis.fetch = async () => { calls++; return reply(user) }
  const state = store()
  await Promise.all([state.fetchLoginUser(), state.fetchLoginUser()])
  await state.fetchLoginUser()
  assert.equal(calls, 1); assert.equal(state.loginUserInfo.id, user.id); assert.equal(state.isAdmin, false)
})
test('401 means guest while server failure stays retryable', async () => {
  globalThis.fetch = async () => reply(null, 401, 40100, '未登录')
  const guest = store(); await guest.fetchLoginUser()
  assert.equal(guest.isLoggedIn, false); assert.equal(guest.initialized, true)
  globalThis.fetch = async () => reply(null, 500, 50000, '服务异常')
  const failed = store(); await assert.rejects(failed.fetchLoginUser(), /服务异常/)
  assert.equal(failed.initialized, false); assert.equal(failed.loadingError, '服务异常')
  globalThis.fetch = async () => reply(user)
  await failed.fetchLoginUser(); assert.equal(failed.isLoggedIn, true)
})
test('successful login and logout update state; failed logout retains user', async () => {
  globalThis.fetch = async () => reply({ ...user, userRole: 'admin' })
  const state = store(); await state.login({ userAccount: 'alice', userPassword: 'Password123' })
  assert.equal(state.isAdmin, true)
  globalThis.fetch = async () => reply(null, 500, 50000, '退出失败')
  await assert.rejects(state.logout()); assert.equal(state.isLoggedIn, true)
  globalThis.fetch = async () => reply(true)
  await state.logout(); assert.equal(state.isLoggedIn, false)
})
test('an old in-flight restore cannot replace a newly logged-in user', async () => {
  let resolveFetch
  globalThis.fetch = () => new Promise(resolve => { resolveFetch = resolve })
  const state = store(); const pending = state.fetchLoginUser()
  globalThis.fetch = async () => reply({ ...user, userAccount: 'admin', userRole: 'admin' })
  await state.login({})
  resolveFetch(reply(user)); await pending
  assert.equal(state.loginUserInfo.userAccount, 'admin')
})
test('API sends cookie credentials and emits session expiration for protected calls', async () => {
  let eventCount = 0
  const listener = () => eventCount++
  window.addEventListener('zerocode:unauthorized', listener)
  try {
    globalThis.fetch = async (url, options) => { assert.equal(options.credentials, 'include'); return reply(null, 401, 40100, '未登录') }
    await assert.rejects(request('/user/list/page', { method: 'POST', body: {} }))
    await assert.rejects(request('/user/get/login', { silentUnauthorized: true }))
    assert.equal(eventCount, 1)
  } finally { window.removeEventListener('zerocode:unauthorized', listener) }
})
test('login redirect accepts known internal routes and rejects external or missing destinations', () => {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: {} }, { path: '/admin/user', component: {} }, { path: '/:pathMatch(.*)*', name: 'not-found', component: {} }] })
  assert.equal(safeRedirect('/admin/user?from=login', router), '/admin/user?from=login')
  for (const value of ['https://example.com', '//example.com', '/missing', '/user/login', null]) assert.equal(safeRedirect(value, router), '/')
})
