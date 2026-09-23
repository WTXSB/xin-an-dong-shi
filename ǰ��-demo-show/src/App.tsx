import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import DetectPage from './pages/DetectPage'
import ChatPage from './pages/ChatPage'
import MindfulnessPage from './pages/MindfulnessPage'
import DiaryPage from './pages/DiaryPage'
import ProfilePage from './pages/ProfilePage'
import GardenPage from './pages/GardenPage'
import MePage from './pages/MePage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Login — no sidebar */}
        <Route path="/login" element={<LoginPage />} />

        {/* All other pages wrapped in AppLayout */}
        <Route element={<AppLayout />}>
          <Route path="/home" element={<HomePage />} />
          <Route path="/detect" element={<DetectPage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/mindfulness" element={<MindfulnessPage />} />
          <Route path="/diary" element={<DiaryPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/garden" element={<GardenPage />} />
          <Route path="/me" element={<MePage />} />
        </Route>

        {/* Default redirect */}
        <Route path="*" element={<Navigate to="/home" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
