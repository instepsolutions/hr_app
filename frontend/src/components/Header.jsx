import { Bell, ChevronDown, CircleHelp, LogOut, Menu, MessageCircle, Search } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Breadcrumb from './Breadcrumb'

export default function Header({ session, onLogout, onMenu, onNotify }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const [search, setSearch] = useState('')
  const navigate = useNavigate()
  const initials = (session?.username || 'HR').slice(0, 2).toUpperCase()
  const role = (session?.role || 'HR').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())

  function submitSearch(event) {
    event.preventDefault()
    if (!search.trim()) return
    navigate(`/employee-management/directory?search=${encodeURIComponent(search.trim())}`)
    setSearch('')
  }

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button className="mobile-menu-button" onClick={onMenu} aria-label="Open navigation"><Menu size={20} /></button>
        <Breadcrumb />
      </div>
      <div className="topbar-tools">
        <form className="topbar-search" onSubmit={submitSearch} role="search"><Search size={15} /><input aria-label="Search employees" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search people, IDs" /><kbd>/</kbd></form>
        <span className="topbar-divider" />
        <button className="icon-button notification-button" aria-label="Notifications" title="Notifications" onClick={() => onNotify({ type: 'success', message: 'You are up to date with dashboard notifications.' })}><Bell size={18} /><i /></button>
        <button className="icon-button topbar-utility-button" aria-label="Messages" title="Messages" onClick={() => onNotify({ type: 'error', message: 'Messaging is not part of the current Employee Management release.' })}><MessageCircle size={17} /></button>
        <button className="icon-button topbar-utility-button" aria-label="Help" title="Help" onClick={() => onNotify({ type: 'success', message: 'Contact your HR administrator for workspace support.' })}><CircleHelp size={17} /></button>
        <div className="account-wrap">
          <button className="account-button" onClick={() => setMenuOpen((open) => !open)} aria-expanded={menuOpen}>
            <span className="account-avatar">{initials}</span>
            <span className="account-name"><strong>{session?.username}</strong><small>{role}</small></span>
            <ChevronDown size={14} />
          </button>
          {menuOpen && <div className="account-menu"><button onClick={onLogout}><LogOut size={15} />Sign out</button></div>}
        </div>
      </div>
    </header>
  )
}
