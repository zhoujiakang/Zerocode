<script setup>
import { Alert as AAlert, Empty as AEmpty, RadioGroup as ARadioGroup, RadioButton as ARadioButton } from 'ant-design-vue'
import { ref, watch } from 'vue'
import { buildAppPreview } from '../utils/preview.js'
const props = defineProps({ files: { type: Object, required: true } })
const document = ref(''), error = ref(''), viewport = ref('desktop'), version = ref(0)
function rebuild() {
  document.value = ''; error.value = ''
  if (!props.files['index.html']) return
  try { document.value = buildAppPreview(props.files); version.value++ }
  catch (failure) { error.value = failure.message }
}
watch(() => props.files, rebuild, { immediate: true })
</script>
<template>
  <div class="preview-tools"><a-radio-group v-model:value="viewport" button-style="solid" size="small"><a-radio-button value="desktop">桌面</a-radio-button><a-radio-button value="mobile">手机</a-radio-button></a-radio-group><a-button size="small" :disabled="!document" @click="rebuild">刷新预览</a-button></div>
  <a-alert v-if="error" type="error" :message="error" />
  <div v-else-if="document" class="preview-stage"><iframe :key="version" :srcdoc="document" sandbox="allow-scripts" referrerpolicy="no-referrer" title="生成应用预览" :class="{ mobile: viewport === 'mobile' }" /></div>
  <a-empty v-else description="生成完成后，可在这里预览网页" class="empty-preview" />
</template>
<style scoped>
.preview-tools { display: flex; justify-content: space-between; gap: 12px; padding: 12px 16px; border-bottom: 1px solid #edf0f5; }
.preview-stage { background: #eef0f5; padding: 16px; overflow: auto; }
iframe { display: block; width: 100%; height: 620px; border: 0; background: white; margin: auto; border-radius: 4px; }
iframe.mobile { width: 375px; max-width: 100%; }.empty-preview { padding: 100px 16px; }
@media(max-width: 640px) { .preview-stage { padding: 8px; } iframe { height: 520px; } }
</style>
