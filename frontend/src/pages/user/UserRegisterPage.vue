<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Alert as AAlert, Form as AForm, FormItem as AFormItem, Input as AInput, InputPassword as AInputPassword } from 'ant-design-vue'
import UserAuthCard from '../../components/UserAuthCard.vue'
import { registerUser } from '../../api/user'
const route = useRoute()
const router = useRouter()
const form = reactive({ userAccount: '', userPassword: '', checkPassword: '' })
const submitting = ref(false)
const errorMessage = ref('')
const rules = {
  userAccount: [{ required: true, message: '请输入账号' }, { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '账号须为 4–32 位字母、数字或下划线' }],
  userPassword: [{ required: true, message: '请输入密码' }, { min: 8, max: 64, message: '密码长度须为 8–64 位' }, { validator: (_, value) => !value || new TextEncoder().encode(value).length <= 72 ? Promise.resolve() : Promise.reject(new Error('密码 UTF-8 长度不能超过 72 字节')) }],
  checkPassword: [{ required: true, message: '请再次输入密码' }, { validator: (_, value) => !value || value === form.userPassword ? Promise.resolve() : Promise.reject(new Error('两次输入的密码不一致')) }],
}
async function submit() {
  if (submitting.value) return
  submitting.value = true; errorMessage.value = ''
  try { await registerUser({ ...form }); form.userPassword = ''; form.checkPassword = ''; message.success('注册成功，请登录'); await router.replace({ path: '/user/login', query: route.query.redirect ? { redirect: route.query.redirect } : {} }) }
  catch (error) { errorMessage.value = error.message }
  finally { submitting.value = false }
}
</script>
<template>
  <UserAuthCard title="创建账号" subtitle="加入 ZeroCode，把你的想法变成可能">
    <a-alert v-if="errorMessage" :message="errorMessage" type="error" show-icon class="form-alert" />
    <a-form :model="form" :rules="rules" layout="vertical" @finish="submit">
      <a-form-item label="账号" name="userAccount"><a-input v-model:value="form.userAccount" placeholder="4–32 位字母、数字或下划线" autocomplete="username" :maxlength="32" size="large" /></a-form-item>
      <a-form-item label="密码" name="userPassword"><a-input-password v-model:value="form.userPassword" placeholder="8–64 位密码" autocomplete="new-password" :maxlength="64" size="large" /></a-form-item>
      <a-form-item label="确认密码" name="checkPassword"><a-input-password v-model:value="form.checkPassword" placeholder="请再次输入密码" autocomplete="new-password" :maxlength="64" size="large" /></a-form-item>
      <a-button type="primary" html-type="submit" :loading="submitting" size="large" block>注册</a-button>
    </a-form>
    <p class="auth-link">已有账号？<router-link :to="{ path: '/user/login', query: route.query.redirect ? { redirect: route.query.redirect } : {} }">去登录</router-link></p>
  </UserAuthCard>
</template>
<style scoped>.form-alert { margin-bottom: 20px; }.auth-link { margin: 22px 0 0; text-align: center; color: #9095a8; font-size: 13px; }</style>
