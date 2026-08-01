import { useEffect, useState } from 'react'
import { memoryApi } from '../../api/memoryApi'

export default function MemoriesPage() {
  const [memories, setMemories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [deletingId, setDeletingId] = useState(null)

  useEffect(() => {
    fetchMemories()
  }, [])

  const fetchMemories = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await memoryApi.list()
      setMemories(data)
    } catch (err) {
      setError('Failed to load memories')
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Forget this memory? The AI will no longer recall this fact.')) return
    setDeletingId(id)
    try {
      await memoryApi.delete(id)
      setMemories((prev) => prev.filter((m) => m.id !== id))
    } catch (err) {
      setError('Failed to delete memory')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-white">Memories</h1>
        <p className="text-sm text-slate-400 mt-1">
          Facts the AI has automatically learned about you from past conversations.
          These are recalled across all your chats, not just the one they came from.
        </p>
      </div>

      {error && (
        <p className="text-sm text-red-400 bg-red-950/50 border border-red-800 rounded-lg px-3 py-2">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-slate-500 text-sm">Loading...</p>
      ) : memories.length === 0 ? (
        <div className="bg-slate-800/50 border border-slate-700 rounded-2xl p-8 text-center">
          <p className="text-slate-400 text-sm">
            No memories yet. Chat naturally and the AI will remember useful facts about you
            over time — like your role, preferences, or ongoing projects.
          </p>
        </div>
      ) : (
        <div className="space-y-2">
          {memories.map((m) => (
            <div
              key={m.id}
              className="bg-slate-800/50 border border-slate-700 rounded-xl px-4 py-3 flex items-start justify-between gap-3"
            >
              <div>
                <p className="text-sm text-slate-200">{m.content}</p>
                <p className="text-xs text-slate-500 mt-1">
                  Learned {new Date(m.createdAt).toLocaleDateString()}
                </p>
              </div>
              <button
                onClick={() => handleDelete(m.id)}
                disabled={deletingId === m.id}
                className="shrink-0 text-xs rounded-lg bg-red-950 hover:bg-red-900 disabled:opacity-50 px-3 py-1.5 text-red-400"
              >
                {deletingId === m.id ? '...' : 'Forget'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}