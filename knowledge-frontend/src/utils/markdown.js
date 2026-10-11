import { marked } from 'marked'
import DOMPurify from 'dompurify'

// marked 只负责 Markdown→HTML；v-html 渲染前必须经 DOMPurify 消毒，
// 防止存储型 XSS（marked 5.x 起已移除内置 sanitize 选项）。
// 文章正文与 AI 回答共用此渲染入口。
export function renderMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md))
}
