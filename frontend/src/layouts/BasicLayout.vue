<script setup>
import GlobalHeader from '../components/GlobalHeader.vue'
import GlobalFooter from '../components/GlobalFooter.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="layout-header"><GlobalHeader /></a-layout-header>
    <a-layout-content class="layout-content"><main id="main-content"><router-view /></main></a-layout-content>
    <a-layout-footer class="layout-footer"><GlobalFooter /></a-layout-footer>
  </a-layout>
</template>

<style scoped>
.basic-layout { min-height: 100vh; min-height: 100dvh; background: #f7f8fc; }
.layout-header { height: auto; min-height: 76px; padding: 0 40px; background: #fff; border-bottom: 1px solid #eaeaf2; line-height: normal; flex-shrink: 0; }
.layout-content { flex: 1; padding: 48px 32px 56px; }
.layout-footer { flex-shrink: 0; background: #fff; padding: 24px 32px; border-top: 1px solid #eaeaf2; }
@media (max-width: 640px) { .layout-header { padding: 0 16px; } .layout-content { padding: 32px 16px 40px; } .layout-footer { padding: 20px 16px; } }
</style>
