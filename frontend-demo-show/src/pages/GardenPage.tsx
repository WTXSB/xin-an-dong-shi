import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import './GardenPage.css'

// ── Types ──────────────────────────────────────────────
interface GardenElement {
  emoji: string
  name: string
  status: string
  color: 'green' | 'blue' | 'purple' | 'warm'
}

interface GardenStat {
  emoji: string
  label: string
  detail: string
  color: 'green' | 'blue' | 'purple' | 'warm'
}

interface BadgeItem {
  emoji: string
  name: string
  earned: boolean
  color: string
}

interface ProgressItem {
  label: string
  percent: number
  color: string
}

// ── Mock Data ──────────────────────────────────────────
const gardenElements: GardenElement[] = [
  {
    emoji: '🌳',
    name: '觉察之树',
    status: 'Lv.3 · 已开花',
    color: 'green',
  },
  {
    emoji: '🌊',
    name: '情绪池塘',
    status: '平静蓝',
    color: 'blue',
  },
  {
    emoji: '🌈',
    name: '正念之桥',
    status: 'Lv.2',
    color: 'purple',
  },
  {
    emoji: '🏡',
    name: '疗愈之亭',
    status: '建设中',
    color: 'warm',
  },
  {
    emoji: '🛤️',
    name: '花园小径',
    status: '连续记录 15 天',
    color: 'green',
  },
]

const gardenStats: GardenStat[] = [
  {
    emoji: '🌳',
    label: '觉察之树',
    detail: '连续觉察 7 天',
    color: 'green',
  },
  {
    emoji: '🌊',
    label: '情绪池塘',
    detail: '本周焦虑行为减少 23%',
    color: 'blue',
  },
  {
    emoji: '🌈',
    label: '正念之桥',
    detail: '累计正念 12 次',
    color: 'purple',
  },
  {
    emoji: '🏡',
    label: '疗愈之亭',
    detail: '完成 4 次安心对话',
    color: 'warm',
  },
]

const progressItems: ProgressItem[] = [
  { label: '觉察之树成长', percent: 70, color: 'green' },
  { label: '正念之桥延展', percent: 45, color: 'purple' },
  { label: '疗愈之亭建设', percent: 60, color: 'warm' },
]

const badges: BadgeItem[] = [
  { emoji: '🌸', name: '觉察新手', earned: true, color: 'purple' },
  { emoji: '🧘', name: '正念坚持者', earned: true, color: 'green' },
  { emoji: '📖', name: '日记记录者', earned: true, color: 'blue' },
  { emoji: '💬', name: '安心表达者', earned: true, color: 'warm' },
  { emoji: '💎', name: '星光收集者', earned: true, color: 'purple' },
  { emoji: '🌿', name: '平静守护者', earned: false, color: 'green' },
]

