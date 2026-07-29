import { useEffect, useRef, useState } from 'react'
import { useParams, useOutletContext, useNavigate } from 'react-router-dom'
import { chatApi } from '../../api/chatApi'
import { streamChatMessage } from '../../api/streamChat'
import { useChatStore } from '../../store/chatStore'
import { useAuthStore } from '../../store/authStore'
import MessageBubble from './MessageBubble'
import TypingIndicator from './TypingIndicator'
import {
  Sparkles,
  ArrowUp,
  PanelLeft,
  AlertCircle,
  Plus,
  Search,
  Paperclip,
  ChevronDown,
  ListTodo,
  Mail,
  AlignLeft,
  Code2,
  User,
} from 'lucide-react'

const EXAMPLE_PROMPTS = [
  {
    title: 'Write a to-do list for a personal project',
    icon: ListTodo,
  },
  {
    title: 'Generate an email to reply to a job offer',
    icon: Mail,
  },
  {
    title: 'Summarize this article in one paragraph',
    icon: AlignLeft,
  },
  {
    title: 'How does AI work in a technical capacity',
    icon: Code2,
  },
]

export default function ChatThreadPage() {
  const { conversationId } = useParams()
  const { toggleSidebar, isSidebarOpen } = useOutletContext() || {}
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)

  const messages = useChatStore((s) => s.messages)
  const setMessages = useChatStore((s) => s.setMessages)
  const appendMessage = useChatStore((s) => s.appendMessage)
  const setActiveConversationId = useChatStore((s) => s.setActiveConversationId)
  const bumpConversationToTop = useChatStore((s) => s.bumpConversationToTop)
  const conversations = useChatStore((s) => s.conversations)
  const addConversationToTop = useChatStore((s) => s.addConversationToTop)
  const messagesLoading = useChatStore((s) => s.messagesLoading)
  const setMessagesLoading = useChatStore((s) => s.setMessagesLoading)

  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [streamingText, setStreamingText] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [error, setError] = useState('')

  const bottomRef = useRef(null)
  const textareaRef = useRef(null)

  const firstName = user?.displayName
    ? user.displayName.split(' ')[0]
    : user?.email
    ? user.email.split('@')[0]
    : 'there'

  // Get greeting based on time of day
  const getGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Good Morning'
    if (hour < 17) return 'Good Afternoon'
    return 'Good Evening'
  }

  const activeConversation = conversations?.find((c) => c.id === conversationId)

  useEffect(() => {
    if (!conversationId) return
    setActiveConversationId(conversationId)
    fetchMessages()
    setStreamingText('')
    setIsStreaming(false)
  }, [conversationId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamingText, isStreaming])

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 200)}px`
    }
  }, [input])

  const fetchMessages = async () => {
    setMessagesLoading(true)
    setError('')
    try {
      const { data } = await chatApi.listMessages(conversationId, 0, 100)
      setMessages(data.content)
    } catch {
      setError('Failed to load messages')
    } finally {
      setMessagesLoading(false)
    }
  }

  const handleNewConversation = async () => {
    try {
      const { data } = await chatApi.createConversation(null)
      addConversationToTop(data)
      navigate(`/chat/${data.id}`)
    } catch {
      setError('Failed to create new chat')
    }
  }

  const handleSend = async (e, promptText) => {
    if (e) e.preventDefault()
    const trimmed = (promptText || input).trim()
    if (!trimmed || sending) return

    let currentConvId = conversationId

    if (!currentConvId) {
      try {
        setSending(true)
        const { data } = await chatApi.createConversation(null)
        addConversationToTop(data)
        currentConvId = data.id
        navigate(`/chat/${data.id}`)
      } catch {
        setError('Failed to start a new thread')
        setSending(false)
        return
      }
    }

    setSending(true)
    setError('')
    setInput('')
    if (textareaRef.current) textareaRef.current.style.height = 'auto'

    const optimisticUserMessage = {
      id: `temp-user-${Date.now()}`,
      conversationId: currentConvId,
      role: 'USER',
      content: trimmed,
      createdAt: new Date().toISOString(),
    }
    appendMessage(optimisticUserMessage)
    bumpConversationToTop(currentConvId)

    setIsStreaming(true)
    setStreamingText('')

    let accumulated = ''

    await streamChatMessage(currentConvId, trimmed, {
      onToken: (token) => {
        accumulated += token
        setStreamingText(accumulated)
      },
      onDone: () => {
        appendMessage({
          id: `temp-assistant-${Date.now()}`,
          conversationId: currentConvId,
          role: 'ASSISTANT',
          content: accumulated,
          createdAt: new Date().toISOString(),
        })
        bumpConversationToTop(currentConvId)
        setStreamingText('')
        setIsStreaming(false)
        setSending(false)
      },
      onError: (message) => {
        setError(message)
        setInput(trimmed)
        setStreamingText('')
        setIsStreaming(false)
        setSending(false)
      },
    })
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="flex-1 flex flex-col h-full bg-white overflow-hidden">
      {/* Top Header — Apple-style */}
      <header className="px-5 py-3 border-b border-gray-100/80 flex items-center justify-between shrink-0 bg-white/80 backdrop-blur-xl">
        {/* Left: toggle sidebar + thread name */}
        <div className="flex items-center gap-3">
          <button
            onClick={toggleSidebar}
            className={`p-2 rounded-xl transition-all cursor-pointer lg:hidden ${
              isSidebarOpen
                ? 'bg-gray-100 text-gray-900'
                : 'text-gray-400 hover:text-gray-700 hover:bg-gray-100/80'
            }`}
            title="Toggle history"
          >
            <PanelLeft className="w-4 h-4" />
          </button>

          {conversationId && (
            <button className="flex items-center gap-1.5 text-sm font-medium text-gray-800 hover:text-gray-900 transition-colors cursor-pointer group">
              <Sparkles className="w-3.5 h-3.5 text-gray-500" />
              <span className="max-w-[200px] truncate">
                {activeConversation?.title || 'PromptIQ'}
              </span>
              <ChevronDown className="w-3.5 h-3.5 text-gray-400 group-hover:text-gray-600 transition-colors" />
            </button>
          )}

        </div>
      </header>

      {/* Body */}
      {!conversationId ? (
        /* ── Welcome / Empty State ── */
        <div className="flex-1 overflow-y-auto flex flex-col items-center justify-center px-4 py-10 bg-white">
          <div className="w-full max-w-2xl mx-auto space-y-10">

            {/* AI Orb */}
            <div className="flex justify-center">
              <div className="ai-orb">
                <div className="ai-orb-inner" />
              </div>
            </div>

            {/* Greeting */}
            <div className="text-center space-y-1.5">
              <h1 className="text-3xl font-semibold text-gray-900 tracking-tight">
                {getGreeting()},{' '}
                <span className="capitalize">{firstName}</span>
              </h1>
              <p className="text-2xl font-medium">
                <span className="text-gray-900">What's on </span>
                <span className="text-gray-500">
                  your mind?
                </span>
              </p>
            </div>

            {/* Input card */}
            <div className="bg-white border border-gray-200 rounded-[28px] shadow-sm focus-within:border-gray-300 focus-within:shadow-md focus-within:shadow-gray-100/60 transition-all duration-200 max-w-xl mx-auto flex items-end gap-2 pl-4 pr-2 py-2">
              <Sparkles className="w-4 h-4 text-gray-400 shrink-0 mb-2" />
              <textarea
                ref={textareaRef}
                rows={1}
                value={input}
                onChange={(e) => {
                  setInput(e.target.value);
                  e.target.style.height = 'auto';
                  e.target.style.height = e.target.scrollHeight + 'px';
                }}
                onKeyDown={handleKeyDown}
                placeholder="Ask AI a question or make a request..."
                disabled={sending}
                className="flex-1 bg-transparent text-sm text-gray-900 placeholder-gray-400 focus:outline-none resize-none max-h-32 font-sans py-1.5 leading-relaxed"
                style={{ minHeight: '32px' }}
              />
              <button
                onClick={handleSend}
                disabled={sending || !input.trim()}
                className="w-8 h-8 rounded-full bg-gray-900 hover:bg-gray-800 disabled:opacity-30 text-white flex items-center justify-center transition-all cursor-pointer shrink-0 shadow-sm mb-0.5"
                title="Send"
              >
                <ArrowUp className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Example prompts */}
            <div className="space-y-3">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest text-center">
                Get started with an example below
              </p>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                {EXAMPLE_PROMPTS.map((prompt, i) => (
                  <button
                    key={i}
                    onClick={() => handleSend(null, prompt.title)}
                    disabled={sending}
                    className="group flex flex-col justify-between bg-gray-50/80 hover:bg-white border border-gray-200/80 hover:border-gray-300 hover:shadow-sm rounded-2xl p-4 text-left transition-all duration-200 cursor-pointer disabled:opacity-50"
                  >
                    <p className="text-xs text-gray-700 group-hover:text-gray-900 leading-relaxed font-medium line-clamp-2 mb-4">
                      {prompt.title}
                    </p>
                    <div className="w-7 h-7 rounded-xl bg-white border border-gray-200 flex items-center justify-center shadow-xs group-hover:border-gray-300 group-hover:bg-gray-100 transition-all">
                      <prompt.icon className="w-3.5 h-3.5 text-gray-500 group-hover:text-gray-800 transition-colors" />
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* ── Active Chat ── */
        <div className="flex-1 flex flex-col h-full min-w-0">
          <div className="flex-1 overflow-y-auto px-4 sm:px-6 py-6">
            <div className="max-w-2xl mx-auto space-y-1">
              {messagesLoading ? (
                <div className="flex items-center justify-center py-16 text-xs text-gray-400">
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:-0.3s]" />
                    <div className="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:-0.15s]" />
                    <div className="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce" />
                  </div>
                </div>
              ) : messages.length === 0 && !isStreaming ? (
                <div className="text-center py-16 text-xs text-gray-400">
                  No messages yet. Start the conversation!
                </div>
              ) : (
                messages.map((m) => (
                  <MessageBubble key={m.id} role={m.role} content={m.content} />
                ))
              )}

              {isStreaming && streamingText && (
                <MessageBubble role="ASSISTANT" content={streamingText} />
              )}
              {isStreaming && !streamingText && <TypingIndicator />}

              <div ref={bottomRef} />
            </div>
          </div>

          {error && (
            <div className="max-w-2xl mx-auto px-4 mb-2 w-full">
              <div className="flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl p-3">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{error}</span>
              </div>
            </div>
          )}

          {/* Chat input */}
          <div className="p-4 sm:p-5 shrink-0 bg-white border-t border-gray-100/80">
            <div className="max-w-xl mx-auto">
              <div className="bg-white border border-gray-200 rounded-[28px] focus-within:border-gray-300 focus-within:shadow-md focus-within:shadow-gray-100/50 transition-all duration-200 shadow-sm flex items-end gap-2 pl-4 pr-2 py-2">
                <textarea
                  ref={textareaRef}
                  rows={1}
                  value={input}
                  onChange={(e) => {
                    setInput(e.target.value);
                    e.target.style.height = 'auto';
                    e.target.style.height = e.target.scrollHeight + 'px';
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="Message PromptIQ..."
                  disabled={sending}
                  className="flex-1 bg-transparent text-sm text-gray-900 placeholder-gray-400 focus:outline-none resize-none max-h-32 disabled:opacity-60 font-sans py-1.5 leading-relaxed"
                  style={{ minHeight: '32px' }}
                />
                <button
                  onClick={handleSend}
                  disabled={sending || !input.trim()}
                  className="w-8 h-8 rounded-full bg-gray-900 hover:bg-gray-800 disabled:opacity-30 text-white flex items-center justify-center transition-all cursor-pointer shrink-0 shadow-sm mb-0.5"
                >
                  <ArrowUp className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}