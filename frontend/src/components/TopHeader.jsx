import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { authApi } from '../api/authApi'
import { Sparkles, LogOut, User, Settings, ShieldCheck, LayoutDashboard } from 'lucide-react'

export default function TopHeader() {
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
    <header className="h-14 bg-white border-b border-gray-200/60 flex items-center justify-between px-4 sm:px-6 shrink-0 z-50 relative">
      <Link to="/chat" className="flex items-center gap-2.5 group">
        <div className="w-8 h-8 rounded-xl bg-black flex items-center justify-center text-white shadow-sm group-hover:shadow transition-all group-hover:scale-105">
          <Sparkles className="w-4 h-4" />
        </div>
        <span className="font-bold text-gray-900 tracking-tight text-lg">PromptIQ</span>
      </Link>

      <Link
        to="/prompts"
        className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium text-slate-400 hover:text-white hover:bg-slate-700/80 transition-colors"
      >
        <Sparkles className="w-4 h-4" />
        <span>Manage Prompts</span>
      </Link>

      <div className="flex items-center gap-3 relative group">
        <div className="w-9 h-9 rounded-full bg-black text-white font-semibold text-sm flex items-center justify-center cursor-pointer shadow-sm hover:ring-2 hover:ring-gray-300 transition-all">
          {(user?.displayName || user?.email || 'U').charAt(0).toUpperCase()}
        </div>

        {/* Hover Dropdown */}
        <div className="absolute right-0 top-full pt-1 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 w-56">
          <div className="bg-white rounded-xl shadow-lg border border-gray-100 overflow-hidden py-1">
            <div className="px-4 py-2.5 border-b border-gray-100 mb-1">
              <p className="text-sm font-semibold text-gray-900 truncate">{user?.displayName || 'User'}</p>
              <p className="text-xs text-gray-500 truncate">{user?.email}</p>
            </div>


            <Link to="/profile" className="flex items-center gap-2.5 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-gray-900 transition-colors">
              <User className="w-4 h-4 text-gray-400" />
              Profile
            </Link>
            <Link to="/profile" className="flex items-center gap-2.5 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-gray-900 transition-colors">
              <Settings className="w-4 h-4 text-gray-400" />
              Settings
            </Link>

            {user?.role === 'ADMIN' && (
              <Link to="/admin/users" className="flex items-center gap-2.5 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-gray-900 transition-colors">
                <ShieldCheck className="w-4 h-4 text-gray-400" />
                Admin
              </Link>
            )}

            <div className="border-t border-gray-100 mt-1 pt-1">
              <button onClick={handleLogout} className="w-full flex items-center gap-2.5 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 transition-colors text-left cursor-pointer">
                <LogOut className="w-4 h-4" />
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  )
}
