import { useState } from 'react'
import {
  Activity, Banknote, BookOpenCheck, Building2, CalendarDays, ChevronDown, ChevronLeft,
  ChevronRight, ContactRound, FileText, Gift, LayoutDashboard, Network, ShieldCheck,
  UserRoundPlus, UsersRound, Workflow,
} from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'

const employeeLinks = [
  { label: 'Employee Directory', to: '/employee-management/directory', icon: UsersRound },
  { label: 'Organization', to: '/employee-management/organizational-structure', icon: Network },
  { label: 'Manage Profiles', to: '/employee-management/manage-profiles', icon: ContactRound },
  { label: 'Employee Lifecycle', to: '/employee-management/employee-lifecycle', icon: Activity },
  { label: 'Bulk Actions', to: '/employee-management/bulk-actions', icon: Workflow },
]

const futureModules = [
  { label: 'Recruitment', icon: UserRoundPlus },
  { label: 'Onboarding', icon: BookOpenCheck },
  { label: 'Attendance & Leave', icon: CalendarDays },
  { label: 'Payroll', icon: Banknote },
  { label: 'Performance (PMS)', icon: Activity },
  { label: 'Training & Development', icon: BookOpenCheck },
  { label: 'Compensation', icon: Banknote },
  { label: 'Benefits', icon: Gift },
  { label: 'Exit Management', icon: UserRoundPlus },
  { label: 'Reports & Analytics', icon: Activity },
  { label: 'Documents', icon: FileText },
  { label: 'HR Administration', icon: ShieldCheck },
  { label: 'System Setup', icon: Building2 },
]

export default function Sidebar({ collapsed, onToggle, mobileOpen, onNavigate }) {
  const { pathname } = useLocation()
  const employeeAreaActive = pathname.startsWith('/employee-management')
  const [employeeOpen, setEmployeeOpen] = useState(employeeAreaActive)

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
        {!collapsed && <div className="workspace-label">WORKSPACE</div>}
        <nav className="primary-nav" aria-label="Primary navigation">
          <NavLink to="/dashboard" end onClick={onNavigate} className={({ isActive }) => `nav-item ${isActive ? 'is-active' : ''}`} title={collapsed ? 'Dashboard' : undefined}>
            <LayoutDashboard size={18} strokeWidth={1.9} />{!collapsed && <span>Dashboard</span>}
          </NavLink>
          <div className={`nav-group ${employeeAreaActive ? 'is-current' : ''}`}>
            <button className={`nav-item nav-group-trigger ${employeeAreaActive ? 'is-group-active' : ''}`} onClick={() => setEmployeeOpen((open) => !open)} aria-expanded={employeeOpen} title={collapsed ? 'Employee Management' : undefined}>
              <UsersRound size={18} strokeWidth={1.9} />{!collapsed && <><span>Employee Management</span><ChevronDown className={`nav-chevron ${employeeOpen ? 'is-open' : ''}`} size={14} /></>}
            </button>
            {!collapsed && employeeOpen && <div className="nav-submenu">{employeeLinks.map(({ label, to, icon: Icon }) => <NavLink key={to} to={to} onClick={onNavigate} className={({ isActive }) => `nav-item nav-subitem ${isActive ? 'is-active' : ''}`}><Icon size={16} strokeWidth={1.9} /><span>{label}</span></NavLink>)}</div>}
          </div>
          {futureModules.map(({ label, icon: Icon }) => <div key={label} className="nav-item nav-item-disabled" aria-disabled="true" title={`${label} is not part of this release`}><Icon size={18} strokeWidth={1.9} />{!collapsed && <span>{label}</span>}</div>)}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-bottom-rule" />
          {!collapsed && <span>People, with purpose.</span>}
          <span className="sidebar-version">HRMS <i>·</i> 2.0</span>
        </div>
      </aside>
    </>
  )
}
