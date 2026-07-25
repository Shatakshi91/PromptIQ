export default function Button({ children, loading, ...props }) {
  return (
    <button
      disabled={loading}
      className="w-full rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800 disabled:cursor-not-allowed px-4 py-2 font-medium text-white transition-colors"
      {...props}
    >
      {loading ? 'Please wait...' : children}
    </button>
  )
}