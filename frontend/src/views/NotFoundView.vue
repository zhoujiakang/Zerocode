<script setup>
import { Result as AResult } from 'ant-design-vue'
</script>
<template><section class="page"><a-result status="404" title="404" sub-title="这个页面还不存在，请返回首页。"><template #extra><router-link to="/"><a-button type="primary">返回首页</a-button></router-link></template></a-result></section></template>
