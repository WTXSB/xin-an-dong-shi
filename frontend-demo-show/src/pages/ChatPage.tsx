import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import './ChatPage.css'

// ── Types ──────────────────────────────────────────────
interface Message {
  id: number
  sender: 'user' | 'bot'
  text: string
  time: string
}

interface ChatHistory {
  id: number
  time: string
  title: string
  summary: string
}

interface MockReply {
  text: string
  showBreathing?: boolean
}

// ── Mock Data ──────────────────────────────────────────
const chatHistories: ChatHistory[] = [
  {
    id: 1,
    time: '今天 14:20',
    title: '最近有点焦虑',
    summary: '聊了工作压力和放松方法，安小宁推荐了呼吸练习...',
  },
  {
    id: 2,
    time: '今天 09:15',
    title: '昨晚睡得不太好',
    summary: '尝试了睡前身体扫描，记录了一些入睡前的感受...',
  },
  {
    id: 3,
    time: '昨天',
    title: '考试前的紧张',
    summary: '练习了 4-7-8 呼吸法，紧张感从 7 分降到了 3 分...',
  },
  {
    id: 4,
    time: '6月9日',
    title: '第一次和安小宁聊天',
    summary: '初次见面，互相认识，聊了为什么会来到这里...',
  },
]

const mockReplies: Record<string, MockReply> = {
  '我有点焦虑': {
    text: '我听见了。焦虑不是你的错，它像一阵突然变大的风。我们先不急着把它赶走，可以先一起做三次慢慢的呼吸。',
  },
  '我睡不着': {
    text: '睡不着的时候，身体可能还在替你处理白天的事情。先不用责怪自己，我们可以把注意力轻轻放回呼吸和身体。',
  },
  '带我做呼吸': {
    text: '好，我们一起做 4-4-6 呼吸。吸气 4 秒，停留 4 秒，呼气 6 秒。你只需要跟着我慢慢来。',
    showBreathing: true,
  },
  '帮我分析今天': {
    text: '从今天的记录看，你有几次紧张的小动作，但也完成了一次温柔感知和一段正念练习。你不是没有进步，你是在慢慢学会看见自己。',
  },
}

const GENERIC_REPLY = '谢谢你愿意说出来。能把感受说出口，本身就是一次很重要的觉察。我们可以慢慢来。'

const WELCOME_MESSAGE: Message = {
  id: 0,
  sender: 'bot',
  text: '嗨，我是安小宁。今天想从哪里开始说起都可以，我会慢慢听。',
  time: '',
}

// ── Helpers ────────────────────────────────────────────
const now = () =>
  new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

// ── Quick Actions Config ───────────────────────────────
const quickActions = [
  { emoji: '😰', label: '我有点焦虑' },
  { emoji: '😴', label: '我睡不着' },
  { emoji: '🧘', label: '带我做呼吸' },
  { emoji: '📝', label: '帮我分析今天' },
]

