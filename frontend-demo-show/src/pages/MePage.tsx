import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { mockUser } from '../data/mockUser'
import './MePage.css'

// ── Types ──────────────────────────────────────────────
interface DataCard {
  emoji: string
  value: string
  label: string
  color: 'green' | 'blue' | 'warm' | 'purple'
}

interface BadgeItem {
  emoji: string
  name: string
  earned: boolean
  color: string
}

interface SettingItem {
  icon: string
  label: string
}

// ── Mock Data ──────────────────────────────────────────
const dataCards: DataCard[] = [
  { emoji: '💎', value: '120', label: '星光币', color: 'purple' },
  { emoji: '🏆', value: '6 枚', label: '成就徽章', color: 'warm' },
  { emoji: '🔥', value: '7 天', label: '连续觉察', color: 'green' },
  { emoji: '📖', value: '15 天', label: '日记记录', color: 'blue' },
]

const badges: BadgeItem[] = [
  { emoji: '🌸', name: '觉察新手', earned: true, color: 'purple' },
  { emoji: '🧘', name: '正念坚持者', earned: true, color: 'green' },
  { emoji: '📖', name: '情绪记录者', earned: true, color: 'blue' },
  { emoji: '💬', name: '安心表达者', earned: true, color: 'warm' },
  { emoji: '🌿', name: '平静守护者', earned: true, color: 'green' },
  { emoji: '💎', name: '星光收集者', earned: true, color: 'purple' },
]

const settingItems: SettingItem[] = [
  { icon: '🔒', label: '隐私设置' },
  { icon: '📊', label: '数据导出' },
  { icon: '🔔', label: '提醒设置' },
  { icon: '📱', label: '设备管理' },
  { icon: '❓', label: '帮助中心' },
  { icon: 'ℹ️', label: '关于我们' },
]

const USER_BIO = '你已经在慢慢学会照顾自己了，这很重要。'
const COMPANION_MESSAGE =
  '谢谢你这几天愿意一次次回来。成长不是一直向前，有时停下来看看自己，也是在前进。'
const MOCK_TOAST = '该功能将在正式版本中开放。'

// ── Color Helpers ──────────────────────────────────────
const colorVar = (c: string) => {
  const map: Record<string, string> = {
    green: 'var(--primary)',
    blue: 'var(--blue)',
    warm: 'var(--warm)',
    purple: 'var(--purple)',
  }
  return map[c] || c
}

const colorBg = (c: string) => {
  const map: Record<string, string> = {
    green: 'var(--primary-bg)',
    blue: 'var(--blue-bg)',
    warm: 'var(--warm-bg)',
    purple: 'var(--purple-bg)',
  }
  return map[c] || 'var(--primary-bg)'
}

