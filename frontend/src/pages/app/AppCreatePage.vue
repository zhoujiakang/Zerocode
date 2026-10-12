<script setup>
import { Card as ACard, Alert as AAlert, Form as AForm, FormItem as AFormItem, Input as AInput, RadioGroup as ARadioGroup, RadioButton as ARadioButton } from 'ant-design-vue'
const ATextarea = AInput.TextArea
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createApp } from '../../api/app.js'
const router = useRouter(), submitting = ref(false), error = ref('')
const form = reactive({ appName: '', initPrompt: '', codeGenType: 'HTML' })
const rules = { appName: [{ required: true, whitespace: true, message: '请输入应用名称' }], initPrompt: [{ required: true, whitespace: true, message: '请描述你想创建的网页' }] }
async function submit() {
  if (submitting.value) return
  submitting.value = true; error.value = ''
  try { const id = await createApp({ ...form }); await router.push(`/app/${id}`) }
  catch (failure) { error.value = failure.message }
  finally { submitting.value = false }
}
</script>
<template>
  <section class="page create-page">
    <router-link to="/app/list">← 我的应用</router-link>
    <h1 class="page-heading">从一个想法开始</h1><p class="page-description">描述你想要的网页，让 AI 帮你实现。</p>
    <a-card class="create-card">
      <a-alert v-if="error" type="error" :message="error" show-icon class="error" />
      <a-form :model="form" :rules="rules" layout="vertical" @finish="submit">
        <a-form-item label="应用名称" name="appName"><a-input v-model:value="form.appName" :maxlength="128" placeholder="例如：我的个人作品集" :disabled="submitting" /></a-form-item>
        <a-form-item label="生成模式" name="codeGenType"><a-radio-group v-model:value="form.codeGenType" :disabled="submitting"><a-radio-button value="HTML">单 HTML</a-radio-button><a-radio-button value="MULTI_FILE">原生多文件</a-radio-button></a-radio-group><p class="mode-note">{{ form.codeGenType === 'HTML' ? 'HTML、CSS、JavaScript 整合为一个文件，适合简单网页和快速原型。' : '分别生成 index.html、style.css、script.js，方便维护结构、样式与交互。' }}</p></a-form-item>
        <a-form-item label="你想创建什么？" name="initPrompt"><a-textarea v-model:value="form.initPrompt" :rows="7" :maxlength="10000" show-count :disabled="submitting" placeholder="描述网页用途、内容、视觉风格和交互。例如：创建一个摄影作品集，使用简洁的黑白风格，包含作品分类、图片画廊和联系表单。" /></a-form-item>
        <a-button type="primary" html-type="submit" :loading="submitting" size="large">创建应用</a-button>
      </a-form>
    </a-card>
  </section>
</template>
<style scoped>
.create-page { max-width: 760px; }.page-heading { margin-top: 24px; }.create-card { margin-top: 28px; }.mode-note { color: #85899d; line-height: 1.8; margin: 12px 0 0; }.error { margin-bottom: 20px; }
</style>
