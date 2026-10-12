<script setup>
import { Result as AResult } from 'ant-design-vue'
</script>
<template><section class="page"><a-result status="403" title="无权访问" sub-title="此页面仅管理员可访问。"><template #extra><router-link to="/"><a-button type="primary">返回首页</a-button></router-link></template></a-result></section></template>
