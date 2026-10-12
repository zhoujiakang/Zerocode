<script setup>
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import BasicLayout from './layouts/BasicLayout.vue'
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
