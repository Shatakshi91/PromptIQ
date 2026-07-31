import axiosInstance from './axiosInstance'

export const promptApi = {
  list: (page = 0, size = 20) =>
    axiosInstance.get(`/prompts?page=${page}&size=${size}`),

  get: (id) => axiosInstance.get(`/prompts/${id}`),

  create: (payload) => axiosInstance.post('/prompts', payload),

  update: (id, payload) => axiosInstance.patch(`/prompts/${id}`, payload),

  delete: (id) => axiosInstance.delete(`/prompts/${id}`),

  setDefault: (id) => axiosInstance.post(`/prompts/${id}/set-default`),

  versions: (id) => axiosInstance.get(`/prompts/${id}/versions`),
}