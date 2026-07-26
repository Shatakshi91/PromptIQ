import { useState } from 'react'
import { userApi } from '../../api/userApi'
import { useAuthStore } from '../../store/authStore'
import Input from '../../components/Input'
import Button from '../../components/Button'

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
      setProfileMsg({ type: 'success', text: 'Profile updated successfully' })
    } catch (err) {
      setProfileMsg({ type: 'error', text: err.response?.data?.message || 'Update failed' })
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
      setPasswordMsg({ type: 'success', text: 'Password changed successfully' })
      setPasswordForm({ currentPassword: '', newPassword: '' })
    } catch (err) {
      const fieldErrors = err.response?.data?.fieldErrors
      const message = fieldErrors
        ? Object.values(fieldErrors)[0]
        : err.response?.data?.message || 'Password change failed'
      setPasswordMsg({ type: 'error', text: message })
    } finally {
      setPasswordLoading(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto space-y-8">
      <h1 className="text-2xl font-bold text-white">Profile Settings</h1>

      {/* Update profile */}
      <form
        onSubmit={handleProfileSubmit}
        className="bg-slate-800/50 border border-slate-700 rounded-2xl p-6 space-y-4"
      >
        <h2 className="text-lg font-semibold text-white">Display Name</h2>
        <AlertBanner message={profileMsg} />
        <Input
          label="Display name"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          required
        />
        <Button type="submit" loading={profileLoading}>
          Save changes
        </Button>
      </form>

      {/* Change password */}
      <form
        onSubmit={handlePasswordSubmit}
        className="bg-slate-800/50 border border-slate-700 rounded-2xl p-6 space-y-4"
      >
        <h2 className="text-lg font-semibold text-white">Change Password</h2>
        <AlertBanner message={passwordMsg} />
        <Input
          label="Current password"
          type="password"
          value={passwordForm.currentPassword}
          onChange={(e) =>
            setPasswordForm({ ...passwordForm, currentPassword: e.target.value })
          }
          required
        />
        <Input
          label="New password"
          type="password"
          value={passwordForm.newPassword}
          onChange={(e) =>
            setPasswordForm({ ...passwordForm, newPassword: e.target.value })
          }
          minLength={8}
          required
        />
        <Button type="submit" loading={passwordLoading}>
          Update password
        </Button>
      </form>
    </div>
  )
}

function AlertBanner({ message }) {
  if (!message?.text) return null
  const isError = message.type === 'error'
  return (
    <p
      className={`text-sm rounded-lg px-3 py-2 border ${
        isError
          ? 'text-red-400 bg-red-950/50 border-red-800'
          : 'text-green-400 bg-green-950/50 border-green-800'
      }`}
    >
      {message.text}
    </p>
  )
}