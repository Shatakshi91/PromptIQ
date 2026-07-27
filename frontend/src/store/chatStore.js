import { create } from 'zustand'

export const useChatStore = create((set, get) => ({
  conversations: [],
  activeConversationId: null,
  messages: [],
  conversationsLoading: false,
  messagesLoading: false,

  setConversations: (conversations) => set({ conversations }),

  addConversationToTop: (conversation) =>
    set((state) => ({ conversations: [conversation, ...state.conversations] })),

  updateConversationInList: (updated) =>
    set((state) => ({
      conversations: state.conversations.map((c) =>
        c.id === updated.id ? { ...c, ...updated } : c
      ),
    })),

  removeConversationFromList: (id) =>
    set((state) => ({
      conversations: state.conversations.filter((c) => c.id !== id),
    })),

  setActiveConversationId: (id) => set({ activeConversationId: id, messages: [] }),

  setMessages: (messages) => set({ messages }),

  appendMessage: (message) =>
    set((state) => ({ messages: [...state.messages, message] })),

  setConversationsLoading: (v) => set({ conversationsLoading: v }),
  setMessagesLoading: (v) => set({ messagesLoading: v }),

  // Moves a conversation to the top of the sidebar list after a new message,
  // mirroring the backend's "order by updatedAt desc" behavior without a refetch
  bumpConversationToTop: (id) => {
    const { conversations } = get()
    const target = conversations.find((c) => c.id === id)
    if (!target) return
    const rest = conversations.filter((c) => c.id !== id)
    set({ conversations: [{ ...target, updatedAt: new Date().toISOString() }, ...rest] })
  },
}))