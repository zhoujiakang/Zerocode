// 仅允许当前应用中已有路由的站内跳转，避免登录 redirect 导向外部地址。
export function safeRedirect(value, router) {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')) return '/'
  const route = router.resolve(value)
  if (!route.matched.length || route.name === 'not-found' || ['/user/login', '/user/register'].includes(route.path)) return '/'
  return route.fullPath
}
