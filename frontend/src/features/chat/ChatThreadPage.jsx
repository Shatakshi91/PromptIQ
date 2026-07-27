import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { chatApi } from '../../api/chatApi'
import { useChatStore } from '../../store/chatStore'
import MessageBubble from './MessageBubble'

export default function ChatThreadPage() {
  const { conversationId } = useParams()

  const messages = useChatStore((s) => s.messages)
  const setMessages = useChatStore((s) => s.setMessages)
  const appendMessage = useChatStore((s) => s.appendMessage)
  const setActiveConversationId = useChatStore((s) => s.setActiveConversationId)
  const updateConversationInList = useChatStore((s) => s.updateConversationInList)
  const bumpConversationToTop = useChatStore((s) => s.bumpConversationToTop)
  const messagesLoading = useChatStore((s) => s.messagesLoading)
  const setMessagesLoading = useChatStore((s) => s.setMessagesLoading)

  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const bottomRef = useRef(null)

  useEffect(() => {
    if (!conversationId) return
    setActiveConversationId(conversationId)
    fetchMessages()
  }, [conversationId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const fetchMessages = async () => {
    setMessagesLoading(true)
    setError('')
    try {
      const { data } = await chatApi.listMessages(conversationId, 0, 100)
      setMessages(data.content)
    } catch (err) {
      setError('Failed to load messages')
    } finally {
      setMessagesLoading(false)
    }
  }

  const handleSend = async (e) => {
    e.preventDefault()
    const trimmed = input.trim()
    if (!trimmed || sending) return

    setSending(true)
    setError('')
    setInput('')

    try {
      const { data } = await chatApi.addMessage(conversationId, 'USER', trimmed)
      appendMessage(data)
      bumpConversationToTop(conversationId)

      // NOTE: Feature 5 will replace this stub with a real LLM call that
      // generates the assistant reply automatically. For now we persist
      // the user message only, so the data layer is proven end-to-end
      // before any AI call is introduced.
    } catch (err) {
      setError('Failed to send message')
      setInput(trimmed) // restore input on failure
    } finally {
      setSending(false)
    }
  }

  if (!conversationId) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-500">
        Select or start a new conversation
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col h-full">
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {messagesLoading ? (
          <p className="text-slate-500 text-sm text-center">Loading messages...</p>
        ) : messages.length === 0 ? (
          <p className="text-slate-500 text-sm text-center">
            No messages yet. Say something to get started.
          </p>
        ) : (
          messages.map((m) => <MessageBubble key={m.id} role={m.role} content={m.content} />)
        )}
        <div ref={bottomRef} />
      </div>

      {error && (
        <p className="mx-6 mb-2 text-xs text-red-400 bg-red-950/50 border border-red-800 rounded-lg px-3 py-2">
          {error}
        </p>
      )}

      <form onSubmit={handleSend} className="p-4 border-t border-slate-800 flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Type a message..."
          className="flex-1 rounded-lg bg-slate-800 border border-slate-700 px-4 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <button
          type="submit"
          disabled={sending || !input.trim()}
          className="rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-5 py-2.5 text-sm font-medium text-white"
        >
          Send
        </button>
      </form>
    </div>
  )
}