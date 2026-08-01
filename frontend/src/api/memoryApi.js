import axiosInstance from './axiosInstance'

export const memoryApi = {
  list: () => axiosInstance.get('/memories'),
  delete: (id) => axiosInstance.delete(`/memories/${id}`),
}