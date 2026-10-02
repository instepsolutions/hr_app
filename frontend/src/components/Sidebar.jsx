import {
  Activity, Building2, ChevronLeft, ChevronRight, ContactRound, LayoutDashboard,
  Network, UsersRound, Workflow,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

const navigation = [
  { label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Employee directory', to: '/employee-management/directory', icon: UsersRound },
  { label: 'Organization', to: '/employee-management/organizational-structure', icon: Network },
  { label: 'Manage profiles', to: '/employee-management/manage-profiles', icon: ContactRound },
  { label: 'Employee lifecycle', to: '/employee-management/employee-lifecycle', icon: Activity },
  { label: 'Bulk actions', to: '/employee-management/bulk-actions', icon: Workflow },
]

export default function Sidebar({ collapsed, onToggle, mobileOpen, onNavigate }) {
  return (
    <>
      <button className={`sidebar-scrim ${mobileOpen ? 'is-visible' : ''}`} onClick={onNavigate} aria-label="Close navigation" tabIndex={mobileOpen ? 0 : -1} />
      <aside className={`sidebar ${collapsed ? 'is-collapsed' : ''} ${mobileOpen ? 'is-mobile-open' : ''}`}>
        <div className="brand-row">
          <div className="brand-mark"><Building2 size={19} strokeWidth={2.3} /></div>
          {!collapsed && <div className="brand-copy"><span>northstar</span><small>PEOPLE OPERATIONS</small></div>}
          <button className="sidebar-toggle" onClick={onToggle} aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}>
            {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
          </button>
        </div>
        <div className="workspace-label">WORKSPACE</div>
        <nav className="primary-nav" aria-label="Primary navigation">
          {navigation.map(({ label, to, icon: Icon }) => (
            <NavLink key={to} to={to} onClick={onNavigate} className={({ isActive }) => `nav-item ${isActive ? 'is-active' : ''}`} title={collapsed ? label : undefined}>
              <Icon size={18} strokeWidth={1.9} />
              {!collapsed && <span>{label}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-bottom-rule" />
          {!collapsed && <span>People, with purpose.</span>}
          <span className="sidebar-version">HRMS <i>·</i> 1.0</span>
        </div>
      </aside>
    </>
  )
}
