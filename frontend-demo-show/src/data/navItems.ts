export interface NavItem {
  path: string
  label: string
  emoji: string
}

export const navItems: NavItem[] = [
  { path: '/home', label: '首页', emoji: '🏠' },
  { path: '/detect', label: '感知空间', emoji: '🎯' },
  { path: '/chat', label: '安心对话', emoji: '💬' },
  { path: '/mindfulness', label: '正念练习', emoji: '🧘' },
  { path: '/diary', label: '情绪日记', emoji: '📔' },
  { path: '/profile', label: '健康画像', emoji: '📊' },
  { path: '/garden', label: '心灵花园', emoji: '🌸' },
  { path: '/me', label: '我的', emoji: '👤' },
]
