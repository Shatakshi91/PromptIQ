import axiosInstance from './axiosInstance'

export const chatApi = {
  listConversations: (page = 0, size = 20) =>
    axiosInstance.get(`/conversations?page=${page}&size=${size}`),

  createConversation: (title) =>
    axiosInstance.post('/conversations', { title }),

  getConversation: (id) =>
    axiosInstance.get(`/conversations/${id}`),

  renameConversation: (id, title) =>
    axiosInstance.patch(`/conversations/${id}`, { title }),

  deleteConversation: (id) =>
    axiosInstance.delete(`/conversations/${id}`),

  listMessages: (conversationId, page = 0, size = 50) =>
    axiosInstance.get(`/conversations/${conversationId}/messages?page=${page}&size=${size}`),

  addMessage: (conversationId, role, content) =>
    axiosInstance.post(`/conversations/${conversationId}/messages`, { role, content }),
}