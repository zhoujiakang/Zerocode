/** 只为预览替换本地 CSS/JS 引用，保留原始源码；data URL 兼容 iframe 的隔离来源。 */
export function buildAppPreview(files) {
  if (!files['index.html']) throw new Error('尚无可预览的 HTML 文件')
  const doc = new DOMParser().parseFromString(files['index.html'], 'text/html')
  doc.querySelectorAll('base, meta[http-equiv]').forEach(node => node.remove())
  const policy = doc.createElement('meta')
  policy.httpEquiv = 'Content-Security-Policy'
  policy.content = "default-src 'none'; script-src 'unsafe-inline' data:; style-src 'unsafe-inline' data:; img-src https: data:; connect-src 'none'; form-action 'none'; base-uri 'none'"
  doc.head.prepend(policy)
  for (const link of doc.querySelectorAll('link[href]')) {
    if (/^(\.\/)?style\.css$/.test(link.getAttribute('href')) && files['style.css'] !== undefined) {
      link.href = `data:text/css;charset=utf-8,${encodeURIComponent(files['style.css'])}`
    } else link.remove()
  }
  for (const script of doc.querySelectorAll('script[src]')) {
    if (/^(\.\/)?script\.js$/.test(script.getAttribute('src')) && files['script.js'] !== undefined) {
      script.src = `data:text/javascript;charset=utf-8,${encodeURIComponent(files['script.js'])}`
    } else script.remove()
  }
  return '<!doctype html>\n' + doc.documentElement.outerHTML
}
