import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser, loginUser, logoutUser } from '../api/user.js'

export const useUserStore = defineStore('user', () => {
  const loginUserInfo = ref(null)
  const initialized = ref(false)
  const loadingError = ref('')
  const isLoggedIn = computed(() => !!loginUserInfo.value)
  const isAdmin = computed(() => loginUserInfo.value?.userRole === 'admin')
  let pending = null
  let revision = 0
  function clearUser() { revision++; loginUserInfo.value = null; initialized.value = true }
  async function fetchLoginUser(force = false) {
    if (!force && initialized.value) return loginUserInfo.value
    if (pending) return pending
    const currentRevision = revision
    pending = (async () => {
      try {
        const user = await getLoginUser()
        if (currentRevision === revision) { loginUserInfo.value = user; initialized.value = true; loadingError.value = '' }
        return loginUserInfo.value
      } catch (error) {
        if (currentRevision === revision) {
          if (error.status === 401) { loginUserInfo.value = null; initialized.value = true; loadingError.value = ''; return null }
          loadingError.value = error.message
        }
        throw error
      } finally { pending = null }
    })()
    return pending
  }
  async function login(values) {
    const user = await loginUser(values)
    revision++; loginUserInfo.value = user; initialized.value = true; loadingError.value = ''
    return user
  }
  async function logout() {
    await logoutUser()
    clearUser()
    loadingError.value = ''
  }
  return { loginUserInfo, initialized, loadingError, isLoggedIn, isAdmin, clearUser, fetchLoginUser, login, logout }
})
