<script setup>
import { Alert as AAlert, Spin as ASpin, Tag as ATag, Empty as AEmpty, Tabs as ATabs, TabPane as ATabPane, Input as AInput, RadioGroup as ARadioGroup, RadioButton as ARadioButton } from 'ant-design-vue'
const ATextarea = AInput.TextArea
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '../../stores/app.js'
import { appStatus } from '../../api/app.js'
import AppPreview from '../../components/AppPreview.vue'
const route = useRoute(), store = useAppStore(), description = ref(''), activeTab = ref('preview'), selectedFile = ref('index.html')
const status = computed(() => appStatus[store.app?.status] || { label: '', color: 'default' })
const first = computed(() => store.app?.status === 'CREATED')
const fileNames = computed(() => Object.keys(store.files))
let pollTimer, disposed = false
watch(() => route.params.id, id => { description.value = ''; activeTab.value = 'preview'; store.load(String(id)) }, { immediate: true })
watch(fileNames, names => { if (!names.includes(selectedFile.value)) selectedFile.value = names[0] || 'index.html' })
function schedulePoll() {
  clearTimeout(pollTimer)
  if (!store.busy || store.generating || disposed) return
  pollTimer = setTimeout(async () => {
    try { await store.refresh() } catch (failure) { store.error = failure.message }
    schedulePoll()
  }, 4000)
}
watch(() => [store.busy, store.generating], schedulePoll)
async function submit() {
  if (!first.value && !description.value.trim()) return
  if (await store.generate(description.value)) description.value = ''
}
async function moreHistory() { try { await store.loadHistory(true) } catch (failure) { store.error = failure.message } }
async function refresh() { try { await store.refresh() } catch (failure) { store.error = failure.message } }
onUnmounted(() => { disposed = true; clearTimeout(pollTimer); store.clear() })
</script>
<template>
  <section class="workspace">
    <div class="workspace-heading"><div><router-link to="/app/list">← 我的应用</router-link><h1>{{ store.app?.appName || '应用工作台' }} <a-tag v-if="store.app" :color="status.color">{{ status.label }}</a-tag></h1></div><a-button :loading="store.loading" :disabled="store.generating" @click="store.app ? refresh() : store.load(String(route.params.id))">刷新状态</a-button></div>
    <a-alert v-if="store.error" type="error" :message="store.error" show-icon class="alert" />
    <a-spin :spinning="store.loading">
      <div v-if="store.app" class="workspace-grid">
        <section class="conversation" aria-label="应用对话">
          <div class="panel-title">与 AI 一起完善应用 <span>{{ store.app.codeGenType === 'HTML' ? '单 HTML' : '原生多文件' }}</span></div>
          <div class="history">
            <div v-if="!store.history.length" class="initial"><span class="message-role">你的想法</span><p>{{ store.app.initPrompt }}</p><p class="hint">点击下方按钮，开始生成你的网页。</p></div>
            <article v-for="entry in store.history" :key="entry.id" :class="['message', entry.role]"><span class="message-role">{{ entry.role === 'user' ? '你' : entry.role === 'assistant' ? 'AI' : '生成提示' }}</span><p>{{ entry.content }}</p></article>
            <a-button v-if="store.history.length < store.historyTotal" size="small" :loading="store.historyLoading" @click="moreHistory">加载更多对话</a-button>
            <div v-if="store.busy" class="generating" role="status"><a-spin size="small" /> {{ store.uncertain ? '正在确认生成状态，请稍候…' : 'AI 正在编写网页，请稍候…' }}</div>
          </div>
          <div class="composer">
            <label for="app-description">{{ first ? '补充你的需求（可选）' : '你想修改什么？' }}</label>
            <a-textarea id="app-description" v-model:value="description" :rows="4" :maxlength="10000" :disabled="store.busy" :placeholder="first ? '留空将使用创建应用时的描述；填写后将使用新的完整描述。' : '例如：把标题改成中文，增加作品分类切换功能。'" />
            <a-button type="primary" block :loading="store.busy" :disabled="store.loading || (!first && !description.trim())" @click="submit">{{ first ? '开始生成' : store.app.status === 'FAILED' ? '重新生成 / 修正' : '发送修改需求' }}</a-button>
            <p class="hint">生成可能需要几分钟。离开页面后任务会继续，稍后可返回查看。</p>
          </div>
        </section>
        <section class="output" aria-label="生成结果">
          <a-tabs v-model:active-key="activeTab" class="output-tabs">
            <a-tab-pane key="preview" tab="网页预览"><a-alert v-if="store.filesError" type="error" :message="store.filesError" show-icon /><AppPreview :files="store.files" /></a-tab-pane>
            <a-tab-pane key="source" tab="源代码"><div v-if="fileNames.length" class="source"><a-radio-group v-model:value="selectedFile" size="small"><a-radio-button v-for="name in fileNames" :key="name" :value="name">{{ name }}</a-radio-button></a-radio-group><pre tabindex="0" :aria-label="`${selectedFile} 源代码`"><code>{{ store.files[selectedFile] }}</code></pre></div><a-empty v-else description="生成完成后查看源码" class="source-empty" /></a-tab-pane>
          </a-tabs>
        </section>
      </div>
      <a-empty v-else-if="!store.loading" description="无法加载应用，请返回列表或重试" class="source-empty" />
      <div v-else class="loading-space" />
    </a-spin>
  </section>
</template>
<style scoped>
.workspace { max-width: 1440px; margin: auto; }.workspace-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }.workspace-heading h1 { font-size: 25px; margin: 16px 0 0; overflow-wrap: anywhere; }.workspace-grid { display: grid; grid-template-columns: minmax(300px,360px) minmax(0,1fr); gap: 22px; }.conversation,.output { min-width: 0; border: 1px solid #e6e9f2; background: white; border-radius: 12px; overflow: hidden; }.conversation { display: flex; flex-direction: column; }.panel-title { padding: 20px; font-weight: 600; border-bottom: 1px solid #edf0f5; }.panel-title span { display: block; font-size: 12px; margin-top: 6px; color: #85899d; font-weight: 400; }.history { padding: 20px; flex: 1; min-height: 200px; max-height: 480px; overflow: auto; }.message,.initial { padding: 14px; border-radius: 8px; background: #f7f8fc; margin-bottom: 16px; }.message.user { background: #eff1ff; }.message.error { background: #fff2f0; }.message-role { font-size: 12px; color: #777d96; font-weight: 600; }.message p,.initial p { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; margin: 8px 0 0; }.composer { padding: 20px; border-top: 1px solid #edf0f5; }.composer label { display: block; margin-bottom: 12px; font-weight: 500; }.composer button { margin-top: 12px; }.hint { font-size: 12px; color: #85899d; line-height: 1.7; }.generating { display: flex; align-items: center; gap: 12px; margin-top: 20px; font-size: 13px; color: #5363e9; }.alert { margin-bottom: 20px; }.output-tabs :deep(.ant-tabs-nav) { margin: 0; padding: 0 20px; }.source { padding: 16px; }.source pre { margin: 16px 0 0; background: #202538; color: #e6e9f5; padding: 20px; border-radius: 8px; height: 620px; overflow: auto; font-size: 12px; line-height: 1.8; }.source-empty { padding: 120px 16px; }.loading-space { min-height: 400px; }
@media(max-width: 980px) { .workspace-grid { grid-template-columns: 1fr; }.history { max-height: 360px; }.source pre { height: 480px; } }
</style>
