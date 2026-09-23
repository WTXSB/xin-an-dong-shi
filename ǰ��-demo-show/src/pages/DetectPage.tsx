import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './DetectPage.css'

interface BehaviorLog {
  id: number
  behavior: string
  confidence: number
  time: string
}

function DetectPage() {
  const navigate = useNavigate()

  const [isScanning, setIsScanning] = useState(false)
  const [detectedBehavior, setDetectedBehavior] = useState<{
    type: string
    confidence: number
  } | null>(null)
  const [behaviorLog, setBehaviorLog] = useState<BehaviorLog[]>([])
  const [observationNote, setObservationNote] = useState(
    '我会轻轻陪着你，只记录需要被照顾的信号，不会评判你。'
  )
  const [noteType, setNoteType] = useState<'default' | 'gentle'>('default')

  const now = () =>
    new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  const handleStartScan = () => {
    setIsScanning(true)
    setDetectedBehavior(null)
    setBehaviorLog([])
    setObservationNote(
      '我会轻轻陪着你，只记录需要被照顾的信号，不会评判你。'
    )
    setNoteType('default')
  }

  const handleStopScan = () => {
    setIsScanning(false)
    setDetectedBehavior(null)
  }

  const handleSimulateNailBiting = () => {
    if (!isScanning) {
      setIsScanning(true)
    }
    setDetectedBehavior({ type: '咬指甲倾向', confidence: 94 })
    setBehaviorLog((prev) => [
      ...prev,
      {
        id: Date.now(),
        behavior: '咬指甲倾向',
        confidence: 94,
        time: now(),
      },
    ])
  }

  const handleSimulateAnxiety = () => {
    setObservationNote(
      '我注意到你的手靠近了嘴边。没关系，这可能只是身体在提醒你有点紧张。要不要一起做 3 次呼吸？'
    )
    setNoteType('gentle')
  }

  return (
    <div className="detect-page">
      {/* ===== Page Header ===== */}
      <header className="detect-header">
        <h1 className="detect-title">🌸 温柔感知空间</h1>
        <p className="detect-subtitle">
          安小宁陪你看见自己，而不是审视自己
        </p>
      </header>

      {/* ===== Main Two-Column Area ===== */}
      <div className="detect-main">
        {/* Left: Camera Card */}
        <section
          className={`camera-card card-glass ${
            isScanning ? 'camera-scanning' : ''
          } ${
            detectedBehavior ? 'camera-detected' : ''
          }`}
        >
          <div className="camera-inner">
            {!isScanning ? (
              /* Idle state */
              <div className="camera-idle">
                <div className="camera-placeholder-icon">📷</div>
                <p className="camera-idle-text">摄像头未开启</p>
                <p className="camera-idle-hint">点击下方按钮开始温柔感知</p>
              </div>
            ) : (
              /* Scanning state */
              <div className="camera-active">
                {/* Breathing rings */}
                <div className="breathing-ring ring-1" />
                <div className="breathing-ring ring-2" />
                <div className="breathing-ring ring-3" />

                {/* Scanning line */}
                <div className="scan-line" />

                <div className="camera-active-content">
                  <span className="camera-active-icon">🌸</span>
                  <p className="camera-active-label">
                    {detectedBehavior ? '关注中...' : '感知中...'}
                  </p>
                  <p className="camera-active-hint">
                    安小宁正在温柔地陪伴你
                  </p>
                </div>

                {/* Detection tag */}
                {detectedBehavior && (
                  <div className="detection-tag">
                    <span className="detection-tag-dot" />
                    <span className="detection-tag-label">
                      检测到：{detectedBehavior.type}
                    </span>
                    <span className="detection-tag-confidence">
                      置信度 {detectedBehavior.confidence}%
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Camera control button */}
          <div className="camera-controls">
            {!isScanning ? (
              <button className="btn btn-primary camera-btn" onClick={handleStartScan}>
                🌸 开启温柔感知
              </button>
            ) : (
              <button className="btn btn-secondary camera-btn" onClick={handleStopScan}>
                结束感知
              </button>
            )}
          </div>
        </section>

        {/* Right: Observation Notes + Simulate Buttons */}
        <div className="detect-side">
          {/* Observation Note */}
          <section className={`obs-card card-glass ${noteType}`}>
            <div className="obs-header">
              <span className="obs-avatar">🌸</span>
              <span className="obs-name">安小宁 · 观察笔记</span>
            </div>
            <div className={`obs-bubble ${noteType}`}>
              <p>{observationNote}</p>
            </div>
            {noteType === 'gentle' && (
              <div className="obs-actions">
                <button className="btn btn-primary btn-sm">一起呼吸</button>
                <button className="btn btn-secondary btn-sm">稍等一下</button>
              </div>
            )}
          </section>

          {/* Simulation Buttons */}
          <section className="sim-card card-glass">
            <h3 className="sim-title">🔧 模拟检测（Demo）</h3>
            <p className="sim-hint">点击下方按钮模拟 AI 检测行为</p>
            <div className="sim-buttons">
              <button
                className="btn sim-btn sim-btn-warm"
                onClick={handleSimulateNailBiting}
              >
                🖐️ 模拟检测到咬指甲
              </button>
              <button
                className="btn sim-btn sim-btn-blue"
                onClick={handleSimulateAnxiety}
              >
                🙋 模拟检测到焦虑小动作
              </button>
            </div>
          </section>
        </div>
      </div>

      {/* ===== Behavior Log (shows when detections exist) ===== */}
      {behaviorLog.length > 0 && (
        <section className="behavior-log card-glass">
          <h3 className="section-heading">📋 本次行为记录</h3>
          <div className="log-list">
            {behaviorLog.map((log) => (
              <div className="log-item" key={log.id}>
                <span className="log-dot" />
                <span className="log-behavior">{log.behavior}</span>
                <span className="log-confidence">
                  置信度 {log.confidence}%
                </span>
                <span className="log-time">{log.time}</span>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ===== Perception Report ===== */}
      {behaviorLog.length > 0 && (
        <section className="report-card card-glass">
          <h3 className="section-heading">📊 本次感知报告</h3>

          <div className="report-grid">
            <div className="report-stat">
              <span className="report-stat-emoji">⏱️</span>
              <div className="report-stat-value">15</div>
              <div className="report-stat-label">感知时长（分钟）</div>
            </div>
            <div className="report-stat">
              <span className="report-stat-emoji">😌</span>
              <div className="report-stat-value">12</div>
              <div className="report-stat-label">平静时段（分钟）</div>
            </div>
            <div className="report-stat">
              <span className="report-stat-emoji">👀</span>
              <div className="report-stat-value">2</div>
              <div className="report-stat-label">关注时段（分钟）</div>
            </div>
            <div className="report-stat">
              <span className="report-stat-emoji">🖐️</span>
              <div className="report-stat-value">{behaviorLog.length}</div>
              <div className="report-stat-label">咬指甲倾向（次）</div>
            </div>
          </div>

          <div className="report-reward">
            <span className="reward-icon">💎</span>
            <span className="reward-text">
              本次奖励：<strong>+5 星光币</strong>
            </span>
          </div>

          <div className="report-summary">
            <div className="summary-header">
              <span className="summary-avatar">🌸</span>
              <span className="summary-name">安小宁总结</span>
            </div>
            <p className="summary-text">
              今天大部分时间都很平静，那两次小动作也只是身体的信号。你已经开始看见自己了，这很重要。
            </p>
          </div>
        </section>
      )}

      {/* ===== Quick Actions ===== */}
      <section className="detect-quick-actions">
        <button
          className="quick-action-btn card-glass"
          onClick={() => navigate('/mindfulness')}
        >
          <span className="qa-emoji">🧘</span>
          <span className="qa-label">开始 1 分钟呼吸</span>
          <span className="qa-arrow">→</span>
        </button>
        <button
          className="quick-action-btn card-glass"
          onClick={() => navigate('/diary')}
        >
          <span className="qa-emoji">📝</span>
          <span className="qa-label">记录此刻感受</span>
          <span className="qa-arrow">→</span>
        </button>
        <button
          className="quick-action-btn card-glass"
          onClick={() => navigate('/profile')}
        >
          <span className="qa-emoji">📊</span>
          <span className="qa-label">查看健康画像</span>
          <span className="qa-arrow">→</span>
        </button>
      </section>
    </div>
  )
}

export default DetectPage
