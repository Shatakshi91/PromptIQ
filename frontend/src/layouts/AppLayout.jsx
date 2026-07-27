import { Outlet, useLocation } from 'react-router-dom'
import Navbar from '../components/Navbar'

export default function AppLayout() {
  const location = useLocation()
  const isChatRoute = location.pathname.startsWith('/chat')

  return (
    <div className="min-h-screen bg-slate-900">
      <Navbar />
      <main className={isChatRoute ? '' : 'p-6'}>
        <Outlet />
      </main>
    </div>
  )
}