import { useLocation } from 'react-router-dom'
import { PageLayout } from '../components/ui'

const pageTitles = {
  '/dashboard': ['Your workspace', 'Dashboard'],
  '/employee-management/organizational-structure': ['People operations', 'Organizational structure'],
  '/employee-management/manage-profiles': ['People operations', 'Manage profiles'],
  '/employee-management/employee-lifecycle': ['People operations', 'Employee lifecycle'],
  '/employee-management/bulk-actions': ['People operations', 'Bulk actions'],
}

export default function PlaceholderPage() {
  const { pathname } = useLocation()
  const [eyebrow, title] = pageTitles[pathname] || ['People operations', 'Employee management']
  return <PageLayout eyebrow={eyebrow} title={title} className="placeholder-page"><div className="placeholder-rule" /></PageLayout>
}
