import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import ConversationSidebar from './ConversationSidebar'

export default function ChatLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  return (
    <div className="flex h-full w-full overflow-hidden bg-white relative">
      <ConversationSidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
      />
      <div className="flex-1 flex flex-col h-full min-w-0">
        <Outlet context={{ toggleSidebar: () => setIsSidebarOpen((prev) => !prev), isSidebarOpen }} />
      </div>
    </div>
  )
}