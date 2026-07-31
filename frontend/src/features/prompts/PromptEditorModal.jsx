import { useEffect, useState } from 'react'
import Input from '../../components/Input'
import Button from '../../components/Button'

export default function PromptEditorModal({ initialPrompt, onSave, onClose, saving }) {
  const isEditing = Boolean(initialPrompt)

  const [form, setForm] = useState({
    name: initialPrompt?.name || '',
    description: initialPrompt?.description || '',
    content: initialPrompt?.content || '',
    isDefault: initialPrompt?.isDefault || false,
  })

  useEffect(() => {
    const handleEsc = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', handleEsc)
    return () => window.removeEventListener('keydown', handleEsc)
  }, [onClose])

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave(form)
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-semibold text-white">
          {isEditing ? 'Edit Prompt' : 'New Prompt'}
        </h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
            disabled={isEditing} // name/description fixed at creation to keep it simple; only content is versioned
          />
          <Input
            label="Description (optional)"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            disabled={isEditing}
          />
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-300">
              System Prompt Content
            </label>
            <textarea
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              required
              rows={6}
              className="w-full rounded-lg bg-slate-900 border border-slate-700 px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
              placeholder="You are a helpful assistant that..."
            />
          </div>

          {!isEditing && (
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={form.isDefault}
                onChange={(e) => setForm({ ...form, isDefault: e.target.checked })}
                className="rounded"
              />
              Set as my default prompt
            </label>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-lg bg-slate-700 hover:bg-slate-600 px-4 py-2 text-sm font-medium text-white"
            >
              Cancel
            </button>
            <div className="flex-1">
              <Button type="submit" loading={saving}>
                {isEditing ? 'Save (new version)' : 'Create Prompt'}
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  )
}