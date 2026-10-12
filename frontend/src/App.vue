<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from './stores/user'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import BasicLayout from './layouts/BasicLayout.vue'
const router = useRouter()
const userStore = useUserStore()
function handleUnauthorized() {
  userStore.clearUser()
  if (router.currentRoute.value.meta.requiresAuth) {
    message.info('登录状态已失效，请重新登录')
    router.replace({ path: '/user/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}
onMounted(() => window.addEventListener('zerocode:unauthorized', handleUnauthorized))
onUnmounted(() => window.removeEventListener('zerocode:unauthorized', handleUnauthorized))
const theme = { token: { colorPrimary: '#5363e9', borderRadius: 8, fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif' } }
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="theme"><BasicLayout /></a-config-provider>
</template>

<style>
* { box-sizing: border-box; }
html, body, #app { margin: 0; min-height: 100%; }
body { color: #25283c; background: #f7f8fc; font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; -webkit-font-smoothing: antialiased; }
a { text-decoration: none; }
button, a, input, textarea { -webkit-tap-highlight-color: transparent; }
:focus-visible { outline: 2px solid #5363e9; outline-offset: 4px; }
.page { width: 100%; max-width: 1160px; margin: 0 auto; }
.page-heading { font-size: 30px; letter-spacing: -1px; font-weight: 700; margin: 0 0 12px; color: #25283c; }
.page-description { color: #85899d; line-height: 1.8; margin: 0; }
</style>
