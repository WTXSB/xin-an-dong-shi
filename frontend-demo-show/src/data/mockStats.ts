export interface StatCard {
  label: string
  value: string | number
  sub: string
  emoji: string
  color: 'green' | 'blue' | 'warm' | 'purple'
}

export const mockHomeStats: StatCard[] = [
  { label: '情绪之花', value: '5/7', sub: '瓣盛开，继续浇灌', emoji: '🌸', color: 'purple' },
  { label: '星光币', value: 120, sub: '可兑换疗愈道具', emoji: '💎', color: 'blue' },
  { label: '连续觉察', value: '7', sub: '天，保持正念习惯', emoji: '🔥', color: 'warm' },
  { label: '正念时长', value: 128, sub: '分钟，比上周 +12%', emoji: '🧘', color: 'green' },
]

export interface QuickAction {
  label: string
  sub: string
  emoji: string
  path: string
  color: string
}

export const quickActions: QuickAction[] = [
  { label: '开始温柔感知', sub: '觉察此刻情绪', emoji: '🔍', path: '/detect', color: '#7babce' },
  { label: '和安小宁聊聊', sub: 'AI 安心对话', emoji: '💬', path: '/chat', color: '#5bae8a' },
  { label: '1 分钟呼吸练习', sub: '快速回归平静', emoji: '🧘', path: '/mindfulness', color: '#f0a261' },
  { label: '记录今天的心情', sub: '书写情绪日记', emoji: '📖', path: '/diary', color: '#c084fc' },
]

export interface HealingRec {
  id: number
  title: string
  desc: string
  duration: string
  emoji: string
  tag: string
}

export const healingRecs: HealingRec[] = [
  { id: 1, title: '晨间正念', desc: '用 3 分钟唤醒身体，感受清晨的第一缕阳光', duration: '3 分钟', emoji: '🌅', tag: '冥想' },
  { id: 2, title: '焦虑急救呼吸', desc: '下午焦虑来袭？试试 4-7-8 呼吸法', duration: '2 分钟', emoji: '🌬️', tag: '呼吸' },
  { id: 3, title: '睡前身体扫描', desc: '从头到脚逐一放松，给今天画上温柔的句号', duration: '5 分钟', emoji: '🌙', tag: '放松' },
]

export interface WeeklyTrend {
  days: { label: string; value: number }[]
  changePercent: number
  summary: string
}

export const weeklyTrend: WeeklyTrend = {
  days: [
    { label: '一', value: 7 },
    { label: '二', value: 6 },
    { label: '三', value: 5 },
    { label: '四', value: 4 },
    { label: '五', value: 3 },
    { label: '六', value: 2 },
    { label: '日', value: 1 },
  ],
  changePercent: 23,
  summary: '本周焦虑行为比上周减少 23%',
}

export interface ActivityItem {
  id: number
  title: string
  time: string
  emoji: string
  color: string
}

export const recentActivities: ActivityItem[] = [
  { id: 1, title: '完成 1 次正念练习', time: '今天 08:30', emoji: '🧘', color: '#5bae8a' },
  { id: 2, title: '记录了情绪日记', time: '昨天 22:15', emoji: '📝', color: '#c084fc' },
  { id: 3, title: '与安小宁对话 30 分钟', time: '昨天 20:00', emoji: '💬', color: '#7babce' },
  { id: 4, title: '完成一次温柔感知', time: '昨天 09:00', emoji: '🔍', color: '#f0a261' },
]
