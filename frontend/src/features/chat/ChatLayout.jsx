import { Outlet } from 'react-router-dom'
import ConversationSidebar from './ConversationSidebar'

export default function ChatLayout() {
  return (
    <div className="flex h-[calc(100vh-57px)]">
      <ConversationSidebar />
      <Outlet />
    </div>
  )
}