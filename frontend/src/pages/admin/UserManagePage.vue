<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Alert as AAlert, Card as ACard, Form as AForm, FormItem as AFormItem, Input as AInput, Popconfirm as APopconfirm, Select as ASelect, Space as ASpace, Table as ATable, Tag as ATag } from 'ant-design-vue'
import { UserOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { searchUsers, deleteUser } from '../../api/user'
import { useUserStore } from '../../stores/user'
const store = useUserStore()
const filters = reactive({ userAccount: '', userName: '', userRole: undefined })
const records = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const errorMessage = ref('')
const deletingId = ref(null)
let requestRevision = 0
const pagination = computed(() => ({ current: current.value, pageSize: pageSize.value, total: total.value, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'], showTotal: count => `共 ${count} 位用户` }))
const columns = [
  { title: '用户', key: 'user', width: 250 }, { title: '账号', dataIndex: 'userAccount', width: 180 },
  { title: '角色', key: 'role', width: 100 }, { title: '注册时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 100 },
]
async function loadUsers() {
  const revision = ++requestRevision
  loading.value = true; errorMessage.value = ''
  try {
    const data = await searchUsers({ current: current.value, pageSize: pageSize.value, userAccount: filters.userAccount.trim() || undefined, userName: filters.userName.trim() || undefined, userRole: filters.userRole })
    if (revision !== requestRevision) return
    records.value = data.records; total.value = data.total
    if (!data.records.length && current.value > 1) { current.value = Math.max(1, Math.ceil(data.total / pageSize.value)); await loadUsers() }
  } catch (error) { if (revision === requestRevision) { records.value = []; total.value = 0; errorMessage.value = error.message } }
  finally { if (revision === requestRevision) loading.value = false }
}
function search() { current.value = 1; loadUsers() }
function reset() { Object.assign(filters, { userAccount: '', userName: '', userRole: undefined }); search() }
function changePage(page) { current.value = page.current; pageSize.value = page.pageSize; loadUsers() }
async function remove(record) {
  if (deletingId.value) return
  deletingId.value = record.id
  try { await deleteUser(record.id); message.success('用户已删除，其登录状态已失效'); await loadUsers() }
  catch (error) { message.error(error.message) }
  finally { deletingId.value = null }
}
onMounted(loadUsers)
</script>
<template>
  <section class="page"><div class="page-title"><div><h1 class="page-heading">用户管理</h1><p class="page-description">搜索和管理平台用户，仅管理员可用。</p></div><a-tag color="purple">管理员</a-tag></div>
    <a-card class="search-card"><a-form layout="inline" :model="filters" @finish="search" class="search-form"><a-form-item label="账号"><a-input v-model:value="filters.userAccount" placeholder="搜索账号" allow-clear :maxlength="256" /></a-form-item><a-form-item label="昵称"><a-input v-model:value="filters.userName" placeholder="搜索昵称" allow-clear :maxlength="256" /></a-form-item><a-form-item label="角色"><a-select v-model:value="filters.userRole" placeholder="全部角色" allow-clear :options="[{ value: 'user', label: '普通用户' }, { value: 'admin', label: '管理员' }]" class="role-select" /></a-form-item><a-form-item><a-space><a-button type="primary" html-type="submit" :loading="loading"><SearchOutlined />搜索</a-button><a-button @click="reset"><ReloadOutlined />重置</a-button></a-space></a-form-item></a-form></a-card>
    <a-alert v-if="errorMessage" :message="errorMessage" type="error" show-icon class="error-alert"><template #action><a-button size="small" @click="loadUsers">重试</a-button></template></a-alert>
    <a-table :columns="columns" :data-source="records" row-key="id" :loading="loading" :pagination="pagination" :scroll="{ x: 810 }" class="user-table" @change="changePage"><template #bodyCell="{ column, record }"><template v-if="column.key === 'user'"><div class="table-user"><a-avatar :src="record.userAvatar || undefined"><template #icon><UserOutlined /></template></a-avatar><div><span>{{ record.userName || record.userAccount }}</span><small>ID: {{ record.id }}</small></div></div></template><template v-else-if="column.key === 'role'"><a-tag :color="record.userRole === 'admin' ? 'purple' : 'blue'">{{ record.userRole === 'admin' ? '管理员' : '普通用户' }}</a-tag></template><template v-else-if="column.key === 'createdAt'">{{ record.createTime?.replace('T', ' ') }}</template><template v-else-if="column.key === 'action'"><a-popconfirm :title="`确定删除用户 ${record.userAccount}？`" description="用户将无法登录，已有会话会失效。" ok-text="删除" cancel-text="取消" :disabled="record.id === store.loginUserInfo?.id || !!deletingId" @confirm="remove(record)"><a-button type="link" danger :disabled="record.id === store.loginUserInfo?.id || (!!deletingId && deletingId !== record.id)" :loading="deletingId === record.id">删除</a-button></a-popconfirm></template></template></a-table>
  </section>
</template>
<style scoped>.page-title { display: flex; justify-content: space-between; align-items: center; gap: 20px; }.search-card { margin: 28px 0 22px; border-radius: 12px; border-color: #e7e9f2; }.search-form { gap: 16px 0; }.role-select { width: 130px; }.error-alert { margin-bottom: 20px; }.table-user { display: flex; align-items: center; gap: 12px; }.table-user > div { min-width: 0; }.table-user span { overflow-wrap: anywhere; }.table-user small { display: block; margin-top: 4px; color: #969bae; font-size: 11px; }.user-table { background: #fff; border-radius: 12px; overflow: hidden; }@media(max-width: 640px) { .search-form { display: flex; flex-direction: column; }.search-form :deep(.ant-form-item) { margin: 0; }.role-select { width: 100%; } }</style>
