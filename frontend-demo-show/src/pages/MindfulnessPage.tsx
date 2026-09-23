import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import './MindfulnessPage.css'

// ── Types ──────────────────────────────────────────────
type BreathingPhase = 'idle' | 'inhale' | 'hold' | 'exhale'

interface CourseItem {
  id: number
  emoji: string
  title: string
  duration: string
}

interface StatItem {
  label: string
  value: string | number
  emoji: string
  color: string
}

// ── Constants ──────────────────────────────────────────
const PHASE_DURATIONS: Record<Exclude<BreathingPhase, 'idle'>, number> = {
  inhale: 4,
  hold: 4,
  exhale: 6,
}

const PHASE_LABELS: Record<Exclude<BreathingPhase, 'idle'>, string> = {
  inhale: '吸气',
  hold: '停留',
  exhale: '呼气',
}

const PHASE_DURATION_LABELS: Record<Exclude<BreathingPhase, 'idle'>, string> = {
  inhale: '4 秒',
  hold: '4 秒',
  exhale: '6 秒',
}

const TICK_MS = 80 // ~12.5 fps — smooth enough for the circle, light on CPU

// ── Mock Data ──────────────────────────────────────────
const statsData: StatItem[] = [
  { label: '今日练习', value: '3 次', emoji: '🧘', color: 'green' },
  { label: '累计时长', value: '128 分钟', emoji: '⏱️', color: 'blue' },
  { label: '平静指数', value: '82 分', emoji: '😌', color: 'purple' },
]

const courses: CourseItem[] = [
  { id: 1, emoji: '🌿', title: '晨间唤醒', duration: '5 分钟' },
  { id: 2, emoji: '🌊', title: '焦虑急救', duration: '3 分钟' },
  { id: 3, emoji: '🌙', title: '睡前放松', duration: '10 分钟' },
  { id: 4, emoji: '🧘', title: '身体扫描', duration: '15 分钟' },
  { id: 5, emoji: '💆', title: '渐进放松', duration: '10 分钟' },
  { id: 6, emoji: '❤️', title: '自我慈悲', duration: '8 分钟' },
]

const COMPANION_IDLE =
  '我会陪你慢慢来。走神也没关系，发现自己走神的那一刻，就是一次觉察。'

const COMPANION_ACTIVE =
  '很好，现在只需要跟着圆环呼吸。你不用追求完美，只要回来就可以。'

// ── Helpers ────────────────────────────────────────────
function getBreathingScale(phase: BreathingPhase, elapsedMs: number): number {
  if (phase === 'idle') return 0.68
  const elapsed = elapsedMs / 1000
  const duration = PHASE_DURATIONS[phase] ?? 4
  const progress = Math.min(elapsed / duration, 1)

  switch (phase) {
    case 'inhale':
      // Ease-out: starts fast, slows toward peak
      return 0.6 + 0.4 * (1 - (1 - progress) ** 3)
    case 'hold':
      return 1.0
    case 'exhale':
      // Ease-in: starts slow, accelerates toward end
      return 1.0 - 0.4 * progress ** 2.5
    default:
      return 0.68
  }
}

