import { useEffect, useState } from 'react'
import { promptApi } from '../../api/promptApi'
import { usePromptStore } from '../../store/promptStore'
import PromptEditorModal from './PromptEditorModal'
import VersionHistoryPanel from './VersionHistoryPanel'

export default function PromptsPage() {
  const prompts = usePromptStore((s) => s.prompts)
  const setPrompts = usePromptStore((s) => s.setPrompts)
  const addPromptToTop = usePromptStore((s) => s.addPromptToTop)
  const updatePromptInList = usePromptStore((s) => s.updatePromptInList)
  const removePromptFromList = usePromptStore((s) => s.removePromptFromList)
  const markAsDefault = usePromptStore((s) => s.markAsDefault)
  const promptsLoading = usePromptStore((s) => s.promptsLoading)
  const setPromptsLoading = usePromptStore((s) => s.setPromptsLoading)

  const [showEditor, setShowEditor] = useState(false)
  const [editingPrompt, setEditingPrompt] = useState(null)
  const [historyPromptId, setHistoryPromptId] = useState(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchPrompts()
  }, [])

  const fetchPrompts = async () => {
    setPromptsLoading(true)
    try {
      const { data } = await promptApi.list(0, 50)
      setPrompts(data.content)
    } catch (err) {
      setError('Failed to load prompts')
    } finally {
      setPromptsLoading(false)
    }
  }

  const handleSave = async (form) => {
    setSaving(true)
    setError('')
    try {
      if (editingPrompt) {
        const { data } = await promptApi.update(editingPrompt.id, {
          name: form.name,
          description: form.description,
          content: form.content,
        })
        updatePromptInList(data)
      } else {
        const { data } = await promptApi.create(form)
        addPromptToTop(data)
        if (form.isDefault) markAsDefault(data.id)
      }
      setShowEditor(false)
      setEditingPrompt(null)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save prompt')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this prompt? Conversations using it will fall back to your default.')) return
    try {
      await promptApi.delete(id)
      removePromptFromList(id)
    } catch (err) {
      setError('Failed to delete prompt')
    }
  }

  const handleSetDefault = async (id) => {
    try {
      await promptApi.setDefault(id)
      markAsDefault(id)
    } catch (err) {
      setError('Failed to set default')
    }
  }

  return (
    <div className="max-w-3xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Prompt Templates</h1>
        <button
          onClick={() => {
            setEditingPrompt(null)
            setShowEditor(true)
          }}
          className="rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2 text-sm font-medium text-white"
        >
          + New Prompt
        </button>
      </div>

      {error && (
        <p className="text-sm text-red-400 bg-red-950/50 border border-red-800 rounded-lg px-3 py-2">
          {error}
        </p>
      )}

      {promptsLoading ? (
        <p className="text-slate-500 text-sm">Loading...</p>
      ) : prompts.length === 0 ? (
        <p className="text-slate-500 text-sm">No prompts yet. Create your first one.</p>
      ) : (
        <div className="space-y-3">
          {prompts.map((p) => (
            <div
              key={p.id}
              className="bg-slate-800/50 border border-slate-700 rounded-2xl p-4 space-y-2"
            >
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-white">{p.name}</h3>
                    {p.isDefault && (
                      <span className="text-xs bg-indigo-900 text-indigo-300 px-2 py-0.5 rounded-full">
                        Default
                      </span>
                    )}
                    <span className="text-xs text-slate-500">v{p.currentVersion}</span>
                  </div>
                  {p.description && (
                    <p className="text-sm text-slate-400 mt-0.5">{p.description}</p>
                  )}
                </div>
              </div>

              <p className="text-sm text-slate-300 line-clamp-2">{p.content}</p>

              <div className="flex gap-2 pt-1 text-xs">
                <button
                  onClick={() => {
                    setEditingPrompt(p)
                    setShowEditor(true)
                  }}
                  className="rounded-lg bg-slate-700 hover:bg-slate-600 px-3 py-1.5 text-white"
                >
                  Edit
                </button>
                <button
                  onClick={() => setHistoryPromptId(p.id)}
                  className="rounded-lg bg-slate-700 hover:bg-slate-600 px-3 py-1.5 text-white"
                >
                  History
                </button>
                {!p.isDefault && (
                  <button
                    onClick={() => handleSetDefault(p.id)}
                    className="rounded-lg bg-slate-700 hover:bg-slate-600 px-3 py-1.5 text-white"
                  >
                    Set Default
                  </button>
                )}
                <button
                  onClick={() => handleDelete(p.id)}
                  className="rounded-lg bg-red-950 hover:bg-red-900 px-3 py-1.5 text-red-400 ml-auto"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showEditor && (
        <PromptEditorModal
          initialPrompt={editingPrompt}
          saving={saving}
          onSave={handleSave}
          onClose={() => {
            setShowEditor(false)
            setEditingPrompt(null)
          }}
        />
      )}

      {historyPromptId && (
        <VersionHistoryPanel
          promptId={historyPromptId}
          onClose={() => setHistoryPromptId(null)}
        />
      )}
    </div>
  )
}