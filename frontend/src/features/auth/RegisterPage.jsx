import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '../../api/authApi'
import Input from '../../components/Input'
import Button from '../../components/Button'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', displayName: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authApi.register(form)
      navigate('/login')
    } catch (err) {
      const fieldErrors = err.response?.data?.fieldErrors
      const message = fieldErrors
        ? Object.values(fieldErrors)[0]
        : err.response?.data?.message || 'Registration failed'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm bg-slate-800/50 border border-slate-700 rounded-2xl p-8 space-y-4"
      >
        <h1 className="text-2xl font-bold text-white text-center">Create account</h1>

        {error && (
          <p className="text-sm text-red-400 bg-red-950/50 border border-red-800 rounded-lg px-3 py-2">
            {error}
          </p>
        )}

        <Input
          label="Display name"
          value={form.displayName}
          onChange={(e) => setForm({ ...form, displayName: e.target.value })}
          required
        />
        <Input
          label="Email"
          type="email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          required
        />
        <Input
          label="Password"
          type="password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          required
          minLength={8}
        />

        <Button type="submit" loading={loading}>
          Create account
        </Button>

        <p className="text-sm text-slate-400 text-center">
          Already have an account?{' '}
          <Link to="/login" className="text-indigo-400 hover:underline">
            Sign in
          </Link>
        </p>
      </form>
    </div>
  )
}