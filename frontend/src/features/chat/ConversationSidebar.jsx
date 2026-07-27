import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { chatApi } from '../../api/chatApi'
import { useChatStore } from '../../store/chatStore'

export default function ConversationSidebar() {
  const navigate = useNavigate()
  const { conversationId } = useParams()

  const conversations = useChatStore((s) => s.conversations)
  const setConversations = useChatStore((s) => s.setConversations)
  const addConversationToTop = useChatStore((s) => s.addConversationToTop)
  const removeConversationFromList = useChatStore((s) => s.removeConversationFromList)
  const conversationsLoading = useChatStore((s) => s.conversationsLoading)
  const setConversationsLoading = useChatStore((s) => s.setConversationsLoading)

  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchConversations()
  }, [])

  const fetchConversations = async () => {
    setConversationsLoading(true)
    setError('')
    try {
      const { data } = await chatApi.listConversations(0, 50)
      setConversations(data.content)
    } catch (err) {
      setError('Failed to load conversations')
    } finally {
      setConversationsLoading(false)
    }
  }

  const handleNewConversation = async () => {
    setCreating(true)
    try {
      const { data } = await chatApi.createConversation(null)
      addConversationToTop(data)
      navigate(`/chat/${data.id}`)
    } catch (err) {
      setError('Failed to create conversation')
    } finally {
      setCreating(false)
    }
  }

  const handleDelete = async (e, id) => {
    e.stopPropagation()
    if (!confirm('Delete this conversation?')) return
    try {
      await chatApi.deleteConversation(id)
      removeConversationFromList(id)
      if (conversationId === id) {
        navigate('/chat')
      }
    } catch (err) {
      setError('Failed to delete conversation')
    }
  }

  return (
    <aside className="w-72 bg-slate-950 border-r border-slate-800 flex flex-col h-full">
      <div className="p-3">
        <button
          onClick={handleNewConversation}
          disabled={creating}
          className="w-full rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-3 py-2 text-sm font-medium text-white"
        >
          + New Chat
        </button>
      </div>

      {error && (
        <p className="mx-3 mb-2 text-xs text-red-400 bg-red-950/50 border border-red-800 rounded-lg px-2 py-1">
          {error}
        </p>
      )}

      <div className="flex-1 overflow-y-auto px-2 space-y-1">
        {conversationsLoading ? (
          <p className="text-slate-500 text-sm px-2 py-4">Loading...</p>
        ) : conversations.length === 0 ? (
          <p className="text-slate-500 text-sm px-2 py-4">No conversations yet</p>
        ) : (
          conversations.map((c) => (
            <div
              key={c.id}
              onClick={() => navigate(`/chat/${c.id}`)}
              className={`group flex items-center justify-between rounded-lg px-3 py-2 text-sm cursor-pointer ${
                conversationId === c.id
                  ? 'bg-slate-800 text-white'
                  : 'text-slate-400 hover:bg-slate-900 hover:text-white'
              }`}
            >
              <span className="truncate">{c.title}</span>
              <button
                onClick={(e) => handleDelete(e, c.id)}
                className="opacity-0 group-hover:opacity-100 text-slate-500 hover:text-red-400 ml-2 shrink-0"
                title="Delete"
              >
                ✕
              </button>
            </div>
          ))
        )}
      </div>
    </aside>
  )
}