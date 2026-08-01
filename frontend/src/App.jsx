import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import DashboardPage from './features/dashboard/DashboardPage'
import ProfilePage from './features/profile/ProfilePage'
import AdminUsersPage from './features/admin/AdminUsersPage'
import PromptsPage from './features/prompts/PromptsPage'
import ChatLayout from './features/chat/ChatLayout'
import ChatThreadPage from './features/chat/ChatThreadPage'
import ProtectedRoute from './components/ProtectedRoute'
import AdminRoute from './components/AdminRoute'
import AppLayout from './layouts/AppLayout'
import MemoriesPage from './features/memories/MemoriesPage'
import DocumentsPage from './features/documents/DocumentsPage'
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<Navigate to="/chat" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/prompts" element={<PromptsPage />} />
          <Route path="/memories" element={<MemoriesPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route
            path="/admin/users"
            element={
              <AdminRoute>
                <AdminUsersPage />
              </AdminRoute>
            }
          />

          <Route path="/chat" element={<ChatLayout />}>
            <Route index element={<ChatThreadPage />} />
            <Route path=":conversationId" element={<ChatThreadPage />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App