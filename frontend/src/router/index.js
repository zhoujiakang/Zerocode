import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '../stores'
import { useUserStore } from '../stores/user'
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: () => import('../pages/homepage/HomePage.vue'), meta: { title: '首页' } },
    { path: '/app/create', component: () => import('../pages/app/AppCreatePage.vue'), meta: { title: '创建应用', requiresAuth: true } },
    { path: '/app/list', component: () => import('../pages/app/AppListPage.vue'), meta: { title: '我的应用', requiresAuth: true } },
    { path: '/app/:id(\\d+)', component: () => import('../pages/app/AppWorkspacePage.vue'), meta: { title: '应用工作台', requiresAuth: true } },
    { path: '/user/login', component: () => import('../pages/user/UserLoginPage.vue'), meta: { title: '用户登录', guestOnly: true } },
    { path: '/user/register', component: () => import('../pages/user/UserRegisterPage.vue'), meta: { title: '用户注册', guestOnly: true } },
    { path: '/user/profile', component: () => import('../pages/user/UserProfilePage.vue'), meta: { title: '个人信息', requiresAuth: true } },
    { path: '/admin/user', component: () => import('../pages/admin/UserManagePage.vue'), meta: { title: '用户管理', requiresAuth: true, role: 'admin' } },
    { path: '/403', component: () => import('../pages/error/ForbiddenPage.vue'), meta: { title: '访问受限' } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../pages/error/NotFoundPage.vue'), meta: { title: '页面不存在' } },
  ],
})
router.beforeEach(async to => {
  const userStore = useUserStore(pinia)
  try { await userStore.fetchLoginUser(!!to.meta.requiresAuth) }
  catch { if (to.meta.requiresAuth) return { path: '/user/login', query: { redirect: to.fullPath } }; return }
  if (to.meta.requiresAuth && !userStore.isLoggedIn) return { path: '/user/login', query: { redirect: to.fullPath } }
  if (to.meta.role === 'admin' && !userStore.isAdmin) return '/403'
  if (to.meta.guestOnly && userStore.isLoggedIn) return '/'
})
router.afterEach(to => { document.title = `${to.meta.title || 'ZeroCode'} · ZeroCode` })
export default router
