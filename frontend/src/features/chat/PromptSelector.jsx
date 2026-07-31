import { useEffect, useState } from 'react'
import { usePromptStore } from '../../store/promptStore'
import { promptApi } from '../../api/promptApi'
import { chatApi } from '../../api/chatApi'

export default function PromptSelector({ conversationId, activePromptId, onChanged }) {
  const prompts = usePromptStore((s) => s.prompts)
  const setPrompts = usePromptStore((s) => s.setPrompts)
  const [updating, setUpdating] = useState(false)

  useEffect(() => {
    // Lazily load prompts if the store is empty (user hasn't visited /prompts yet this session)
    if (prompts.length === 0) {
      promptApi.list(0, 50).then(({ data }) => setPrompts(data.content))
    }
  }, [])

  const handleChange = async (e) => {
    const value = e.target.value
    const promptTemplateId = value === '' ? null : value
    setUpdating(true)
    try {
      await chatApi.assignPrompt(conversationId, promptTemplateId)
      onChanged(promptTemplateId)
    } catch (err) {
      // fail silently with a console log — this is a secondary control, not worth a full error banner
      console.error('Failed to assign prompt', err)
    } finally {
      setUpdating(false)
    }
  }

  return (
    <select
      value={activePromptId || ''}
      onChange={handleChange}
      disabled={updating}
      className="bg-slate-800 border border-slate-700 rounded-lg px-2 py-1 text-xs text-slate-300 focus:outline-none focus:ring-1 focus:ring-indigo-500"
    >
      <option value="">Default persona</option>
      {prompts.map((p) => (
        <option key={p.id} value={p.id}>
          {p.name}
        </option>
      ))}
    </select>
  )
}