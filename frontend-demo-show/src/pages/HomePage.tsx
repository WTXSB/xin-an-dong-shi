import { useNavigate } from 'react-router-dom'
import { mockUser } from '../data/mockUser'
import {
  mockHomeStats,
  quickActions,
  healingRecs,
  weeklyTrend,
  recentActivities,
} from '../data/mockStats'
import './HomePage.css'

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

function HomePage() {
  const navigate = useNavigate()

  return (
    <div className="home-page">
      {/* ===== 1. Hero Card ===== */}
      <section className="hero-card card-glass">
        <div className="hero-left">
          <div className="hero-greeting-row">
            <span className="hero-wave">👋</span>
            <div>
              <h1 className="hero-title">
                下午好，{mockUser.name}。
                <span className="hero-companion">{mockUser.companion}</span>
                一直在这里陪你。
              </h1>
              <p className="hero-subtitle">
                今天也不用急着变好，先和自己待一会儿。
              </p>
            </div>
          </div>
          <div className="hero-tags">
            <span className="hero-tag tag-mood">
              {mockUser.moodEmoji} 今日状态：{mockUser.mood}
            </span>
            <span className="hero-tag tag-streak">
              🔥 连续觉察 {mockUser.streakDays} 天
            </span>
          </div>
        </div>

        {/* Emotion flower visual */}
        <div className="hero-right">
          <div className="emotion-flower">
            {[...Array(7)].map((_, i) => (
              <div
                key={i}
                className={`flower-petal ${i < 5 ? 'petal-active' : 'petal-inactive'}`}
                style={{ transform: `rotate(${i * 51.43}deg)` }}
              />
            ))}
            <div className="flower-center">
              <span className="flower-center-emoji">🌸</span>
            </div>
          </div>
          <div className="flower-label">5/7 瓣盛开</div>
        </div>

        {/* Decorative blur dots */}
        <div className="hero-blur hero-blur-1" />
        <div className="hero-blur hero-blur-2" />
      </section>

      {/* ===== 2. Data Cards ===== */}
      <section className="stats-row">
        {mockHomeStats.map((stat, i) => (
          <div className="stat-card card-glass" key={i}>
            <div className="stat-icon-ring" style={{ background: colorBg(stat.color) }}>
              <span className="stat-emoji">{stat.emoji}</span>
            </div>
            <div className="stat-body">
              <div className="stat-label">{stat.label}</div>
              <div className="stat-value" style={{ color: colorVar(stat.color) }}>
                {stat.value}
              </div>
              <div className="stat-sub">{stat.sub}</div>
            </div>
          </div>
        ))}
      </section>

      {/* ===== 3. Quick Actions ===== */}
      <section className="quick-actions">
        {quickActions.map((action, i) => (
          <button
            key={i}
            className="action-card card-glass"
            onClick={() => navigate(action.path)}
          >
            <span className="action-emoji-ring" style={{ background: `${action.color}18` }}>
              <span className="action-emoji">{action.emoji}</span>
            </span>
            <div className="action-text">
              <div className="action-label">{action.label}</div>
              <div className="action-sub">{action.sub}</div>
            </div>
            <span className="action-arrow" style={{ color: action.color }}>→</span>
          </button>
        ))}
      </section>

      {/* ===== 4 & 5. Healing Recs + Weekly Trend ===== */}
      <div className="home-columns">
        {/* Healing Recommendations */}
        <section className="healing-section card-glass">
          <h3 className="section-heading">🌿 今日疗愈推荐</h3>
          <div className="healing-list">
            {healingRecs.map((rec) => (
              <div className="healing-item" key={rec.id}>
                <span className="healing-emoji">{rec.emoji}</span>
                <div className="healing-content">
                  <div className="healing-title">{rec.title}</div>
                  <div className="healing-desc">{rec.desc}</div>
                </div>
                <span className="healing-tag">{rec.tag}</span>
                <span className="healing-duration">{rec.duration}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Weekly Trend */}
        <section className="trend-section card-glass">
          <h3 className="section-heading">📈 本周趋势</h3>
          <div className="trend-badge">
            <div className="trend-arrow-ring">
              <span className="trend-arrow">↓</span>
            </div>
            <div>
              <div className="trend-percent">{weeklyTrend.changePercent}%</div>
              <div className="trend-summary">{weeklyTrend.summary}</div>
            </div>
          </div>
          {/* Mini bar chart */}
          <div className="trend-chart">
            {weeklyTrend.days.map((d) => (
              <div className="trend-bar-col" key={d.label}>
                <div
                  className="trend-bar"
                  style={{ height: `${(d.value / 7) * 100}%` }}
                />
                <span className="trend-bar-label">{d.label}</span>
              </div>
            ))}
          </div>
          <div className="trend-axis-labels">
            <span>周一</span>
            <span>周日</span>
          </div>
          <p className="trend-note">焦虑行为指数呈下降趋势，继续保持 ✨</p>
        </section>
      </div>

      {/* ===== 6. Recent Activity ===== */}
      <section className="activity-section card-glass">
        <h3 className="section-heading">🕐 最近动态</h3>
        <div className="activity-list">
          {recentActivities.map((act) => (
            <div className="activity-item" key={act.id}>
              <span className="activity-dot" style={{ background: act.color }} />
              <span className="activity-item-emoji">{act.emoji}</span>
              <span className="activity-item-title">{act.title}</span>
              <span className="activity-item-time">{act.time}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}

export default HomePage
