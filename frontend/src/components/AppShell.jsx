import { useState } from 'react'
import Header from './Header'
import Sidebar from './Sidebar'

export default function AppShell({ session, onLogout, children }) {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <div className={`app-shell ${collapsed ? 'sidebar-collapsed' : ''}`}>
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((value) => !value)} mobileOpen={mobileOpen} onNavigate={() => setMobileOpen(false)} />
      <div className="main-column">
        <Header session={session} onLogout={onLogout} onMenu={() => setMobileOpen(true)} />
        {children}
      </div>
    </div>
  )
}
