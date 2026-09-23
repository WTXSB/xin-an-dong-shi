import { useNavigate } from 'react-router-dom'
import './ProfilePage.css'

// ── Types ──────────────────────────────────────────────
interface StatCard {
  emoji: string
  value: string
  label: string
  color: 'green' | 'blue' | 'warm' | 'purple'
}

interface BehaviorItem {
  label: string
  percent: number
  color: string
}

interface WeekTrend {
  week: string
  value: number
}

interface BadgeItem {
  emoji: string
  name: string
  color: string
}

// ── Mock Data ──────────────────────────────────────────
const heroSummary = {
  title: '本周情绪之花：正在绽放',
  petalsOpen: 5,
  petalsTotal: 7,
  text: '这一周，你有更多时刻能够看见自己的情绪，而不是被情绪推着走。',
}

const coreStats: StatCard[] = [
  {
    emoji: '📉',
    value: '23%',
    label: '焦虑小动作 · 比上周减少',
    color: 'green',
  },
  {
    emoji: '🧘',
    value: '12 次',
    label: '正念练习 · 本月累计',
    color: 'blue',
  },
  {
    emoji: '📖',
    value: '15 天',
    label: '日记记录 · 连续天数',
    color: 'warm',
  },
  {
    emoji: '💬',
    value: '4 次',
    label: '安心对话 · 本周',
    color: 'purple',
  },
]

const thirtyDayTrend: WeekTrend[] = [
  { week: '30 天前', value: 8 },
  { week: '3 周前', value: 6 },
  { week: '2 周前', value: 5 },
  { week: '上周', value: 4 },
  { week: '本周', value: 3 },
]

const trendSummary =
  '从每天 8 次到每天 3 次，减少约 62.5%。这不是一夜之间完成的，而是一次次觉察累积出来的。'

const behaviorDistribution: BehaviorItem[] = [
  { label: '咬指甲倾向', percent: 45, color: 'warm' },
  { label: '抓头发倾向', percent: 25, color: 'blue' },
  { label: '抠手指倾向', percent: 20, color: 'purple' },
  { label: '其他紧张小动作', percent: 10, color: 'green' },
]

const peakTimeInsight = {
  morning: { label: '上午', desc: '较平静', level: 'low' },
  afternoon: { label: '下午 2-4 点', desc: '更容易紧张', level: 'high' },
  evening: { label: '晚上', desc: '逐渐放松', level: 'low' },
  text: '你的紧张小动作更多出现在下午 2-4 点，也许可以在这个时间前安排 1 分钟呼吸练习。',
}

const adviceList = [
  {
    emoji: '🧘',
    text: '下午开始学习或会议前，先做 1 分钟呼吸',
  },
  {
    emoji: '🤲',
    text: '手靠近嘴边时，可以换成握住柔软物品',
  },
  {
    emoji: '📝',
    text: '睡前写下今天一个已经完成的小进步',
  },
]

const badges: BadgeItem[] = [
  { emoji: '🌸', name: '觉察新手', color: 'purple' },
  { emoji: '🧘', name: '正念坚持者', color: 'green' },
  { emoji: '📖', name: '情绪记录者', color: 'blue' },
  { emoji: '💎', name: '星光收集者', color: 'warm' },
]

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

const behaviorColorVar = (c: string) => {
  const map: Record<string, string> = {
    warm: 'var(--warm)',
    blue: 'var(--blue)',
    purple: 'var(--purple)',
    green: 'var(--primary)',
  }
  return map[c] || c
}

