import { useAuthStore } from '../../store/authStore'
import { authApi } from '../../api/authApi'
import { useNavigate, Link } from 'react-router-dom'
import { MessageSquare, User, ShieldCheck, LogOut, ArrowRight } from 'lucide-react'

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

  const initial = (user?.displayName || user?.email || 'U').charAt(0).toUpperCase()

  return (
    <div className="flex-1 overflow-y-auto bg-gray-50">
      <div className="max-w-2xl mx-auto px-6 py-10 space-y-8">

        {/* User header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-gray-900 text-white font-semibold text-sm flex items-center justify-center shrink-0">
              {initial}
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-900">
                {user?.displayName || 'User'}
              </p>
              <p className="text-xs text-gray-400">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-1.5 text-xs text-gray-500 hover:text-red-500 transition-colors cursor-pointer px-3 py-1.5 rounded-lg hover:bg-red-50"
          >
            <LogOut className="w-3.5 h-3.5" />
            Sign out
          </button>
        </div>

        {/* Nav cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <NavCard
            to="/chat"
            icon={MessageSquare}
            title="Chat"
            description="Start or continue a conversation with the AI."
          />
          <NavCard
            to="/profile"
            icon={User}
            title="Profile"
            description="Update your display name or change your password."
          />
          {user?.role === 'ADMIN' && (
            <NavCard
              to="/admin/users"
              icon={ShieldCheck}
              title="Admin"
              description="Manage user accounts, roles, and permissions."
            />
          )}
        </div>

      </div>
    </div>
  )
}

function NavCard({ to, icon: Icon, title, description }) {
  return (
    <Link
      to={to}
      className="group flex items-start justify-between gap-4 bg-white border border-gray-200 rounded-xl p-5 hover:border-gray-300 hover:shadow-sm transition-all"
    >
      <div className="space-y-1">
        <div className="flex items-center gap-2">
          <Icon className="w-4 h-4 text-gray-500" />
          <span className="text-sm font-medium text-gray-900">{title}</span>
        </div>
        <p className="text-xs text-gray-400 leading-relaxed">{description}</p>
      </div>
      <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-gray-500 shrink-0 mt-0.5 transition-colors" />
    </Link>
  )
}