// ── Component ──────────────────────────────────────────
function ChatPage() {
  const navigate = useNavigate()

  // State
  const [messages, setMessages] = useState<Message[]>([WELCOME_MESSAGE])
  const [input, setInput] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [streamingText, setStreamingText] = useState('')
  const [streamingMsgId, setStreamingMsgId] = useState<number | null>(null)
  const [showTyping, setShowTyping] = useState(false)
  const [showBreathing, setShowBreathing] = useState(false)

  // Refs
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const streamTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // ── Auto-scroll ────────────────────────────────────
  const scrollToBottom = useCallback(() => {
    // Use requestAnimationFrame so the DOM has painted the new message
    requestAnimationFrame(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    })
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, streamingText, showTyping, showBreathing, scrollToBottom])

  // Cleanup stream timer on unmount
  useEffect(() => {
    return () => {
      if (streamTimerRef.current) clearInterval(streamTimerRef.current)
    }
  }, [])

  // ── Streaming Engine ───────────────────────────────
  const startStream = useCallback((fullText: string, msgId: number) => {
    // Show typing indicator first
    setShowTyping(true)
    setStreamingMsgId(msgId)

    const typingDelay = 800 + Math.random() * 600

    const typingTimeout = setTimeout(() => {
      setShowTyping(false)
      setIsStreaming(true)
      setStreamingText('')

      const chars = [...fullText]
      let index = 0

      streamTimerRef.current = setInterval(() => {
        if (index < chars.length) {
          // Variable chunk size for natural cadence
          const chunkSize = Math.floor(Math.random() * 3) + 1
          const chunk = chars.slice(index, index + chunkSize).join('')
          index += chunkSize
          setStreamingText((prev) => prev + chunk)
        } else {
          // Streaming complete
          if (streamTimerRef.current) {
            clearInterval(streamTimerRef.current)
            streamTimerRef.current = null
          }
          setIsStreaming(false)
          setStreamingText('')
          setStreamingMsgId(null)
          // Commit the full text into the message
          setMessages((prev) =>
            prev.map((m) => (m.id === msgId ? { ...m, text: fullText } : m)),
          )
        }
      }, 40 + Math.random() * 35) // 40-75ms per chunk → natural pace
    }, typingDelay)

    return () => clearTimeout(typingTimeout)
  }, [])

  // ── Send Message ───────────────────────────────────
  const handleSend = useCallback(
    (text?: string) => {
      const messageText = (text || input.trim())
      if (!messageText || isStreaming) return

      // Clear previous breathing card
      setShowBreathing(false)

      // Add user message
      const userMsg: Message = {
        id: Date.now(),
        sender: 'user',
        text: messageText,
        time: now(),
      }
      setMessages((prev) => [...prev, userMsg])
      setInput('')

      // Determine reply
      const matched = mockReplies[messageText]
      const replyText = matched?.text ?? GENERIC_REPLY
      const shouldShowBreathing = matched?.showBreathing ?? false

      // Schedule bot reply with a short "thinking" delay
      const thinkingDelay = 400 + Math.random() * 500
      setTimeout(() => {
        const botMsg: Message = {
          id: Date.now() + 1,
          sender: 'bot',
          text: '', // Empty initially; filled by stream
          time: now(),
        }
        setMessages((prev) => [...prev, botMsg])

        if (shouldShowBreathing) {
          setShowBreathing(true)
        }

        startStream(replyText, botMsg.id)
      }, thinkingDelay)
    },
    [input, isStreaming, startStream],
  )

  // ── Quick Action Handler ───────────────────────────
  const handleQuickAction = (label: string) => {
    if (isStreaming) return
    handleSend(label)
  }

  // ── Render ─────────────────────────────────────────
  // Determine what to display in the currently-streaming bubble
  const renderBubbleContent = (msg: Message) => {
    const isLastBotMsg =
      msg.sender === 'bot' &&
      msg.id === messages[messages.length - 1]?.id

    // Still in "typing" phase
    if (isLastBotMsg && showTyping && streamingMsgId === msg.id) {
      return (
        <span className="typing-indicator">
          安小宁正在认真听
          <span className="typing-dots">
            <span>.</span>
            <span>.</span>
            <span>.</span>
          </span>
        </span>
      )
    }

    // Actively streaming characters
    if (isLastBotMsg && isStreaming && streamingMsgId === msg.id) {
      return (
        <>
          {streamingText}
          <span className="streaming-cursor">|</span>
        </>
      )
    }

    // Normal completed message
    return msg.text
  }

  return (
    <div className="chat-page">
      {/* ===== Page Header ===== */}
      <header className="chat-page-header">
        <h1 className="chat-page-title">💬 安心对话</h1>
        <p className="chat-page-subtitle">
          安小宁会慢慢听你说，不催促，也不评判
        </p>
      </header>

      {/* ===== Two-Column Layout ===== */}
      <div className="chat-layout">
        {/* ── Left: Chat History Sidebar ── */}
        <aside className="chat-sidebar card-glass">
          <div className="sidebar-header">
            <span className="sidebar-header-icon">📋</span>
            <span className="sidebar-header-title">对话记录</span>
          </div>
          <div className="history-list">
            {chatHistories.map((h) => (
              <div className="history-item" key={h.id}>
                <div className="history-time">{h.time}</div>
                <div className="history-title">{h.title}</div>
                <div className="history-summary">{h.summary}</div>
              </div>
            ))}
          </div>
          <div className="sidebar-footer">
            <span className="sidebar-footer-icon">🔒</span>
            <span className="sidebar-footer-text">
              对话仅保存在本地，安小宁为你守护隐私
            </span>
          </div>
        </aside>

        {/* ── Right: Main Chat Area ── */}
        <div className="chat-main card-glass">
          {/* Chat Header */}
          <div className="chat-header">
            <div className="chat-header-left">
              <div className="chat-avatar-ring">
                <span className="chat-avatar-emoji">🌸</span>
              </div>
              <div className="chat-header-info">
                <span className="chat-header-name">安小宁</span>
                <span className="chat-header-status">
                  <span className="status-dot" />
                  正在陪伴中
                </span>
              </div>
            </div>
          </div>

          {/* Messages Area */}
          <div className="chat-messages">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`message-row ${msg.sender} ${
                  msg.id === WELCOME_MESSAGE.id ? 'message-row--welcome' : ''
                }`}
              >
                {/* Bot avatar (left side) */}
                {msg.sender === 'bot' && (
                  <div className="message-avatar">
                    <span className="message-avatar-emoji">🌸</span>
                  </div>
                )}

                {/* Bubble */}
                <div className="message-bubble-wrapper">
                  <div className={`message-bubble ${msg.sender}`}>
                    {renderBubbleContent(msg)}
                  </div>
                  {msg.time && (
                    <div className="message-time">{msg.time}</div>
                  )}
                </div>

                {/* User avatar (right side) */}
                {msg.sender === 'user' && (
                  <div className="message-avatar user-avatar">
                    <span className="message-avatar-emoji">😌</span>
                  </div>
                )}
              </div>
            ))}

            {/* Breathing Exercise Card */}
            {showBreathing && (
              <div className="breathing-card">
                <div className="breathing-card-header">
                  <span className="breathing-card-icon">🧘</span>
                  <div className="breathing-card-title-group">
                    <div className="breathing-card-title">4-4-6 呼吸法</div>
                    <div className="breathing-card-subtitle">
                      跟着安小宁的节奏，慢慢来
                    </div>
                  </div>
                </div>
                <div className="breathing-steps">
                  <div className="breathing-step">
                    <div className="step-circle">1</div>
                    <div className="step-label">吸气</div>
                    <div className="step-duration">4 秒</div>
                  </div>
                  <div className="breathing-step-arrow">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <path
                        d="M5 12h14M13 5l7 7-7 7"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                  <div className="breathing-step">
                    <div className="step-circle">2</div>
                    <div className="step-label">停留</div>
                    <div className="step-duration">4 秒</div>
                  </div>
                  <div className="breathing-step-arrow">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <path
                        d="M5 12h14M13 5l7 7-7 7"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                  <div className="breathing-step">
                    <div className="step-circle">3</div>
                    <div className="step-label">呼气</div>
                    <div className="step-duration">6 秒</div>
                  </div>
                </div>
                <button
                  className="btn btn-primary breathing-start-btn"
                  onClick={() => navigate('/mindfulness')}
                >
                  🧘 开始练习
                </button>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Quick Action Chips */}
          <div className="quick-actions-row">
            {quickActions.map((action) => (
              <button
                key={action.label}
                className="quick-action-chip"
                onClick={() => handleQuickAction(action.label)}
                disabled={isStreaming}
              >
                <span className="qa-chip-emoji">{action.emoji}</span>
                <span className="qa-chip-label">{action.label}</span>
              </button>
            ))}
          </div>

          {/* Input Bar */}
          <form
            className="chat-input-bar"
            onSubmit={(e) => {
              e.preventDefault()
              handleSend()
            }}
          >
            <input
              ref={inputRef}
              className="chat-input"
              type="text"
              placeholder={
                isStreaming ? '安小宁正在回复...' : '说说你的想法...'
              }
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={isStreaming}
            />
            <button
              type="submit"
              className="btn btn-primary chat-send-btn"
              disabled={!input.trim() || isStreaming}
            >
              发送
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}

export default ChatPage
