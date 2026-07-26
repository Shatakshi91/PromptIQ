import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { authApi } from '../api/authApi'

export default function Navbar() {
  const user = useAuthStore((s) => s.user)
  const logoutStore = useAuthStore((s) => s.logout)
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } finally {
      logoutStore()
      navigate('/login')
    }
  }

  return (
    <nav className="bg-slate-900 border-b border-slate-800 px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-6">
        <Link to="/" className="text-white font-bold">
          AI Agent Platform
        </Link>
        <Link to="/profile" className="text-slate-400 hover:text-white text-sm">
          Profile
        </Link>
        {user?.role === 'ADMIN' && (
          <Link to="/admin/users" className="text-slate-400 hover:text-white text-sm">
            Admin
          </Link>
        )}
      </div>
      <div className="flex items-center gap-4">
        <span className="text-slate-400 text-sm">{user?.displayName}</span>
        <button
          onClick={handleLogout}
          className="rounded-lg bg-red-600 hover:bg-red-500 px-3 py-1.5 text-sm font-medium text-white"
        >
          Logout
        </button>
      </div>
    </nav>
  )
}