// ── Component ──────────────────────────────────────────
function ProfilePage() {
  const navigate = useNavigate()
  const maxTrendValue = Math.max(...thirtyDayTrend.map((d) => d.value))

  return (
    <div className="profile-page">
      {/* ===== Page Header ===== */}
      <header className="profile-header">
        <h1 className="profile-title">📊 情绪健康画像</h1>
        <p className="profile-subtitle">
          看见变化，也看见正在努力的自己
        </p>
      </header>

      {/* ===== 1. Hero Summary Card ===== */}
      <section className="hero-summary-card card-glass">
        <div className="hero-summary-left">
          <h2 className="hero-summary-title">{heroSummary.title}</h2>
          <div className="hero-petal-progress">
            <div className="petal-count">
              <span className="petal-count-num">{heroSummary.petalsOpen}</span>
              <span className="petal-count-sep">/</span>
              <span className="petal-count-total">{heroSummary.petalsTotal}</span>
              <span className="petal-count-unit"> 瓣盛开</span>
            </div>
            <div className="petal-mini-track">
              <div
                className="petal-mini-fill"
                style={{
                  width: `${(heroSummary.petalsOpen / heroSummary.petalsTotal) * 100}%`,
                }}
              />
            </div>
          </div>
          <p className="hero-summary-text">{heroSummary.text}</p>
        </div>
        <div className="hero-summary-right">
          {/* CSS Emotion Flower */}
          <div className="hero-flower-visual">
            {[...Array(7)].map((_, i) => (
              <div
                key={i}
                className={`hero-flower-petal ${
                  i < heroSummary.petalsOpen ? 'petal--open' : 'petal--closed'
                }`}
                style={{ transform: `rotate(${i * 51.43}deg)` }}
              />
            ))}
            <div className="hero-flower-center">
              <span className="hero-flower-emoji">🌸</span>
            </div>
          </div>
        </div>
      </section>

      {/* ===== 2. Core Data Cards ===== */}
      <section className="core-stats-row">
        {coreStats.map((stat, i) => (
          <div className="core-stat-card card-glass" key={i}>
            <div
              className="core-stat-icon-ring"
              style={{ background: colorBg(stat.color) }}
            >
              <span className="core-stat-emoji">{stat.emoji}</span>
            </div>
            <div className="core-stat-body">
              <div
                className="core-stat-value"
                style={{ color: colorVar(stat.color) }}
              >
                {stat.value}
              </div>
              <div className="core-stat-label">{stat.label}</div>
            </div>
          </div>
        ))}
      </section>

      {/* ===== 3 & 4: 30-Day Trend + Behavior Distribution ===== */}
      <div className="profile-two-col">
        {/* 30-Day Trend */}
        <section className="trend-card card-glass">
          <h3 className="section-heading">📉 30 天趋势</h3>
          <p className="trend-card-subtitle">焦虑小动作 · 每日平均次数</p>

          <div className="trend-chart">
            {thirtyDayTrend.map((d) => (
              <div className="trend-bar-col" key={d.week}>
                <span className="trend-bar-value">{d.value} 次</span>
                <div className="trend-bar-wrapper">
                  <div
                    className="trend-bar"
                    style={{
                      height: `${(d.value / maxTrendValue) * 100}%`,
                    }}
                  />
                </div>
                <span className="trend-bar-label">{d.week}</span>
              </div>
            ))}
          </div>

          <div className="trend-summary-box">
            <span className="trend-summary-icon">💡</span>
            <p className="trend-summary-text">{trendSummary}</p>
          </div>
        </section>

        {/* Behavior Distribution */}
        <section className="behavior-card card-glass">
          <h3 className="section-heading">🔍 行为分布</h3>
          <p className="section-subtitle">近 30 天紧张小动作占比</p>

          <div className="behavior-list">
            {behaviorDistribution.map((item) => (
              <div className="behavior-item" key={item.label}>
                <div className="behavior-header">
                  <span className="behavior-label">{item.label}</span>
                  <span
                    className="behavior-percent"
                    style={{ color: behaviorColorVar(item.color) }}
                  >
                    {item.percent}%
                  </span>
                </div>
                <div className="behavior-track">
                  <div
                    className="behavior-fill"
                    style={{
                      width: `${item.percent}%`,
                      background: `linear-gradient(90deg, ${behaviorColorVar(item.color)}99, ${behaviorColorVar(item.color)})`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>

          {/* Mini donut illusion — 4 color dots */}
          <div className="behavior-legend">
            {behaviorDistribution.map((item) => (
              <div className="legend-dot-row" key={item.label}>
                <span
                  className="legend-dot"
                  style={{ background: behaviorColorVar(item.color) }}
                />
                <span className="legend-label">{item.label}</span>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* ===== 5. Peak Time Card ===== */}
      <section className="peak-card card-glass">
        <h3 className="section-heading">⏰ 高频触发时段</h3>
        <p className="section-subtitle">一天中的紧张小动作分布</p>

        <div className="peak-timeline">
          <div className="peak-block peak-block--morning">
            <div className="peak-block-icon">🌅</div>
            <div className="peak-block-label">{peakTimeInsight.morning.label}</div>
            <div className="peak-block-desc">{peakTimeInsight.morning.desc}</div>
          </div>

          <div className="peak-connector-line">
            <div className="peak-connector-arrow">→</div>
          </div>

          <div className="peak-block peak-block--afternoon peak-block--highlight">
            <div className="peak-block-icon">☀️</div>
            <div className="peak-block-label">
              {peakTimeInsight.afternoon.label}
            </div>
            <div className="peak-block-desc">
              {peakTimeInsight.afternoon.desc}
            </div>
            <div className="peak-alert-dot" />
          </div>

          <div className="peak-connector-line">
            <div className="peak-connector-arrow">→</div>
          </div>

          <div className="peak-block peak-block--evening">
            <div className="peak-block-icon">🌙</div>
            <div className="peak-block-label">{peakTimeInsight.evening.label}</div>
            <div className="peak-block-desc">{peakTimeInsight.evening.desc}</div>
          </div>
        </div>

        <div className="peak-insight-box">
          <span className="peak-insight-icon">🌸</span>
          <p className="peak-insight-text">{peakTimeInsight.text}</p>
        </div>
      </section>

      {/* ===== 6 & 7: Advice + Badges ===== */}
      <div className="profile-two-col">
        {/* AnXiaoNing Advice */}
        <section className="advice-card card-glass">
          <div className="advice-header">
            <div className="advice-avatar-ring">
              <span className="advice-avatar-emoji">🌸</span>
            </div>
            <div>
              <h3 className="advice-title">安小宁的温柔建议</h3>
              <p className="advice-subtitle">
                根据你的数据，慢慢调整就好
              </p>
            </div>
          </div>
          <div className="advice-list">
            {adviceList.map((item, i) => (
              <div className="advice-item" key={i}>
                <div className="advice-item-number">{i + 1}</div>
                <span className="advice-item-emoji">{item.emoji}</span>
                <span className="advice-item-text">{item.text}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Achievement Badges */}
        <section className="badges-card card-glass">
          <h3 className="section-heading">🏆 本周获得的徽章</h3>
          <p className="section-subtitle">每一次觉察都值得被看见</p>

          <div className="badges-grid">
            {badges.map((badge) => (
              <div className="badge-item" key={badge.name}>
                <div
                  className="badge-icon-ring"
                  style={{
                    background: `linear-gradient(135deg, ${colorBg(badge.color)}, ${colorBg(badge.color)}00)`,
                    borderColor: colorVar(badge.color),
                  }}
                >
                  <span className="badge-emoji">{badge.emoji}</span>
                </div>
                <span className="badge-name">{badge.name}</span>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* ===== 8. Quick Nav ===== */}
      <div className="profile-quick-nav">
        <button
          className="profile-nav-btn card-glass"
          onClick={() => navigate('/mindfulness')}
        >
          <span className="profile-nav-emoji">🧘</span>
          <span className="profile-nav-label">去正念练习</span>
          <span className="profile-nav-arrow">→</span>
        </button>
        <button
          className="profile-nav-btn card-glass"
          onClick={() => navigate('/diary')}
        >
          <span className="profile-nav-emoji">📝</span>
          <span className="profile-nav-label">写一篇日记</span>
          <span className="profile-nav-arrow">→</span>
        </button>
        <button
          className="profile-nav-btn card-glass"
          onClick={() => navigate('/home')}
        >
          <span className="profile-nav-emoji">🏠</span>
          <span className="profile-nav-label">回到首页</span>
          <span className="profile-nav-arrow">→</span>
        </button>
      </div>
    </div>
  )
}

export default ProfilePage
