import { useEffect, useState } from 'react'

function App() {
  const [status, setStatus] = useState('checking...')

  useEffect(() => {
    fetch('/api/v1/ping')
      .then((res) => res.json())
      .then((data) => setStatus(data.status))
      .catch(() => setStatus('backend unreachable'))
  }, [])

  return (
    <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
      <div className="text-center space-y-2">
        <h1 className="text-3xl font-bold">Get ready to give prompts</h1>
        <p className="text-slate-400">
          Backend status:{' '}
          <span className={status === 'UP' ? 'text-green-400' : 'text-red-400'}>
            {status}
          </span>
        </p>
      </div>
    </div>
  )
}

export default App