import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import './DiaryPage.css'

// ── Types ──────────────────────────────────────────────
interface MoodOption {
  emoji: string
  label: string
}

interface BehaviorSticker {
  emoji: string
  label: string
  detail: string
  color: 'green' | 'blue' | 'warm' | 'purple'
}

interface HistoryEntry {
  id: number
  date: string
  preview: string
}

// ── Mock Data ──────────────────────────────────────────
const moodOptions: MoodOption[] = [
  { emoji: '😊', label: '平静' },
  { emoji: '😰', label: '焦虑' },
  { emoji: '😴', label: '疲惫' },
  { emoji: '😢', label: '难过' },
  { emoji: '🌤️', label: '好转' },
  { emoji: '💪', label: '有力量' },
]

const behaviorStickers: BehaviorSticker[] = [
  { emoji: '🖐️', label: '咬指甲倾向', detail: '×2', color: 'warm' },
  { emoji: '🧘', label: '完成正念', detail: '×1', color: 'green' },
  { emoji: '💬', label: '安心对话', detail: '30 分钟', color: 'blue' },
  { emoji: '🌸', label: '平静时段', detail: '12 分钟', color: 'purple' },
]

const historyEntries: HistoryEntry[] = [
  {
    id: 1,
    date: '6月10日',
    preview: '今天做了呼吸练习，感觉平静了一点。晚上还写了一点日记，慢慢来。',
  },
  {
    id: 2,
    date: '6月9日',
    preview: '下午有些紧张，但后来慢慢缓过来了。安小宁陪我聊了一会儿。',
  },
  {
    id: 3,
    date: '6月8日',
    preview: '睡前写了一点自己的感受。今天整体来说还不错，完成了一次温柔感知。',
  },
  {
    id: 4,
    date: '6月7日',
    preview: '第一次尝试记录情绪。还不太习惯把感受写下来，但愿意试一试。',
  },
]

const AI_OBSERVATION = `今天你完成了一次温柔感知，也做了一段正念练习。虽然出现了几次紧张的小动作，但你已经开始看见它们，而不是被它们推着走。`

const TODAY_GUIDANCE = '不用写得完整，能留下几个词也很好。'

const SAVE_CONFIRMATION = '已经轻轻收好了。谢谢你愿意照顾自己的感受。'

// ── Color Helpers ──────────────────────────────────────
const stickerColorVar = (c: string) => {
  const map: Record<string, string> = {
    green: 'var(--primary)',
    blue: 'var(--blue)',
    warm: 'var(--warm)',
    purple: 'var(--purple)',
  }
  return map[c] || c
}

const stickerColorBg = (c: string) => {
  const map: Record<string, string> = {
    green: 'var(--primary-bg)',
    blue: 'var(--blue-bg)',
    warm: 'var(--warm-bg)',
    purple: 'var(--purple-bg)',
  }
  return map[c] || 'var(--primary-bg)'
}

