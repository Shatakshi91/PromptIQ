import { useAuthStore } from '../../store/authStore'
import { authApi } from '../../api/authApi'
import { useNavigate } from 'react-router-dom'

export default function DashboardPage() {
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
    <div className="min-h-screen bg-slate-900 text-white flex flex-col items-center justify-center gap-4">
      <h1 className="text-2xl font-bold">
        Welcome, {user?.displayName || 'User'} 👋
      </h1>
      <p className="text-slate-400">{user?.email}</p>
      <button
        onClick={handleLogout}
        className="rounded-lg bg-red-600 hover:bg-red-500 px-4 py-2 text-sm font-medium"
      >
        Logout
      </button>
    </div>
  )
}