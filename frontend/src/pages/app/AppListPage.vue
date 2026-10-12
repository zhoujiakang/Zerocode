<script setup>
import { Alert as AAlert, Spin as ASpin, Tag as ATag, Empty as AEmpty, Pagination as APagination } from 'ant-design-vue'
import { onMounted, onUnmounted, ref } from 'vue'
import { listMyApps, appStatus } from '../../api/app.js'
const apps = ref([]), loading = ref(false), error = ref(''), current = ref(1), total = ref(0)
let revision = 0
async function load(page = current.value) {
  const token = ++revision; loading.value = true; error.value = ''
  try { const result = await listMyApps({ current: page, pageSize: 9 }); if (token !== revision) return; apps.value = result.records; total.value = result.total; current.value = page }
  catch (failure) { if (token === revision) error.value = failure.message }
  finally { if (token === revision) loading.value = false }
}
onMounted(() => load()); onUnmounted(() => revision++)
</script>
<template>
  <section class="page">
    <div class="list-heading"><div><h1 class="page-heading">我的应用</h1><p class="page-description">把想法变成网页，持续完善你的作品。</p></div><router-link to="/app/create"><a-button type="primary" size="large">创建应用</a-button></router-link></div>
    <a-alert v-if="error" type="error" :message="error" show-icon class="error"><template #action><a-button size="small" @click="load()">重试</a-button></template></a-alert>
    <a-spin :spinning="loading">
      <div v-if="apps.length" class="app-grid"><router-link v-for="app in apps" :key="app.id" :to="`/app/${app.id}`" class="app-card"><div class="card-top"><span class="app-icon">{{ app.codeGenType === 'HTML' ? '&lt;/&gt;' : '{ }' }}</span><a-tag :color="appStatus[app.status]?.color">{{ appStatus[app.status]?.label || app.status }}</a-tag></div><h2>{{ app.appName }}</h2><p class="prompt">{{ app.initPrompt }}</p><div class="card-bottom"><span>{{ app.codeGenType === 'HTML' ? '单 HTML' : '原生多文件' }}</span><span>打开工作台 →</span></div></router-link></div>
      <a-empty v-else-if="!loading && !error" description="还没有应用，创建你的第一个作品吧" class="empty"><router-link to="/app/create"><a-button type="primary">创建应用</a-button></router-link></a-empty>
      <div v-else class="placeholder" />
    </a-spin>
    <a-pagination v-if="total > 9" :current="current" :total="total" :page-size="9" :show-size-changer="false" class="pagination" @change="load" />
  </section>
</template>
<style scoped>
.list-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-bottom: 32px; }.app-grid { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 20px; }.app-card { padding: 24px; border: 1px solid #e6e9f2; border-radius: 12px; background: white; color: inherit; transition: border-color .2s; }.app-card:hover { border-color: #5363e9; }.card-top,.card-bottom { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.app-icon { background: #eef0ff; color: #5363e9; padding: 10px; border-radius: 8px; font-weight: 700; }.app-card h2 { font-size: 18px; margin: 22px 0 12px; overflow-wrap: anywhere; }.prompt { color: #85899d; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.8; min-height: 76px; }.card-bottom { border-top: 1px solid #f0f1f7; padding-top: 16px; font-size: 12px; color: #85899d; }.pagination { margin-top: 28px; text-align: right; }.empty { padding: 80px 0; }.placeholder { min-height: 240px; }.error { margin-bottom: 20px; }
@media(max-width: 900px) { .app-grid { grid-template-columns: repeat(2,minmax(0,1fr)); } }@media(max-width: 640px) { .app-grid { grid-template-columns: 1fr; }.list-heading { align-items: flex-start; }.page-heading { font-size: 26px; } }
</style>
