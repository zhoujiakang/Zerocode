<template>
  <section class="page home-page">
    <span class="eyebrow">从想法到网页</span><h1>用一句话，<br />开始你的下一个应用。</h1>
    <p class="page-description">描述你的想法，让 AI 为你编写原生网页。<br />实时查看成果，通过对话持续完善。</p>
    <div class="actions"><router-link to="/app/create"><a-button type="primary" size="large">创建应用 →</a-button></router-link><router-link to="/app/list"><a-button size="large">我的应用</a-button></router-link></div>
    <div class="features"><article><span>01</span><h2>描述你的想法</h2><p>告诉 AI 网页用途、内容和交互。</p></article><article><span>02</span><h2>生成原生网页</h2><p>选择单 HTML 或 HTML、CSS、JS 多文件。</p></article><article><span>03</span><h2>继续打磨作品</h2><p>预览网页、查看源码，通过对话进行修改。</p></article></div>
  </section>
</template>
<style scoped>
.home-page { padding-top: 36px; text-align: center; }.eyebrow { display: inline-block; color: #5363e9; background: #ecefff; padding: 8px 16px; border-radius: 20px; font-size: 13px; }.home-page h1 { font-size: clamp(34px,5vw,56px); line-height: 1.3; letter-spacing: -1.5px; margin: 24px 0; }.actions { display: flex; justify-content: center; gap: 14px; margin-top: 28px; }.features { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 20px; margin-top: 64px; text-align: left; }.features article { padding: 28px; background: white; border: 1px solid #e7e9f2; border-radius: 12px; }.features span { color: #5363e9; font-size: 12px; font-weight: 600; }.features h2 { font-size: 17px; margin: 16px 0 10px; }.features p { font-size: 13px; line-height: 1.8; color: #85899d; margin: 0; }@media(max-width: 640px) { .home-page { padding-top: 8px; }.features { grid-template-columns: 1fr; margin-top: 40px; gap: 12px; } }
</style>
