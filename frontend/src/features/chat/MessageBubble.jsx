import { useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Sparkles, Copy, Check, User } from 'lucide-react'

export default function MessageBubble({ role, content }) {
  const isUser = role === 'USER'
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(content)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  if (isUser) {
    return (
      <div className="flex gap-3 my-3 justify-end flex-row-reverse">
        <div className="max-w-[88%] sm:max-w-[80%]">
          <div className="text-sm font-medium text-gray-800 mb-1 px-1 text-right">You</div>
          <div className="bg-gray-50/80 border border-gray-200/60 rounded-2xl rounded-tr-sm px-4 py-2.5 text-sm text-gray-900 leading-relaxed whitespace-pre-wrap break-words">
            {content}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex gap-3 my-3 justify-start">
      <div className="w-7 h-7 rounded-lg bg-gray-100 border border-gray-200 flex items-center justify-center shrink-0 mt-0.5">
        <Sparkles className="w-3.5 h-3.5 text-gray-500" />
      </div>

      <div className="group relative max-w-[88%] sm:max-w-[80%]">
        <div className="text-sm font-semibold text-gray-800 mb-1 px-1">PromptIQ</div>
        <div className="bg-white border border-gray-100 rounded-2xl rounded-tl-sm px-4 py-3 text-sm shadow-sm">
          <div className="markdown-body">
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                code({ node, inline, className, children, ...props }) {
                  const match = /language-(\w+)/.exec(className || '')
                  const codeString = String(children).replace(/\n$/, '')

                  if (!inline && (match || codeString.includes('\n'))) {
                    return (
                      <CodeBlock
                        language={match ? match[1] : ''}
                        value={codeString}
                      />
                    )
                  }
                  return (
                    <code
                      className="bg-gray-100 text-gray-800 border border-gray-200 px-1.5 py-0.5 rounded text-xs font-mono"
                      {...props}
                    >
                      {children}
                    </code>
                  )
                },
              }}
            >
              {content}
            </ReactMarkdown>
          </div>

          {/* Copy button */}
          <div className="mt-2.5 pt-2 border-t border-gray-100 flex justify-end">
            <button
              onClick={handleCopy}
              className="inline-flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors cursor-pointer"
              title="Copy response"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-green-500" />
                  <span className="text-green-600">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>Copy</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function CodeBlock({ language, value }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(value)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="my-3 rounded-xl overflow-hidden border border-gray-800 bg-gray-950 text-gray-100">
      <div className="flex items-center justify-between px-4 py-2 bg-gray-900 border-b border-gray-800">
        <span className="text-xs text-gray-400 font-mono">{language || 'code'}</span>
        <button
          onClick={handleCopy}
          className="inline-flex items-center gap-1 text-xs text-gray-500 hover:text-gray-200 transition-colors cursor-pointer"
        >
          {copied ? (
            <>
              <Check className="w-3.5 h-3.5 text-green-400" />
              <span className="text-green-400">Copied</span>
            </>
          ) : (
            <>
              <Copy className="w-3.5 h-3.5" />
              <span>Copy</span>
            </>
          )}
        </button>
      </div>
      <div className="p-4 overflow-x-auto text-xs font-mono leading-relaxed">
        <pre className="m-0 bg-transparent p-0">
          <code>{value}</code>
        </pre>
      </div>
    </div>
  )
}