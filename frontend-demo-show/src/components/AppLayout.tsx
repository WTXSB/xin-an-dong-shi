import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { navItems } from '../data/navItems'
import { mockUser } from '../data/mockUser'
import './AppLayout.css'

function AppLayout() {
  const location = useLocation()

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-icon">🌸</span>
          <div>
            <div className="brand-name">心安动识</div>
            <div className="brand-sub">MindEase</div>
          </div>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `nav-item ${isActive ? 'nav-item--active' : ''}`
              }
            >
              <span className="nav-emoji">{item.emoji}</span>
              <span className="nav-label">{item.label}</span>
            </NavLink>
          ))}
        </nav>

        {/* User mini profile at bottom of sidebar */}
        <div className="sidebar-user">
          <div className="user-avatar">
            {mockUser.avatar ? (
              <img src={mockUser.avatar} alt={mockUser.name} />
            ) : (
              <span className="avatar-placeholder">{mockUser.moodEmoji}</span>
            )}
          </div>
          <div className="user-info">
            <div className="user-name">{mockUser.name}</div>
            <div className="user-mood">{mockUser.moodEmoji} {mockUser.mood}</div>
          </div>
        </div>
      </aside>

      {/* Main content area */}
      <main className="main-content">
        <div className="page-container fade-in" key={location.pathname}>
          <Outlet />
        </div>
      </main>
    </div>
  )
}

export default AppLayout
