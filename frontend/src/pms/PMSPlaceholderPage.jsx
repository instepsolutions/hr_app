import { useLocation } from 'react-router-dom'
import { PageLayout } from '../components/ui'

const pages = {
  'kra-kpi': ['Goal Framework', 'KRA & KPI Management'],
  'self-appraisal': ['Appraisal Cycle', 'Self Appraisal'],
  'manager-review': ['Appraisal Cycle', 'Manager Review'],
  'pip-management': ['Performance Support', 'PIP Management'],
  'promotions-increments': ['Rewards', 'Promotions & Increments'],
}

export default function PMSPlaceholderPage() {
  const { pathname } = useLocation()
  const segment = pathname.split('/').filter(Boolean).at(-1)
  const [eyebrow, title] = pages[segment] || ['Performance Management', 'Performance workspace']
  return <PageLayout eyebrow={eyebrow} title={title} description="This area is scheduled for a later Performance Management phase." className="pms-page"><div className="pms-phase-empty"><span>PM</span><h2>{title}</h2><p>This module is not part of the current release scope.</p></div></PageLayout>
}
