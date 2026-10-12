<script setup>
import { computed } from 'vue'
import { Card as ACard, Descriptions as ADescriptions, DescriptionsItem as ADescriptionsItem, Tag as ATag } from 'ant-design-vue'
import { useUserStore } from '../../stores/user'
import { UserOutlined } from '@ant-design/icons-vue'
const store = useUserStore()
const user = computed(() => store.loginUserInfo)
</script>
<template>
  <section v-if="user" class="page profile-page"><h1 class="page-heading">个人信息</h1><p class="page-description">你的 ZeroCode 账号信息</p><a-card class="profile-card"><div class="profile-header"><a-avatar :size="64" :src="user.userAvatar || undefined"><template #icon><UserOutlined /></template></a-avatar><div><h2>{{ user.userName || user.userAccount }}</h2><a-tag :color="store.isAdmin ? 'purple' : 'blue'">{{ store.isAdmin ? '管理员' : '普通用户' }}</a-tag></div></div><a-descriptions :column="1"><a-descriptions-item label="用户 ID">{{ user.id }}</a-descriptions-item><a-descriptions-item label="账号">{{ user.userAccount }}</a-descriptions-item><a-descriptions-item label="简介">{{ user.userProfile || '暂未填写' }}</a-descriptions-item><a-descriptions-item label="注册时间">{{ user.createTime?.replace('T', ' ') }}</a-descriptions-item></a-descriptions></a-card></section>
</template>
<style scoped>.profile-page { max-width: 760px; }.profile-card { margin-top: 30px; border-radius: 14px; border-color: #e7e9f2; }.profile-header { display: flex; align-items: center; gap: 18px; margin-bottom: 30px; }.profile-header h2 { margin: 0 0 8px; font-size: 20px; overflow-wrap: anywhere; }</style>
