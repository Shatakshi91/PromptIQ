import axiosInstance from './axiosInstance'

export const userApi = {
  me: () => axiosInstance.get('/users/me'),
  updateProfile: (payload) => axiosInstance.patch('/users/me', payload),
  changePassword: (payload) => axiosInstance.patch('/users/me/password', payload),
}