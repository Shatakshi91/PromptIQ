import { create } from 'zustand'

export const usePromptStore = create((set) => ({
  prompts: [],
  promptsLoading: false,

  setPrompts: (prompts) => set({ prompts }),
  setPromptsLoading: (v) => set({ promptsLoading: v }),

  addPromptToTop: (prompt) =>
    set((state) => ({ prompts: [prompt, ...state.prompts] })),

  updatePromptInList: (updated) =>
    set((state) => ({
      prompts: state.prompts.map((p) => (p.id === updated.id ? updated : p)),
    })),

  removePromptFromList: (id) =>
    set((state) => ({ prompts: state.prompts.filter((p) => p.id !== id) })),

  // Only one prompt can be default — reflect that locally without a refetch
  markAsDefault: (id) =>
    set((state) => ({
      prompts: state.prompts.map((p) => ({ ...p, isDefault: p.id === id })),
    })),
}))