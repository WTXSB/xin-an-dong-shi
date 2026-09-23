import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './LoginPage.css'

// ── Mock Data ──────────────────────────────────────────
const capabilityCards = [
  {
    emoji: '🔍',
    title: '温柔感知',
    desc: '看见焦虑小动作',
  },
  {
    emoji: '💬',
    title: '安心对话',
    desc: '慢慢说，不被评判',
  },
  {
    emoji: '🌸',
    title: '心灵花园',
    desc: '记录每一次觉察',
  },
]

// ── Component ──────────────────────────────────────────
function LoginPage() {
  const [username, setUsername] = useState('浩诚')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()

  const handleLogin = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    navigate('/home')
  }

  return (
    <div className="login-page">
      {/* ── Decorative background ── */}
      <div className="login-decor">
        <div className="login-blob login-blob--1" />
        <div className="login-blob login-blob--2" />
        <div className="login-blob login-blob--3" />
        {/* Floating petals */}
        <span className="login-petal login-petal--1">🌸</span>
        <span className="login-petal login-petal--2">🌿</span>
        <span className="login-petal login-petal--3">✨</span>
        <span className="login-petal login-petal--4">💚</span>
        <span className="login-petal login-petal--5">🌱</span>
      </div>

      {/* ── Main two-column layout ── */}
      <div className="login-container">
        {/* ===== Left: Brand Area ===== */}
        <div className="login-left">
          {/* Companion visual */}
          <div className="login-companion">
            <div className="login-companion-ring">
              <span className="login-companion-emoji">🌸</span>
            </div>
            <div className="login-companion-glow" />
          </div>

          {/* Brand text */}
          <div className="login-brand">
            <h1 className="login-brand-name">心安动识</h1>
            <span className="login-brand-en">MindEase</span>
          </div>

          {/* Taglines */}
          <div className="login-taglines">
            <p className="login-tagline-main">看见焦虑，遇见安宁</p>
            <p className="login-tagline-sub">
              一个陪你觉察情绪、理解身体信号的温柔伙伴
            </p>
          </div>

          {/* Gentle quote */}
          <div className="login-quote">
            <span className="login-quote-mark">"</span>
            <p>无论今天感觉如何，你都可以从这里慢慢开始。</p>
          </div>

          {/* Capability cards */}
          <div className="login-capabilities">
            {capabilityCards.map((cap) => (
              <div className="login-cap-card" key={cap.title}>
                <span className="login-cap-emoji">{cap.emoji}</span>
                <div className="login-cap-text">
                  <span className="login-cap-title">{cap.title}</span>
                  <span className="login-cap-desc">{cap.desc}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ===== Right: Login Card ===== */}
        <div className="login-right">
          <div className="login-card">
            <div className="login-card-header">
              <span className="login-card-icon">🌿</span>
              <h2 className="login-card-title">欢迎回来</h2>
              <p className="login-card-subtitle">
                安小宁在这里等你
              </p>
            </div>

            <form className="login-form" onSubmit={handleLogin}>
              <div className="login-field">
                <label className="login-label">👤 用户名</label>
                <input
                  className="login-input"
                  type="text"
                  placeholder="你的名字"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
              <div className="login-field">
                <label className="login-label">🔒 密码</label>
                <input
                  className="login-input"
                  type="password"
                  placeholder="演示模式可随意输入"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              <button type="submit" className="btn btn-primary login-submit-btn">
                🌸 进入心安动识
              </button>
            </form>

            <div className="login-divider">
              <span className="login-divider-text">或</span>
            </div>

            <button
              className="btn login-guest-btn"
              onClick={() => handleLogin()}
            >
              👋 先随便看看
            </button>

            <p className="login-privacy">
              🔒 你的数据只属于你。当前版本为前端演示 demo，不会上传任何真实个人数据。
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
