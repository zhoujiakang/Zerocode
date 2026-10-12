<script setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { UserOutlined, DownOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import logo from '../assets/logo.png'
import { siteConfig, menuItems } from '../config/site'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const visibleMenuItems = computed(() => menuItems.filter(item => !item.role || (item.role === 'admin' && userStore.isAdmin)))
const userName = computed(() => userStore.loginUserInfo?.userName || userStore.loginUserInfo?.userAccount)
async function logout() {
  try { await userStore.logout(); message.success('已退出登录'); await router.replace('/') }
  catch (error) { message.error(error.message) }
}
const selectedKeys = computed(() => [visibleMenuItems.value.find(item => item.key === route.path || (item.key !== '/' && route.path.startsWith(item.key + '/')))?.key].filter(Boolean))
</script>

<template>
  <a href="#main-content" class="skip-link">跳转到内容</a>
  <div class="global-header">
    <router-link to="/" class="brand" aria-label="ZeroCode 首页"><img :src="logo" alt="" width="34" height="34" /><span>{{ siteConfig.title }}</span><span class="brand-tag">BETA</span></router-link>
    <nav class="navigation" aria-label="主导航">
      <a-menu mode="horizontal" :selected-keys="selectedKeys" class="nav-menu">
        <a-menu-item v-for="item in visibleMenuItems" :key="item.key"><router-link :to="item.key">{{ item.label }}</router-link></a-menu-item>
      </a-menu>
    </nav>
    <a-dropdown v-if="userStore.isLoggedIn" :trigger="['click']">
      <button type="button" class="user-trigger" aria-label="用户菜单"><a-avatar :size="32" :src="userStore.loginUserInfo.userAvatar || undefined"><template #icon><UserOutlined /></template></a-avatar><span class="user-name">{{ userName }}</span><DownOutlined class="user-chevron" /></button>
      <template #overlay><a-menu><a-menu-item key="profile"><router-link to="/user/profile"><UserOutlined /> 个人信息</router-link></a-menu-item><a-menu-item v-if="userStore.isAdmin" key="admin"><router-link to="/admin/user">用户管理</router-link></a-menu-item><a-menu-divider /><a-menu-item key="logout" @click="logout"><LogoutOutlined /> 退出登录</a-menu-item></a-menu></template>
    </a-dropdown>
    <router-link v-else to="/user/login" class="login-link"><a-button><template #icon><UserOutlined /></template>登录</a-button></router-link>
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
.user-trigger { display: flex; align-items: center; gap: 9px; border: 0; background: transparent; cursor: pointer; padding: 6px; border-radius: 8px; font: inherit; color: #505773; flex-shrink: 0; }
.user-trigger:hover { background: #f7f8fc; }.user-name { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }.user-chevron { color: #999fb5; font-size: 10px; }
.skip-link { position: absolute; left: 16px; top: -100px; z-index: 10; padding: 10px; background: white; }
.skip-link:focus { top: 8px; }
@media (max-width: 640px) { .global-header { gap: 12px; flex-wrap: wrap; padding-top: 16px; } .brand { font-size: 21px; } .login-link, .user-trigger { margin-left: auto; } .navigation { order: 3; flex-basis: 100%; } .nav-menu { line-height: 48px; } }
</style>
