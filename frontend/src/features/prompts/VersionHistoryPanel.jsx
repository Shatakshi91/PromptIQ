import { useEffect, useState } from 'react'
import { promptApi } from '../../api/promptApi'

export default function VersionHistoryPanel({ promptId, onClose }) {
  const [versions, setVersions] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    promptApi.versions(promptId).then(({ data }) => {
      setVersions(data.reverse()) // newest first
      setLoading(false)
    })
  }, [promptId])

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-lg max-h-[80vh] overflow-hidden flex flex-col">
        <div className="p-6 pb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white">Version History</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white">
            ✕
          </button>
        </div>

        <div className="overflow-y-auto px-6 pb-6 space-y-3">
          {loading ? (
            <p className="text-slate-500 text-sm">Loading...</p>
          ) : (
            versions.map((v) => (
              <div
                key={v.versionNumber}
                className="bg-slate-900 border border-slate-700 rounded-lg p-3"
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-medium text-indigo-400">
                    Version {v.versionNumber}
                  </span>
                  <span className="text-xs text-slate-500">
                    {new Date(v.createdAt).toLocaleString()}
                  </span>
                </div>
                <p className="text-sm text-slate-300 whitespace-pre-wrap">{v.content}</p>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}