// ── Component ──────────────────────────────────────────
function MindfulnessPage() {
  const navigate = useNavigate()

  // Breathing state
  const [phase, setPhase] = useState<BreathingPhase>('idle')
  const [phaseElapsed, setPhaseElapsed] = useState(0)
  const [roundCount, setRoundCount] = useState(0)
  const [activeCourse, setActiveCourse] = useState<string | null>(null)
  const [showReward, setShowReward] = useState(false)
  const [rewardText, setRewardText] = useState('')

  // Refs for timer
  const tickRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const rewardTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Derived values
  const isActive = phase !== 'idle'
  const scale = getBreathingScale(phase, phaseElapsed)
  // Echo ring with 400ms delay
  const echoScale = getBreathingScale(
    phase,
    Math.max(0, phaseElapsed - 400),
  )
  const currentPhaseLabel = isActive ? PHASE_LABELS[phase] ?? '' : ''
  const currentPhaseDuration = isActive
    ? PHASE_DURATION_LABELS[phase] ?? ''
    : ''
  const remainingSeconds = isActive
    ? Math.ceil(((PHASE_DURATIONS[phase] ?? 4) * 1000 - phaseElapsed) / 1000)
    : 0

  // ── Phase transition ──────────────────────────────
  const advancePhase = useCallback(() => {
    setPhase((prev) => {
      if (prev === 'inhale') return 'hold'
      if (prev === 'hold') return 'exhale'
      if (prev === 'exhale') {
        // Round complete!
        setRoundCount((r) => {
          const newRound = r + 1
          setRewardText(`+2 星光币 · 第 ${newRound} 轮完成`)
          return newRound
        })
        setShowReward(true)
        if (rewardTimerRef.current) clearTimeout(rewardTimerRef.current)
        rewardTimerRef.current = setTimeout(() => setShowReward(false), 2800)
        return 'inhale'
      }
      return prev
    })
    setPhaseElapsed(0)
  }, [])

  // ── Main breathing tick ───────────────────────────
  useEffect(() => {
    if (!isActive) return

    tickRef.current = setInterval(() => {
      setPhaseElapsed((prev) => {
        const next = prev + TICK_MS
        const durationMs = (PHASE_DURATIONS[phase] ?? 4) * 1000
        if (next >= durationMs) {
          advancePhase()
          return 0
        }
        return next
      })
    }, TICK_MS)

    return () => {
      if (tickRef.current) {
        clearInterval(tickRef.current)
        tickRef.current = null
      }
    }
  }, [isActive, phase, advancePhase])

  // Cleanup reward timer on unmount
  useEffect(() => {
    return () => {
      if (rewardTimerRef.current) clearTimeout(rewardTimerRef.current)
    }
  }, [])

  // ── Handlers ──────────────────────────────────────
  const handleStart = () => {
    setPhase('inhale')
    setPhaseElapsed(0)
    setRoundCount(0)
  }

  const handlePause = () => {
    setPhase('idle')
    // Preserve roundCount so resume continues counting
  }

  const handleReset = () => {
    setPhase('idle')
    setPhaseElapsed(0)
    setRoundCount(0)
    setShowReward(false)
  }

  const handleResume = () => {
    // Resume from the phase that makes sense — restart inhale
    setPhase('inhale')
    setPhaseElapsed(0)
  }

  const handleCourseClick = (course: CourseItem) => {
    setActiveCourse(course.title)
    // Auto-start if idle
    if (!isActive) {
      setPhase('inhale')
      setPhaseElapsed(0)
      setRoundCount(0)
    }
  }

  // ── Circle size computation ───────────────────────
  // Circle diameter in px: base is 200px, scales with breathing
  const circleSize = Math.round(200 * scale)
  const echoSize = Math.round(200 * echoScale + 16) // echo ring is slightly larger

  // Glow intensity: strongest at peak of inhale, fades during exhale
  const glowAlpha =
    phase === 'idle' ? 0.08 : 0.08 + 0.22 * ((scale - 0.6) / 0.4)

  const breathingCardTitle = activeCourse
    ? `${activeCourse}`
    : '4-4-6 呼吸练习'

  return (
    <div className="mindfulness-page">
      {/* ===== Page Header ===== */}
      <header className="mindfulness-header">
        <h1 className="mindfulness-title">🧘 正念练习</h1>
        <p className="mindfulness-subtitle">
          此刻不需要做得很好，只需要慢慢呼吸
        </p>
      </header>

      {/* ===== Two-Column Layout ===== */}
      <div className="mindfulness-layout">
        {/* ── Left: Breathing Practice ── */}
        <div className="mindfulness-main">
          {/* Breathing Circle Card */}
          <section className="breathing-card card-glass">
            <div className="breathing-card-header">
              <h2 className="breathing-card-title">{breathingCardTitle}</h2>
              {isActive && (
                <span className="breathing-round-badge">
                  第 {roundCount + 1} 轮
                </span>
              )}
            </div>

            {/* Circle Area */}
            <div className="breathing-circle-area">
              {/* Echo ring */}
              <div
                className="breathing-ring breathing-ring--echo"
                style={{
                  width: echoSize,
                  height: echoSize,
                }}
              />

              {/* Main breathing ring */}
              <div
                className="breathing-ring breathing-ring--main"
                style={{
                  width: circleSize,
                  height: circleSize,
                  boxShadow: isActive
                    ? `0 0 ${Math.round(60 * scale)}px rgba(91, 174, 138, ${glowAlpha}), 0 0 ${Math.round(120 * scale)}px rgba(91, 174, 138, ${glowAlpha * 0.6})`
                    : '0 0 0 transparent',
                }}
              >
                {/* Inner content */}
                <div className="breathing-ring-inner">
                  {isActive ? (
                    <>
                      <span className="breathing-phase-label">
                        {currentPhaseLabel}
                      </span>
                      <span className="breathing-countdown">
                        {remainingSeconds}
                      </span>
                      <span className="breathing-phase-duration">
                        {currentPhaseDuration}
                      </span>
                    </>
                  ) : (
                    <>
                      <span className="breathing-idle-icon">🌸</span>
                      <span className="breathing-idle-text">准备开始</span>
                    </>
                  )}
                </div>
              </div>
            </div>

            {/* Reward toast */}
            <div
              className={`breathing-reward ${showReward ? 'breathing-reward--visible' : ''}`}
            >
              <span className="reward-toast-emoji">💎</span>
              <span className="reward-toast-text">{rewardText}</span>
            </div>

            {/* Phase progress bar */}
            {isActive && (
              <div className="breathing-progress-bar">
                <div
                  className="breathing-progress-fill"
                  style={{
                    width: `${(phaseElapsed / ((PHASE_DURATIONS[phase] ?? 4) * 1000)) * 100}%`,
                    transition: `width ${TICK_MS}ms linear`,
                  }}
                />
              </div>
            )}

            {/* Controls */}
            <div className="breathing-controls">
              {!isActive ? (
                <button
                  className="btn btn-primary breathing-btn breathing-btn--start"
                  onClick={roundCount > 0 ? handleResume : handleStart}
                >
                  {roundCount > 0 ? '▶ 继续练习' : '🌸 开始练习'}
                </button>
              ) : (
                <button
                  className="btn btn-secondary breathing-btn"
                  onClick={handlePause}
                >
                  ⏸ 暂停
                </button>
              )}
              <button
                className="btn breathing-btn breathing-btn--reset"
                onClick={handleReset}
                disabled={!isActive && roundCount === 0}
              >
                ↺ 重置
              </button>
            </div>
          </section>

          {/* Quick Nav Buttons */}
          <div className="mindfulness-quick-nav">
            <button
              className="quick-nav-btn card-glass"
              onClick={() => navigate('/diary')}
            >
              <span className="quick-nav-emoji">📝</span>
              <span className="quick-nav-label">记录练习后的感受</span>
              <span className="quick-nav-arrow">→</span>
            </button>
            <button
              className="quick-nav-btn card-glass"
              onClick={() => navigate('/profile')}
            >
              <span className="quick-nav-emoji">📊</span>
              <span className="quick-nav-label">查看健康画像</span>
              <span className="quick-nav-arrow">→</span>
            </button>
          </div>
        </div>

        {/* ── Right: Stats + Companion ── */}
        <div className="mindfulness-side">
          {/* Stats Cards */}
          <div className="stats-cards">
            {statsData.map((stat, i) => (
              <div className="stat-mini-card card-glass" key={i}>
                <span className="stat-mini-emoji">{stat.emoji}</span>
                <div className="stat-mini-body">
                  <div className="stat-mini-value">{stat.value}</div>
                  <div className="stat-mini-label">{stat.label}</div>
                </div>
              </div>
            ))}
          </div>

          {/* AnXiaoNing Companion Card */}
          <div className="companion-card card-glass">
            <div className="companion-header">
              <div className="companion-avatar-ring">
                <span className="companion-avatar-emoji">🌸</span>
              </div>
              <div className="companion-header-info">
                <span className="companion-name">安小宁</span>
                <span className="companion-role">你的正念伙伴</span>
              </div>
            </div>
            <div className={`companion-bubble ${isActive ? 'companion-bubble--active' : ''}`}>
              <p>{isActive ? COMPANION_ACTIVE : COMPANION_IDLE}</p>
            </div>
          </div>
        </div>
      </div>

      {/* ===== Course Center ===== */}
      <section className="course-center card-glass">
        <h3 className="course-center-title">📚 课程中心</h3>
        <p className="course-center-subtitle">
          选择一门课程，开始你的正念之旅
        </p>
        <div className="course-grid">
          {courses.map((course) => (
            <button
              key={course.id}
              className={`course-card ${
                activeCourse === course.title ? 'course-card--active' : ''
              }`}
              onClick={() => handleCourseClick(course)}
            >
              <span className="course-emoji">{course.emoji}</span>
              <div className="course-info">
                <span className="course-title">{course.title}</span>
                <span className="course-duration">⏱️ {course.duration}</span>
              </div>
              {activeCourse === course.title && (
                <span className="course-check">✓</span>
              )}
            </button>
          ))}
        </div>
      </section>
    </div>
  )
}

export default MindfulnessPage
