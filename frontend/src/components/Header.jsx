import { Bell, Menu, Search, LogOut, ChevronDown } from 'lucide-react'
import { useState } from 'react'
import Breadcrumb from './Breadcrumb'

export default function Header({ session, onLogout, onMenu }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const initials = (session?.username || 'HR').slice(0, 2).toUpperCase()

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button className="mobile-menu-button" onClick={onMenu} aria-label="Open navigation"><Menu size={20} /></button>
        <Breadcrumb />
      </div>
      <div className="topbar-tools">
        <button className="topbar-search" type="button" aria-label="Search employees" onClick={() => document.querySelector('#employee-search')?.focus()}>
          <Search size={15} /><span>Find an employee</span><kbd>/</kbd>
        </button>
        <span className="topbar-divider" />
        <button className="icon-button notification-button" aria-label="Notifications"><Bell size={18} /><i /></button>
        <div className="account-wrap">
          <button className="account-button" onClick={() => setMenuOpen((open) => !open)} aria-expanded={menuOpen}>
            <span className="account-avatar">{initials}</span>
            <span className="account-name">{session?.username}</span>
            <ChevronDown size={14} />
          </button>
          {menuOpen && <div className="account-menu"><button onClick={onLogout}><LogOut size={15} />Sign out</button></div>}
        </div>
      </div>
    </header>
  )
}
