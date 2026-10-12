import { createRouter, createWebHistory } from 'vue-router'
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
    { path: '/:pathMatch(.*)*', component: () => import('../views/NotFoundView.vue'), meta: { title: '页面不存在' } },
  ],
})
router.afterEach(to => { document.title = `${to.meta.title || 'ZeroCode'} · ZeroCode` })
export default router
