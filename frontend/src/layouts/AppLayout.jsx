import { Outlet } from 'react-router-dom'
import Navbar from '../components/Navbar'

export default function AppLayout() {
  return (
    <div className="min-h-screen bg-slate-900">
      <Navbar />
      <main className="p-6">
        <Outlet />
      </main>
    </div>
  )
}