const COMPANION_TIP =
  '花园不会一天长成。你今天愿意看见自己，它就已经多长出了一片叶子。'

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
function GardenPage() {
  const navigate = useNavigate()

  const [claimed, setClaimed] = useState(false)
  const [showClaimMsg, setShowClaimMsg] = useState(false)
  const claimTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (claimTimerRef.current) clearTimeout(claimTimerRef.current)
    }
  }, [])

  const handleClaim = () => {
    if (claimed) return
    setClaimed(true)
    setShowClaimMsg(true)
    if (claimTimerRef.current) clearTimeout(claimTimerRef.current)
    claimTimerRef.current = setTimeout(() => setShowClaimMsg(false), 3500)
  }

  return (
    <div className="garden-page">
      {/* ===== Page Header ===== */}
      <header className="garden-header">
        <h1 className="garden-title">🌸 心灵花园</h1>
        <p className="garden-subtitle">
          你的每一次觉察，都会让这里多一点光
        </p>
      </header>

      {/* ===== 1. Garden Main Visual ===== */}
      <section className="garden-visual-card card-glass">
        <h2 className="garden-visual-title">我的心灵花园</h2>
        <div className="garden-elements-grid">
          {gardenElements.map((el) => (
            <div className="garden-element" key={el.name}>
              <div
                className="garden-element-icon-ring"
                style={{ background: colorBg(el.color) }}
              >
                <span className="garden-element-emoji">{el.emoji}</span>
              </div>
              <div className="garden-element-info">
                <span className="garden-element-name">{el.name}</span>
                <span
                  className="garden-element-status"
                  style={{ color: colorVar(el.color) }}
                >
                  {el.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ===== 2. Starlight + Progress (two columns) ===== */}
      <div className="garden-two-col">
        {/* Starlight Card */}
        <section className="starlight-card card-glass">
          <div className="starlight-header">
            <span className="starlight-icon">💎</span>
            <div>
              <h3 className="starlight-title">星光币</h3>
              <p className="starlight-subtitle">可用于解锁花园装饰</p>
            </div>
          </div>
          <div className="starlight-balance">
            <span className="starlight-amount">120</span>
            <span className="starlight-unit">星光币</span>
          </div>
          <div className="starlight-today">
            <span className="starlight-today-label">今日获得</span>
            <span className="starlight-today-value">+7</span>
          </div>
          <button
            className={`btn btn-primary starlight-claim-btn ${claimed ? 'starlight-claim-btn--claimed' : ''}`}
            onClick={handleClaim}
            disabled={claimed}
          >
            {claimed ? '✨ 已领取' : '🌟 领取今日星光'}
          </button>
          <div
            className={`starlight-claim-msg ${showClaimMsg ? 'starlight-claim-msg--visible' : ''}`}
          >
            <span>💎 今日星光已收下。每一次觉察都值得被看见。</span>
          </div>
        </section>

        {/* Garden Growth Progress */}
        <section className="progress-card card-glass">
          <h3 className="progress-title">🌱 花园成长进度</h3>
          <div className="progress-list">
            {progressItems.map((item) => (
              <div className="progress-item" key={item.label}>
                <div className="progress-header">
                  <span className="progress-label">{item.label}</span>
                  <span
                    className="progress-percent"
                    style={{ color: colorVar(item.color) }}
                  >
                    {item.percent}%
                  </span>
                </div>
                <div className="progress-track">
                  <div
                    className="progress-fill"
                    style={{
                      width: `${item.percent}%`,
                      background: `linear-gradient(90deg, ${colorVar(item.color)}, ${colorVar(item.color)}dd)`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* ===== 3. Garden Status Cards ===== */}
      <section className="garden-status-row">
        {gardenStats.map((stat) => (
          <div className="garden-status-card card-glass" key={stat.label}>
            <div
              className="garden-status-icon-ring"
              style={{ background: colorBg(stat.color) }}
            >
              <span className="garden-status-emoji">{stat.emoji}</span>
            </div>
            <div className="garden-status-body">
              <span className="garden-status-label">{stat.label}</span>
              <span
                className="garden-status-detail"
                style={{ color: colorVar(stat.color) }}
              >
                {stat.detail}
              </span>
            </div>
          </div>
        ))}
      </section>

      {/* ===== 4. Achievement Badges ===== */}
      <section className="badges-section card-glass">
        <h3 className="badges-section-title">🏆 成就徽章</h3>
        <div className="badges-grid">
          {badges.map((badge) => (
            <div
              className={`badge-card ${badge.earned ? 'badge-card--earned' : 'badge-card--locked'}`}
              key={badge.name}
            >
              <div
                className={`badge-icon-ring ${
                  badge.earned ? 'badge-icon-ring--earned' : ''
                }`}
                style={
                  badge.earned
                    ? {
                        borderColor: colorVar(badge.color),
                        background: colorBg(badge.color),
                      }
                    : {}
                }
              >
                <span className="badge-emoji">{badge.emoji}</span>
              </div>
              <span className="badge-name">{badge.name}</span>
              {!badge.earned && <span className="badge-lock">🔒</span>}
            </div>
          ))}
        </div>
      </section>

      {/* ===== 5. AnXiaoNing Tip ===== */}
      <section className="garden-companion-card card-glass">
        <div className="garden-companion-header">
          <div className="garden-companion-avatar">
            <span className="garden-companion-emoji">🌸</span>
          </div>
          <div>
            <span className="garden-companion-name">安小宁</span>
            <span className="garden-companion-role">花园守护者</span>
          </div>
        </div>
        <div className="garden-companion-bubble">
          <p>{COMPANION_TIP}</p>
        </div>
      </section>

      {/* ===== 6. Quick Nav ===== */}
      <div className="garden-quick-nav">
        <button
          className="garden-nav-btn card-glass"
          onClick={() => navigate('/detect')}
        >
          <span className="garden-nav-emoji">🔍</span>
          <span className="garden-nav-label">去温柔感知</span>
          <span className="garden-nav-arrow">→</span>
        </button>
        <button
          className="garden-nav-btn card-glass"
          onClick={() => navigate('/mindfulness')}
        >
          <span className="garden-nav-emoji">🧘</span>
          <span className="garden-nav-label">去正念练习</span>
          <span className="garden-nav-arrow">→</span>
        </button>
        <button
          className="garden-nav-btn card-glass"
          onClick={() => navigate('/diary')}
        >
          <span className="garden-nav-emoji">📝</span>
          <span className="garden-nav-label">写情绪日记</span>
          <span className="garden-nav-arrow">→</span>
        </button>
      </div>
    </div>
  )
}

export default GardenPage