// ── Component ──────────────────────────────────────────
function DiaryPage() {
  const navigate = useNavigate()

  // Today's diary
  const [diaryText, setDiaryText] = useState('')
  const [selectedMood, setSelectedMood] = useState<string | null>(null)
  const [isSaved, setIsSaved] = useState(false)
  const [showConfirmation, setShowConfirmation] = useState(false)

  // Tomorrow message
  const [tomorrowMsg, setTomorrowMsg] = useState('')
  const [tomorrowSaved, setTomorrowSaved] = useState(false)

  // Refs
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const confirmTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Cleanup timers
  useEffect(() => {
    return () => {
      if (confirmTimerRef.current) clearTimeout(confirmTimerRef.current)
    }
  }, [])

  // ── Handlers ──────────────────────────────────────
  const handleMoodSelect = (mood: MoodOption) => {
    setSelectedMood((prev) => (prev === mood.label ? null : mood.label))
  }

  const handleSaveDiary = () => {
    setIsSaved(true)
    setShowConfirmation(true)
    if (confirmTimerRef.current) clearTimeout(confirmTimerRef.current)
    confirmTimerRef.current = setTimeout(() => setShowConfirmation(false), 3500)
  }

  const handleSaveTomorrow = () => {
    if (!tomorrowMsg.trim()) return
    setTomorrowSaved(true)
    setTimeout(() => setTomorrowSaved(false), 2500)
  }

  const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setDiaryText(e.target.value)
    // Reset saved state if user edits after saving
    if (isSaved) {
      setIsSaved(false)
      setShowConfirmation(false)
    }
  }

  // ── Render ────────────────────────────────────────
  return (
    <div className="diary-page">
      {/* ===== Page Header ===== */}
      <header className="diary-page-header">
        <h1 className="diary-page-title">📔 情绪日记</h1>
        <p className="diary-page-subtitle">今天，和自己待一会儿</p>
      </header>

      {/* ===== Two-Column Layout ===== */}
      <div className="diary-layout">
        {/* ── Left: Main Content ── */}
        <div className="diary-main">
          {/* ===== 1. Today's Record Main Card ===== */}
          <section className="diary-today-card card-glass">
            <div className="today-card-header">
              <div className="today-date-row">
                <span className="today-date-icon">📅</span>
                <span className="today-date-label">今天</span>
                {selectedMood && (
                  <span className="today-mood-badge">
                    {moodOptions.find((m) => m.label === selectedMood)?.emoji}{' '}
                    今天的心情：{selectedMood}
                  </span>
                )}
              </div>
              <p className="today-guidance">{TODAY_GUIDANCE}</p>
            </div>

            {/* Text area */}
            <div className="today-textarea-wrapper">
              <textarea
                ref={textareaRef}
                className="today-textarea"
                placeholder="这一刻，我想记录的是……"
                value={diaryText}
                onChange={handleTextareaChange}
                rows={5}
              />
              <div className="today-textarea-footer">
                <span className="today-char-count">
                  {diaryText.length} 字
                </span>
              </div>
            </div>

            {/* Save button */}
            <button
              className="btn btn-primary today-save-btn"
              onClick={handleSaveDiary}
              disabled={!diaryText.trim() && !selectedMood}
            >
              💾 保存今日心情
            </button>

            {/* Save confirmation */}
            <div
              className={`today-confirmation ${showConfirmation ? 'today-confirmation--visible' : ''}`}
            >
              <span className="confirmation-icon">🌸</span>
              <span className="confirmation-text">{SAVE_CONFIRMATION}</span>
            </div>
          </section>

          {/* ===== 2. Mood Selection ===== */}
          <section className="mood-section card-glass">
            <h3 className="section-label">今天的心情</h3>
            <div className="mood-grid">
              {moodOptions.map((mood) => (
                <button
                  key={mood.label}
                  className={`mood-select-btn ${
                    selectedMood === mood.label ? 'mood-select-btn--active' : ''
                  }`}
                  onClick={() => handleMoodSelect(mood)}
                >
                  <span className="mood-select-emoji">{mood.emoji}</span>
                  <span className="mood-select-label">{mood.label}</span>
                </button>
              ))}
            </div>
          </section>

          {/* ===== 3. Behavior Stickers ===== */}
          <section className="sticker-section card-glass">
            <h3 className="section-label">🏷️ 今日行为贴纸</h3>
            <div className="sticker-grid">
              {behaviorStickers.map((sticker) => (
                <div className="sticker-item" key={sticker.label}>
                  <div
                    className="sticker-icon-ring"
                    style={{ background: stickerColorBg(sticker.color) }}
                  >
                    <span className="sticker-emoji">{sticker.emoji}</span>
                  </div>
                  <div className="sticker-info">
                    <span className="sticker-label">{sticker.label}</span>
                    <span
                      className="sticker-detail"
                      style={{ color: stickerColorVar(sticker.color) }}
                    >
                      {sticker.detail}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* ===== 4. AnXiaoNing Observation ===== */}
          <section className="observation-card card-glass">
            <div className="observation-header">
              <div className="observation-avatar-ring">
                <span className="observation-avatar-emoji">🌸</span>
              </div>
              <div className="observation-header-text">
                <span className="observation-title">安小宁的今日观察</span>
                <span className="observation-subtitle">基于今天的感知数据</span>
              </div>
            </div>
            <div className="observation-bubble">
              <p>{AI_OBSERVATION}</p>
            </div>
          </section>

          {/* ===== 5. Tomorrow Message ===== */}
          <section className="tomorrow-card card-glass">
            <h3 className="section-label">✉️ 给明天的自己</h3>
            <div className="tomorrow-input-row">
              <input
                className="tomorrow-input"
                type="text"
                placeholder="给明天的自己留一句温柔的话……"
                value={tomorrowMsg}
                onChange={(e) => {
                  setTomorrowMsg(e.target.value)
                  setTomorrowSaved(false)
                }}
                maxLength={60}
              />
              <button
                className="btn btn-primary tomorrow-save-btn"
                onClick={handleSaveTomorrow}
                disabled={!tomorrowMsg.trim()}
              >
                保存
              </button>
            </div>
            <div
              className={`tomorrow-confirmation ${tomorrowSaved ? 'tomorrow-confirmation--visible' : ''}`}
            >
              <span>💌 明天的你会收到这句话。</span>
            </div>
          </section>

          {/* ===== 6. Quick Nav ===== */}
          <div className="diary-quick-nav">
            <button
              className="quick-nav-btn card-glass"
              onClick={() => navigate('/chat')}
            >
              <span className="quick-nav-emoji">💬</span>
              <span className="quick-nav-label">去安心对话</span>
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

        {/* ── Right: History Sidebar ── */}
        <aside className="diary-sidebar">
          <div className="history-card card-glass">
            <div className="history-header">
              <span className="history-header-icon">📋</span>
              <span className="history-header-title">历史日记</span>
            </div>
            <div className="history-list">
              {historyEntries.map((entry) => (
                <div className="history-item" key={entry.id}>
                  <div className="history-item-date">{entry.date}</div>
                  <p className="history-item-preview">{entry.preview}</p>
                </div>
              ))}
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}

export default DiaryPage
