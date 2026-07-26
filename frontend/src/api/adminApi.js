import axiosInstance from './axiosInstance'

export const adminApi = {
  listUsers: (page = 0, size = 20) =>
    axiosInstance.get(`/admin/users?page=${page}&size=${size}`),
  getUser: (id) => axiosInstance.get(`/admin/users/${id}`),
  updateStatus: (id, enabled) =>
    axiosInstance.patch(`/admin/users/${id}/status`, { enabled }),
  updateRole: (id, role) =>
    axiosInstance.patch(`/admin/users/${id}/role`, { role }),
}