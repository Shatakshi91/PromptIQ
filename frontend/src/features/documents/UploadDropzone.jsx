import { useRef, useState } from 'react'

export default function UploadDropzone({ onFileSelected, uploading }) {
  const inputRef = useRef(null)
  const [dragActive, setDragActive] = useState(false)

  const handleDrop = (e) => {
    e.preventDefault()
    setDragActive(false)
    const file = e.dataTransfer.files?.[0]
    if (file) onFileSelected(file)
  }

  const handleChange = (e) => {
    const file = e.target.files?.[0]
    if (file) onFileSelected(file)
    e.target.value = '' // allow re-selecting the same file later
  }

  return (
    <div
      onDragOver={(e) => { e.preventDefault(); setDragActive(true) }}
      onDragLeave={() => setDragActive(false)}
      onDrop={handleDrop}
      onClick={() => !uploading && inputRef.current?.click()}
      className={`border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-colors ${
        dragActive
          ? 'border-indigo-500 bg-indigo-950/30'
          : 'border-slate-700 hover:border-slate-600'
      } ${uploading ? 'opacity-60 cursor-not-allowed' : ''}`}
    >
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.docx,.doc,.txt"
        onChange={handleChange}
        disabled={uploading}
        className="hidden"
      />
      {uploading ? (
        <p className="text-slate-400 text-sm">Uploading and processing...</p>
      ) : (
        <>
          <p className="text-slate-300 text-sm font-medium">
            Drop a file here, or click to browse
          </p>
          <p className="text-slate-500 text-xs mt-1">PDF, DOCX, or TXT — up to 10MB</p>
        </>
      )}
    </div>
  )
}