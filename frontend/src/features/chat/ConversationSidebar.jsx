import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { chatApi } from '../../api/chatApi'
import { useChatStore } from '../../store/chatStore'
import { useAuthStore } from '../../store/authStore'
import { Link } from 'react-router-dom'
import { authApi } from '../../api/authApi'
import {
  Plus,
  Sparkles,
  Trash2,
  Loader2,
  AlertCircle,
  MessageSquare,
  ChevronLeft,
  MoreHorizontal,
  Pencil
} from 'lucide-react'

export default function ConversationSidebar({ isOpen, onClose }) {
  const navigate = useNavigate()
  const { conversationId } = useParams()
  const user = useAuthStore((s) => s.user)
  const logoutStore = useAuthStore((s) => s.logout)

  const conversations = useChatStore((s) => s.conversations)
  const setConversations = useChatStore((s) => s.setConversations)
  const addConversationToTop = useChatStore((s) => s.addConversationToTop)
  const updateConversationInList = useChatStore((s) => s.updateConversationInList)
  const removeConversationFromList = useChatStore((s) => s.removeConversationFromList)
  const conversationsLoading = useChatStore((s) => s.conversationsLoading)
  const setConversationsLoading = useChatStore((s) => s.setConversationsLoading)

  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')
  const [dropdownOpenId, setDropdownOpenId] = useState(null)
  const [renamingId, setRenamingId] = useState(null)
  const [renameTitle, setRenameTitle] = useState('')

  useEffect(() => {
    fetchConversations()
  }, [])

  const fetchConversations = async () => {
    setConversationsLoading(true)
    setError('')
    try {
      const { data } = await chatApi.listConversations(0, 50)
      setConversations(data.content)
    } catch {
      setError('Failed to load history')
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
      if (onClose) onClose()
    } catch {
      setError('Failed to create chat')
    } finally {
      setCreating(false)
    }
  }

  const submitRename = async (id) => {
    if (!renameTitle.trim()) {
      setRenamingId(null)
      return
    }
    try {
      await chatApi.renameConversation(id, renameTitle)
      updateConversationInList({ id, title: renameTitle })
    } catch {
      setError('Failed to rename')
    } finally {
      setRenamingId(null)
    }
  }

  const handleDelete = async (e, id) => {
    e.stopPropagation()
    if (!confirm('Delete this conversation?')) return
    try {
      await chatApi.deleteConversation(id)
      removeConversationFromList(id)
      if (conversationId === id) navigate('/chat')
    } catch {
      setError('Failed to delete')
    }
  }

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } finally {
      logoutStore()
      navigate('/login')
    }
  }

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/20 z-40 lg:hidden backdrop-blur-sm"
          onClick={onClose}
        />
      )}

      <aside
        className={`fixed lg:static inset-y-0 left-0 z-40 flex flex-col h-full bg-[#f5f5f7]/95 backdrop-blur-xl border-r border-gray-200/60 transition-transform duration-300 ease-out overflow-hidden w-64 shrink-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="w-64 flex flex-col h-full">
          {/* Mobile Close Button */}
          <div className="lg:hidden flex justify-end px-3 pt-3">
            <button
              onClick={onClose}
              className="w-7 h-7 rounded-lg flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-200/60 transition-all cursor-pointer"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
          </div>

          {/* New Chat Button */}
          <div className="px-3 py-3">
            <button
              onClick={handleNewConversation}
              disabled={creating}
              className="w-full flex items-center justify-center gap-2 rounded-xl bg-black hover:bg-gray-800 disabled:opacity-60 px-4 py-2.5 text-xs font-semibold text-white transition-all shadow-sm hover:shadow-md cursor-pointer"
            >
              {creating ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <Plus className="w-3.5 h-3.5" />
              )}
              New Chat
            </button>
          </div>

          {error && (
            <div className="mx-3 mb-2 flex items-center gap-1.5 text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl p-2.5">
              <AlertCircle className="w-3.5 h-3.5 shrink-0" />
              {error}
            </div>
          )}

          {/* Conversation list */}
          <div className="flex-1 overflow-y-auto px-2 py-1">
            {conversationsLoading ? (
              <div className="flex items-center justify-center gap-2 py-8 text-xs text-gray-400">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Loading...
              </div>
            ) : conversations.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 gap-2 text-gray-400">
                <MessageSquare className="w-5 h-5" />
                <p className="text-xs">No conversations yet</p>
              </div>
            ) : (
              <div className="space-y-0.5">
                <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-widest px-3 py-2">Recent</p>
                {conversations.map((c) => {
                  const isSelected = conversationId === c.id
                  return (
                    <div
                      key={c.id}
                      onClick={() => {
                        navigate(`/chat/${c.id}`)
                        if (onClose) onClose()
                      }}
                      className={`group relative flex items-center justify-between rounded-xl px-3 py-2.5 text-xs cursor-pointer transition-all ${
                        isSelected
                          ? 'bg-white text-gray-900 font-medium shadow-sm border border-gray-200/60'
                          : 'text-gray-600 hover:bg-white/70 hover:text-gray-900'
                      }`}
                    >
                      {renamingId === c.id ? (
                        <input
                          autoFocus
                          value={renameTitle}
                          onChange={(e) => setRenameTitle(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') submitRename(c.id)
                            if (e.key === 'Escape') setRenamingId(null)
                          }}
                          onBlur={() => submitRename(c.id)}
                          onClick={(e) => e.stopPropagation()}
                          className="flex-1 bg-transparent border-b border-gray-400 outline-none text-gray-900"
                        />
                      ) : (
                        <span className="truncate flex-1">{c.title || 'New Conversation'}</span>
                      )}

                      {!renamingId && (
                        <div className="relative flex items-center ml-1 shrink-0">
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              setDropdownOpenId(dropdownOpenId === c.id ? null : c.id)
                            }}
                            className={`opacity-0 group-hover:opacity-100 p-1 rounded-md hover:bg-gray-200/60 text-gray-400 transition-all ${
                              dropdownOpenId === c.id ? 'opacity-100 bg-gray-200/60' : ''
                            }`}
                          >
                            <MoreHorizontal className="w-4 h-4" />
                          </button>

                          {dropdownOpenId === c.id && (
                            <>
                              <div
                                className="fixed inset-0 z-40"
                                onClick={(e) => {
                                  e.stopPropagation()
                                  setDropdownOpenId(null)
                                }}
                              />
                              <div className="absolute right-0 top-full mt-1 z-50 w-36 bg-white rounded-xl shadow-lg border border-gray-100 overflow-hidden py-1">
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    setDropdownOpenId(null)
                                    setRenameTitle(c.title || '')
                                    setRenamingId(c.id)
                                  }}
                                  className="w-full flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 transition-colors text-left cursor-pointer"
                                >
                                  <Pencil className="w-3.5 h-3.5 text-gray-400" />
                                  Rename
                                </button>
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    setDropdownOpenId(null)
                                    handleDelete(e, c.id)
                                  }}
                                  className="w-full flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors text-left cursor-pointer"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                  Delete
                                </button>
                              </div>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
          </div>

        </div>
      </aside>
    </>
  )
}