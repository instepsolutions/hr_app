import { ChevronRight, House } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'

const routeNames = {
  dashboard: 'Dashboard',
  'employee-management': 'Employee management',
  directory: 'Directory',
  'organizational-structure': 'Organizational structure',
  'manage-profiles': 'Manage profiles',
  'employee-lifecycle': 'Employee lifecycle',
  'bulk-actions': 'Bulk actions',
  employees: 'Employee details',
}

export default function Breadcrumb() {
  const { pathname } = useLocation()
  const segments = pathname.split('/').filter(Boolean)

  return (
    <nav className="breadcrumb" aria-label="Breadcrumb">
      <Link to="/dashboard" className="breadcrumb-home" aria-label="Dashboard"><House size={14} /></Link>
      {segments.map((segment, index) => {
        const isLast = index === segments.length - 1
        const label = routeNames[segment] || (segment === segments.at(-1) ? `Employee ${segment}` : segment)
        const href = `/${segments.slice(0, index + 1).join('/')}`
        return (
          <span className="breadcrumb-part" key={`${segment}-${index}`}>
            <ChevronRight size={13} aria-hidden="true" />
            {isLast ? <span aria-current="page">{label}</span> : <Link to={href}>{label}</Link>}
          </span>
        )
      })}
    </nav>
  )
}
