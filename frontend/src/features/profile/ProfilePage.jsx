import { useState } from 'react'
import { userApi } from '../../api/userApi'
import { useAuthStore } from '../../store/authStore'
import Input from '../../components/Input'
import Button from '../../components/Button'
import { User, Lock, CheckCircle2, AlertCircle, Shield } from 'lucide-react'

export default function ProfilePage() {
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)

  const [displayName, setDisplayName] = useState(user?.displayName || '')
  const [profileMsg, setProfileMsg] = useState({ type: '', text: '' })
  const [profileLoading, setProfileLoading] = useState(false)

  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '' })
  const [passwordMsg, setPasswordMsg] = useState({ type: '', text: '' })
  const [passwordLoading, setPasswordLoading] = useState(false)

  const handleProfileSubmit = async (e) => {
    e.preventDefault()
    setProfileMsg({ type: '', text: '' })
    setProfileLoading(true)
    try {
      const { data } = await userApi.updateProfile({ displayName })
      setUser(data)
      setProfileMsg({ type: 'success', text: 'Display name updated.' })
    } catch (err) {
      setProfileMsg({ type: 'error', text: err.response?.data?.message || 'Failed to update.' })
    } finally {
      setProfileLoading(false)
    }
  }

  const handlePasswordSubmit = async (e) => {
    e.preventDefault()
    setPasswordMsg({ type: '', text: '' })
    setPasswordLoading(true)
    try {
      await userApi.changePassword(passwordForm)
      setPasswordMsg({ type: 'success', text: 'Password updated.' })
      setPasswordForm({ currentPassword: '', newPassword: '' })
    } catch (err) {
      const fieldErrors = err.response?.data?.fieldErrors
      const message = fieldErrors
        ? Object.values(fieldErrors)[0]
        : err.response?.data?.message || 'Failed to update password.'
      setPasswordMsg({ type: 'error', text: message })
    } finally {
      setPasswordLoading(false)
    }
  }

  const initial = (user?.displayName || user?.email || 'U').charAt(0).toUpperCase()

  return (
    <div className="flex-1 overflow-y-auto bg-gray-50">
      <div className="max-w-xl mx-auto px-6 py-10 space-y-6">

        {/* User identity */}
        <div className="flex items-center gap-3 pb-2">
          <div className="w-10 h-10 rounded-full bg-gray-900 text-white font-semibold text-sm flex items-center justify-center shrink-0">
            {initial}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <p className="text-sm font-semibold text-gray-900">{user?.displayName || 'User'}</p>
              {user?.role === 'ADMIN' && (
                <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-1.5 py-0.5 rounded bg-amber-100 text-amber-700 border border-amber-200">
                  <Shield className="w-2.5 h-2.5" />
                  Admin
                </span>
              )}
            </div>
            <p className="text-xs text-gray-400">{user?.email}</p>
          </div>
        </div>

        {/* Profile section */}
        <section className="bg-white border border-gray-200 rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100 flex items-center gap-2">
            <User className="w-4 h-4 text-gray-400" />
            <h2 className="text-sm font-medium text-gray-900">Profile</h2>
          </div>
          <form onSubmit={handleProfileSubmit} className="p-5 space-y-4">
            <AlertBanner message={profileMsg} />
            <Input
              label="Display Name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
            />
            <div className="flex justify-end">
              <Button type="submit" loading={profileLoading} size="sm">
                Save
              </Button>
            </div>
          </form>
        </section>

        {/* Password section */}
        <section className="bg-white border border-gray-200 rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100 flex items-center gap-2">
            <Lock className="w-4 h-4 text-gray-400" />
            <h2 className="text-sm font-medium text-gray-900">Password</h2>
          </div>
          <form onSubmit={handlePasswordSubmit} className="p-5 space-y-4">
            <AlertBanner message={passwordMsg} />
            <Input
              label="Current Password"
              type="password"
              placeholder="••••••••"
              value={passwordForm.currentPassword}
              onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
              required
            />
            <Input
              label="New Password"
              type="password"
              placeholder="••••••••"
              value={passwordForm.newPassword}
              onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
              minLength={8}
              required
              helperText="Minimum 8 characters."
            />
            <div className="flex justify-end">
              <Button type="submit" loading={passwordLoading} size="sm">
                Update
              </Button>
            </div>
          </form>
        </section>

      </div>
    </div>
  )
}

function AlertBanner({ message }) {
  if (!message?.text) return null
  const isError = message.type === 'error'
  return (
    <div
      className={`flex items-center gap-2 text-xs rounded-lg p-3 border ${
        isError
          ? 'text-red-600 bg-red-50 border-red-100'
          : 'text-green-700 bg-green-50 border-green-100'
      }`}
    >
      {isError ? (
        <AlertCircle className="w-4 h-4 shrink-0" />
      ) : (
        <CheckCircle2 className="w-4 h-4 shrink-0" />
      )}
      <span>{message.text}</span>
    </div>
  )
}