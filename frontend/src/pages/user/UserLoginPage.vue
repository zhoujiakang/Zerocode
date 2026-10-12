<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Alert as AAlert, Form as AForm, FormItem as AFormItem, Input as AInput, InputPassword as AInputPassword } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import UserAuthCard from '../../components/UserAuthCard.vue'
import { useUserStore } from '../../stores/user'
import { safeRedirect } from '../../utils/navigation'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const form = reactive({ userAccount: '', userPassword: '' })
const submitting = ref(false)
const errorMessage = ref('')
const rules = {
  userAccount: [{ required: true, message: '请输入账号' }, { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '账号须为 4–32 位字母、数字或下划线' }],
  userPassword: [{ required: true, message: '请输入密码' }, { min: 8, max: 64, message: '密码长度须为 8–64 位' }],
}
async function submit() {
  if (submitting.value) return
  submitting.value = true; errorMessage.value = ''
  try { await userStore.login({ ...form }); form.userPassword = ''; message.success('登录成功'); await router.replace(safeRedirect(route.query.redirect, router)) }
  catch (error) { errorMessage.value = error.message }
  finally { submitting.value = false }
}
</script>
<template>
  <UserAuthCard title="欢迎回来" subtitle="登录 ZeroCode，开始你的应用创作">
    <a-alert v-if="errorMessage || userStore.loadingError" :message="errorMessage || userStore.loadingError" type="error" show-icon class="form-alert" />
    <a-form :model="form" :rules="rules" layout="vertical" @finish="submit">
      <a-form-item label="账号" name="userAccount"><a-input v-model:value="form.userAccount" placeholder="请输入账号" autocomplete="username" :maxlength="32" size="large"><template #prefix><UserOutlined /></template></a-input></a-form-item>
      <a-form-item label="密码" name="userPassword"><a-input-password v-model:value="form.userPassword" placeholder="请输入密码" autocomplete="current-password" :maxlength="64" size="large"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
      <a-button type="primary" html-type="submit" :loading="submitting" size="large" block>登录</a-button>
    </a-form>
    <p class="auth-link">还没有账号？<router-link :to="{ path: '/user/register', query: route.query.redirect ? { redirect: route.query.redirect } : {} }">立即注册</router-link></p>
  </UserAuthCard>
</template>
<style scoped>.form-alert { margin-bottom: 20px; }.auth-link { margin: 22px 0 0; text-align: center; color: #9095a8; font-size: 13px; }</style>
