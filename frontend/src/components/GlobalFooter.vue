<script setup>
import { siteConfig } from '../config/site'
const year = new Date().getFullYear()
</script>

<template>
  <div class="global-footer"><span>© {{ year }} {{ siteConfig.title }} · 由 {{ siteConfig.author }} 构建</span><a v-if="siteConfig.githubUrl" :href="siteConfig.githubUrl" target="_blank" rel="noopener noreferrer">GitHub ↗</a><span v-else class="footer-note">让想法，更快成为应用</span></div>
</template>

<style scoped>
.global-footer { max-width: 1160px; margin: auto; display: flex; justify-content: space-between; align-items: center; gap: 12px; color: #9296a8; font-size: 12px; }
.global-footer a { color: #6e759b; }
@media (max-width: 480px) { .global-footer { flex-direction: column; text-align: center; } }
</style>
