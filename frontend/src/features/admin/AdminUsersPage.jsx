import { useEffect, useState, useCallback } from 'react'
import { adminApi } from '../../api/adminApi'

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
            setError(err.response?.data?.message || 'Failed to load users')
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
            setError(err.response?.data?.message || 'Failed to update status')
        }
    }

    const handleRoleChange = async (user, newRole) => {
        try {
            await adminApi.updateRole(user.id, newRole)
            fetchUsers(page)
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to update role')
        }
    }

    return (
        <div className="max-w-4xl mx-auto space-y-4">
            <h1 className="text-2xl font-bold text-white">User Management</h1>

            {error && (
                <p className="text-sm text-red-400 bg-red-950/50 border border-red-800 rounded-lg px-3 py-2">
                    {error}
                </p>
            )}

            <div className="bg-slate-800/50 border border-slate-700 rounded-2xl overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-slate-800 text-slate-400 text-left">
                        <tr>
                            <th className="px-4 py-3">Name</th>
                            <th className="px-4 py-3">Email</th>
                            <th className="px-4 py-3">Role</th>
                            <th className="px-4 py-3">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="text-white divide-y divide-slate-700">
                        {loading ? (
                            <tr>
                                <td colSpan={4} className="px-4 py-6 text-center text-slate-400">
                                    Loading...
                                </td>
                            </tr>
                        ) : (
                            pageData?.content.map((u) => (
                                <tr key={u.id}>
                                    <td className="px-4 py-3">{u.displayName}</td>
                                    <td className="px-4 py-3 text-slate-400">{u.email}</td>
                                    <td className="px-4 py-3">


                                        <select
                                            value={u.role}
                                            onChange={(e) => handleRoleChange(u, e.target.value)}
                                            className="bg-slate-900 border border-slate-700 rounded px-2 py-1 text-sm"
                                        >
                                            <option value="USER">USER</option>
                                            <option value="ADMIN">ADMIN</option>
                                        </select>

                                    </td>
                                    <td className="px-4 py-3 flex items-center gap-2">
                                        <span
                                            className={`text-xs px-2 py-0.5 rounded-full ${u.enabled ? 'bg-green-900 text-green-400' : 'bg-red-900 text-red-400'
                                                }`}
                                        >
                                            {u.enabled ? 'Active' : 'Disabled'}
                                        </span>
                                        <button
                                            onClick={() => handleToggleStatus(u)}
                                            className="text-xs rounded-lg bg-slate-700 hover:bg-slate-600 px-3 py-1.5"
                                        >
                                            Toggle
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {pageData && (
                <div className="flex items-center justify-between text-sm text-slate-400">
                    <span>
                        Page {pageData.page + 1} of {pageData.totalPages} ({pageData.totalElements} users)
                    </span>
                    <div className="flex gap-2">
                        <button
                            disabled={page === 0}
                            onClick={() => setPage((p) => p - 1)}
                            className="px-3 py-1.5 rounded-lg bg-slate-800 disabled:opacity-40"
                        >
                            Previous
                        </button>
                        <button
                            disabled={page + 1 >= pageData.totalPages}
                            onClick={() => setPage((p) => p + 1)}
                            className="px-3 py-1.5 rounded-lg bg-slate-800 disabled:opacity-40"
                        >
                            Next
                        </button>
                    </div>
                </div>
            )}
        </div>
    )
}