// ── Component ──────────────────────────────────────────
function MePage() {
  const navigate = useNavigate()

  const [toastVisible, setToastVisible] = useState(false)
  const toastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (toastTimerRef.current) clearTimeout(toastTimerRef.current)
    }
  }, [])

  const handleSettingClick = () => {
    setToastVisible(true)
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current)
    toastTimerRef.current = setTimeout(() => setToastVisible(false), 2500)
  }

  return (
    <div className="me-page">
      {/* ===== Page Header ===== */}
      <header className="me-header">
        <h1 className="me-title">👤 我的</h1>
        <p className="me-subtitle">这里记录着你一点点变好的痕迹</p>
      </header>

      {/* ===== 1. User Info Card ===== */}
      <section className="me-user-card card-glass">
        <div className="me-user-top">
          <div className="me-user-avatar">
            <span className="me-user-avatar-emoji">{mockUser.moodEmoji}</span>
          </div>
          <div className="me-user-info">
            <h2 className="me-user-name">{mockUser.name}</h2>
            <div className="me-user-companion">
              <span className="me-user-companion-emoji">🌸</span>
              <span className="me-user-companion-text">
                {mockUser.companion} ·{' '}
                <span className="me-user-companion-level">Lv.5 忠实伙伴</span>
              </span>
            </div>
            <div className="me-user-streak">
              <span className="me-user-streak-icon">🔥</span>
              <span className="me-user-streak-text">
                连续觉察 {mockUser.streakDays} 天
              </span>
            </div>
          </div>
        </div>
        <div className="me-user-bio">
          <span className="me-user-bio-icon">💚</span>
          <p>{USER_BIO}</p>
        </div>
      </section>

      {/* ===== 2. Data Overview ===== */}
      <section className="me-data-row">
        {dataCards.map((card) => (
          <div className="me-data-card card-glass" key={card.label}>
            <div
              className="me-data-icon-ring"
              style={{ background: colorBg(card.color) }}
            >
              <span className="me-data-emoji">{card.emoji}</span>
            </div>
            <div className="me-data-body">
              <div
                className="me-data-value"
                style={{ color: colorVar(card.color) }}
              >
                {card.value}
              </div>
              <div className="me-data-label">{card.label}</div>
            </div>
          </div>
        ))}
      </section>

      {/* ===== 3. Achievements ===== */}
      <section className="me-badges-section card-glass">
        <h3 className="me-section-title">🏆 我的成就</h3>
        <div className="me-badges-grid">
          {badges.map((badge) => (
            <div className="me-badge-item" key={badge.name}>
              <div
                className="me-badge-ring"
                style={{
                  borderColor: colorVar(badge.color),
                  background: colorBg(badge.color),
                }}
              >
                <span className="me-badge-emoji">{badge.emoji}</span>
              </div>
              <span className="me-badge-name">{badge.name}</span>
            </div>
          ))}
        </div>
      </section>

      {/* ===== 4. Settings ===== */}
      <section className="me-settings-card card-glass">
        <h3 className="me-section-title">⚙️ 数据与隐私设置</h3>
        <div className="me-settings-list">
          {settingItems.map((item) => (
            <button
              className="me-setting-item"
              key={item.label}
              onClick={handleSettingClick}
            >
              <span className="me-setting-icon">{item.icon}</span>
              <span className="me-setting-label">{item.label}</span>
              <span className="me-setting-arrow">→</span>
            </button>
          ))}
        </div>
      </section>

      {/* ===== 5. AnXiaoNing Companion ===== */}
      <section className="me-companion-card card-glass">
        <div className="me-companion-header">
          <div className="me-companion-avatar">
            <span className="me-companion-emoji">🌸</span>
          </div>
          <div className="me-companion-header-text">
            <span className="me-companion-name">安小宁想对你说</span>
          </div>
        </div>
        <div className="me-companion-bubble">
          <p>{COMPANION_MESSAGE}</p>
        </div>
      </section>

      {/* ===== 6. Quick Nav ===== */}
      <div className="me-quick-nav">
        <button
          className="me-nav-btn card-glass"
          onClick={() => navigate('/home')}
        >
          <span className="me-nav-emoji">🏠</span>
          <span className="me-nav-label">回到首页</span>
          <span className="me-nav-arrow">→</span>
        </button>
        <button
          className="me-nav-btn card-glass"
          onClick={() => navigate('/garden')}
        >
          <span className="me-nav-emoji">🌸</span>
          <span className="me-nav-label">查看心灵花园</span>
          <span className="me-nav-arrow">→</span>
        </button>
        <button
          className="me-nav-btn card-glass"
          onClick={() => navigate('/profile')}
        >
          <span className="me-nav-emoji">📊</span>
          <span className="me-nav-label">查看健康画像</span>
          <span className="me-nav-arrow">→</span>
        </button>
      </div>

      {/* ── Mock Toast ── */}
      <div className={`me-toast ${toastVisible ? 'me-toast--visible' : ''}`}>
        <span>💡 {MOCK_TOAST}</span>
      </div>
    </div>
  )
}

export default MePage
