import { useEffect, useState, useCallback } from 'react'
import { adminApi } from '../../api/adminApi'
import { Shield, Users, ChevronLeft, ChevronRight, Loader2, AlertCircle } from 'lucide-react'

export default function AdminUsersPage() {
  const [pageData, setPageData] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchUsers = useCallback(async (targetPage) => {
    setLoading(true)
    setError('')
    try {
      const { data } = await adminApi.listUsers(targetPage, 10)
      setPageData(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load users.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchUsers(page)
  }, [page, fetchUsers])

  const handleToggleStatus = async (user) => {
    try {
      await adminApi.updateStatus(user.id, !user.enabled)
      fetchUsers(page)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update status.')
    }
  }

  const handleRoleChange = async (user, newRole) => {
    try {
      await adminApi.updateRole(user.id, newRole)
      fetchUsers(page)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update role.')
    }
  }

  return (
    <div className="flex-1 overflow-y-auto bg-gray-50">
      <div className="max-w-4xl mx-auto px-6 py-10 space-y-6">

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <Shield className="w-4 h-4 text-gray-400" />
              <h1 className="text-base font-semibold text-gray-900">User Management</h1>
            </div>
            <p className="text-xs text-gray-400 mt-0.5">Manage roles and account access.</p>
          </div>
          {pageData && (
            <span className="text-xs text-gray-500">
              {pageData.totalElements} user{pageData.totalElements !== 1 ? 's' : ''}
            </span>
          )}
        </div>

        {error && (
          <div className="flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg p-3">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Table */}
        <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="border-b border-gray-100">
                <tr className="text-xs text-gray-400 font-medium">
                  <th className="px-5 py-3">User</th>
                  <th className="px-5 py-3">Email</th>
                  <th className="px-5 py-3">Role</th>
                  <th className="px-5 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {loading ? (
                  <tr>
                    <td colSpan={4} className="px-5 py-12 text-center text-gray-400">
                      <div className="flex items-center justify-center gap-2 text-xs">
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Loading...
                      </div>
                    </td>
                  </tr>
                ) : pageData?.content?.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-5 py-8 text-center text-xs text-gray-400">
                      No users found.
                    </td>
                  </tr>
                ) : (
                  pageData?.content?.map((u) => (
                    <tr key={u.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-2.5">
                          <div className="w-7 h-7 rounded-full bg-gray-100 border border-gray-200 flex items-center justify-center text-xs font-semibold text-gray-600">
                            {(u.displayName || u.email || 'U').charAt(0).toUpperCase()}
                          </div>
                          <span className="text-sm font-medium text-gray-900">
                            {u.displayName || '—'}
                          </span>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-xs text-gray-500 font-mono">{u.email}</td>
                      <td className="px-5 py-3.5">
                        <select
                          value={u.role}
                          onChange={(e) => handleRoleChange(u, e.target.value)}
                          className="bg-gray-50 border border-gray-200 rounded-lg px-2.5 py-1 text-xs font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-200 cursor-pointer"
                        >
                          <option value="USER">User</option>
                          <option value="ADMIN">Admin</option>
                        </select>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <span
                            className={`inline-flex items-center gap-1.5 text-xs font-medium px-2 py-0.5 rounded-full ${
                              u.enabled
                                ? 'bg-green-50 text-green-700'
                                : 'bg-gray-100 text-gray-500'
                            }`}
                          >
                            <span
                              className={`w-1.5 h-1.5 rounded-full ${
                                u.enabled ? 'bg-green-500' : 'bg-gray-400'
                              }`}
                            />
                            {u.enabled ? 'Active' : 'Disabled'}
                          </span>
                          <button
                            onClick={() => handleToggleStatus(u)}
                            className="text-xs text-gray-400 hover:text-gray-700 underline cursor-pointer transition-colors"
                          >
                            Toggle
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {pageData && pageData.totalPages > 1 && (
            <div className="flex items-center justify-between px-5 py-3 border-t border-gray-100 text-xs text-gray-500">
              <span>
                Page {pageData.page + 1} of {pageData.totalPages}
              </span>
              <div className="flex items-center gap-2">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-white border border-gray-200 hover:bg-gray-50 disabled:opacity-40 text-gray-600 transition-colors cursor-pointer"
                >
                  <ChevronLeft className="w-3.5 h-3.5" />
                  Prev
                </button>
                <button
                  disabled={page + 1 >= pageData.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                  className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-white border border-gray-200 hover:bg-gray-50 disabled:opacity-40 text-gray-600 transition-colors cursor-pointer"
                >
                  Next
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>

      </div>
    </div>
  )
}