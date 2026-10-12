<script setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { UserOutlined } from '@ant-design/icons-vue'
import logo from '../assets/logo.png'
import { siteConfig, menuItems } from '../config/site'
const route = useRoute()
const selectedKeys = computed(() => [menuItems.find(item => item.key === route.path || (item.key !== '/' && route.path.startsWith(item.key + '/')))?.key].filter(Boolean))
</script>

<template>
  <a href="#main-content" class="skip-link">跳转到内容</a>
  <div class="global-header">
    <router-link to="/" class="brand" aria-label="ZeroCode 首页"><img :src="logo" alt="" width="34" height="34" /><span>{{ siteConfig.title }}</span><span class="brand-tag">BETA</span></router-link>
    <nav class="navigation" aria-label="主导航">
      <a-menu mode="horizontal" :selected-keys="selectedKeys" class="nav-menu">
        <a-menu-item v-for="item in menuItems" :key="item.key"><router-link :to="item.key">{{ item.label }}</router-link></a-menu-item>
      </a-menu>
    </nav>
    <a-button class="login-link" @click="message.info('登录功能待接入')"><template #icon><UserOutlined /></template>登录</a-button>
  </div>
</template>

<style scoped>
.global-header { max-width: 1360px; margin: auto; min-height: 76px; display: flex; align-items: center; gap: 48px; }
.brand { display: flex; align-items: center; gap: 11px; color: #25283c; font-size: 23px; font-weight: 750; letter-spacing: -.7px; white-space: nowrap; }
.brand img { border-radius: 9px; }
.brand-tag { font-size: 9px; color: #737ca7; background: #f0f2fc; letter-spacing: .6px; padding: 3px 5px; border-radius: 4px; margin-left: -3px; }
.navigation { flex: 1; min-width: 0; }
.nav-menu { border-bottom: 0; line-height: 76px; background: transparent; }
.login-link { flex-shrink: 0; }
.skip-link { position: absolute; left: 16px; top: -100px; z-index: 10; padding: 10px; background: white; }
.skip-link:focus { top: 8px; }
@media (max-width: 640px) { .global-header { gap: 12px; flex-wrap: wrap; padding-top: 16px; } .brand { font-size: 21px; } .login-link { margin-left: auto; } .navigation { order: 3; flex-basis: 100%; } .nav-menu { line-height: 48px; } }
</style>
