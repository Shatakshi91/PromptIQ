import axiosInstance from './axiosInstance'

export const documentApi = {
  list: (page = 0, size = 20) =>
    axiosInstance.get(`/documents?page=${page}&size=${size}`),

  get: (id) => axiosInstance.get(`/documents/${id}`),

  upload: (file, onUploadProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return axiosInstance.post('/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    })
  },

  delete: (id) => axiosInstance.delete(`/documents/${id}`),
}