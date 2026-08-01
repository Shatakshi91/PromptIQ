export default function StatusBadge({ status }) {
  const styles = {
    PENDING: 'bg-yellow-900 text-yellow-400',
    PROCESSED: 'bg-green-900 text-green-400',
    FAILED: 'bg-red-900 text-red-400',
  }

  return (
    <span className={`text-xs px-2 py-0.5 rounded-full ${styles[status] || 'bg-slate-700 text-slate-300'}`}>
      {status}
    </span>
  )
}