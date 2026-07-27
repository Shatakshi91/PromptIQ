export default function MessageBubble({ role, content }) {
  const isUser = role === 'USER'

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-2xl rounded-2xl px-4 py-2.5 text-sm whitespace-pre-wrap ${
          isUser
            ? 'bg-indigo-600 text-white'
            : 'bg-slate-800 text-slate-100 border border-slate-700'
        }`}
      >
        {content}
      </div>
    </div>
  )
}