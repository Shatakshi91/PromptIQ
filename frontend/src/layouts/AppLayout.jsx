import { Outlet } from 'react-router-dom'
import TopHeader from '../components/TopHeader'

export default function AppLayout() {
  return (
    <div className="h-screen w-screen overflow-hidden bg-[#f5f5f7] text-gray-900 flex flex-col font-sans antialiased">
      <TopHeader />
      <main className="flex-1 flex min-h-0 min-w-0 overflow-hidden">
        <Outlet />
      </main>
    </div>
  )
}