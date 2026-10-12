<script setup>
import logo from '../assets/logo.png'
defineProps({ title: String, subtitle: String })
</script>
<template>
  <section class="auth-card"><img :src="logo" width="44" height="44" alt="ZeroCode" /><h1>{{ title }}</h1><p class="auth-subtitle">{{ subtitle }}</p><slot /></section>
</template>
<style scoped>
.auth-card { max-width: 420px; margin: 20px auto; padding: 36px; background: #fff; border: 1px solid #e7e9f2; border-radius: 16px; box-shadow: 0 8px 30px #30367808; }
.auth-card > img { display: block; margin: 0 auto 20px; border-radius: 12px; }.auth-card h1 { margin: 0 0 10px; font-size: 26px; font-weight: 650; text-align: center; }.auth-subtitle { margin: 0 0 28px; font-size: 13px; color: #9498ac; text-align: center; }
@media(max-width: 480px) { .auth-card { padding: 28px 22px; margin-top: 0; } }
</style>
