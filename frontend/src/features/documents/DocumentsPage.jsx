import { useEffect, useState } from 'react'
import { documentApi } from '../../api/documentApi'
import UploadDropzone from './UploadDropzone'
import StatusBadge from './StatusBadge'

function formatBytes(bytes) {
  if (!bytes) return '—'
  const kb = bytes / 1024
  return kb > 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb.toFixed(0)} KB`
}

export default function DocumentsPage() {
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [deletingId, setDeletingId] = useState(null)

  useEffect(() => {
    fetchDocuments()
  }, [])

  const fetchDocuments = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await documentApi.list(0, 50)
      setDocuments(data.content)
    } catch (err) {
      setError('Failed to load documents')
    } finally {
      setLoading(false)
    }
  }

  const handleFileSelected = async (file) => {
    setError('')
    setUploading(true)
    try {
      const { data } = await documentApi.upload(file)
      setDocuments((prev) => [data, ...prev])
      if (data.status === 'FAILED') {
        setError(`"${data.filename}" failed to process: ${data.errorMessage || 'Unknown error'}`)
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this document? The AI will no longer be able to reference it.')) return
    setDeletingId(id)
    try {
      await documentApi.delete(id)
      setDocuments((prev) => prev.filter((d) => d.id !== id))
    } catch (err) {
      setError('Failed to delete document')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Documents</h1>
        <p className="text-sm text-slate-400 mt-1">
          Upload documents to let the AI reference them when answering your questions —
          across any of your conversations.
        </p>
      </div>

      <UploadDropzone onFileSelected={handleFileSelected} uploading={uploading} />

      {error && (
        <p className="text-sm text-red-400 bg-red-950/50 border border-red-800 rounded-lg px-3 py-2">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-slate-500 text-sm">Loading...</p>
      ) : documents.length === 0 ? (
        <p className="text-slate-500 text-sm text-center py-4">No documents uploaded yet.</p>
      ) : (
        <div className="space-y-2">
          {documents.map((d) => (
            <div
              key={d.id}
              className="bg-slate-800/50 border border-slate-700 rounded-xl px-4 py-3 flex items-center justify-between gap-3"
            >
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm text-slate-200 truncate">{d.filename}</p>
                  <StatusBadge status={d.status} />
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  {formatBytes(d.fileSizeBytes)}
                  {d.status === 'PROCESSED' && ` · ${d.chunkCount} chunks indexed`}
                  {d.status === 'FAILED' && d.errorMessage && ` · ${d.errorMessage}`}
                </p>
              </div>
              <button
                onClick={() => handleDelete(d.id)}
                disabled={deletingId === d.id}
                className="shrink-0 text-xs rounded-lg bg-red-950 hover:bg-red-900 disabled:opacity-50 px-3 py-1.5 text-red-400"
              >
                {deletingId === d.id ? '...' : 'Delete'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}