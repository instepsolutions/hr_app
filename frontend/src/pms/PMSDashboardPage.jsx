import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Award, ClipboardCheck, CircleGauge, Goal, ShieldCheck, UsersRound } from 'lucide-react'
import { getApiErrorMessage } from '../api/client'
import { PageLayout } from '../components/ui'
import { pmsService } from '../services/pmsService'
import { AppraisalPanel, BarChartPanel, DashboardDeadlines, DashboardTable, DepartmentPerformance, MetricCards, PageControls, PageFooter, Panel, PipPanel, TopPerformers, TrendChart, defaultRange } from './PmsUI'

const tabs = [
  { id: 'overview', label: 'Overview' }, { id: 'goal-progress', label: 'Goal Progress' },
  { id: 'appraisal-status', label: 'Appraisal Status' }, { id: 'rating-distribution', label: 'Rating Distribution' },
  { id: 'department-performance', label: 'Department Performance' }, { id: 'pip-overview', label: 'PIP Overview' },
  { id: 'promotion-increment', label: 'Promotion & Increment' },
]
const metricLabels = [
  ['totalEmployees', 'Total Employees'], ['goalsAssigned', 'Goals Assigned'], ['goalsCompleted', 'Goals Completed'],
  ['averageProgress', 'Average Progress', '%'], ['appraisalsCompleted', 'Appraisals Completed'], ['averageRating', 'Average Rating'],
]

function RatingDistribution({ rows }) {
  return <BarChartPanel title="Rating distribution" rows={(rows || []).map((row) => ({ ...row, label: `${row.rating} star${row.rating > 1 ? 's' : ''}` }))} categoryKey="label" valueKey="count" color="#d48a32" />
}

function PromotionEmpty() {
  return <section className="pms-phase-empty"><span><Award size={22} /></span><h2>Promotion & Increment</h2><p>This workspace will be available in a later Performance Management phase.</p></section>
}

function DashboardQuickActions({ onNavigate }) {
  const actions = [
    ['Create New Goal', Goal, '/performance/goal-management'],
    ['Self Appraisal', ClipboardCheck, '/performance/self-appraisal'],
    ['Manager Review', UsersRound, '/performance/manager-review'],
    ['PIP Management', ShieldCheck, '/performance/pip-management'],
  ]
  return <Panel title="Quick actions" className="pms-quick-actions"><div>{actions.map(([label, Icon, path]) => <button key={label} onClick={() => onNavigate(path)}><span><Icon size={16} /></span>{label}</button>)}</div></Panel>
}

function DashboardContent({ tab, data, trend, granularity, onGranularityChange, onNavigate }) {
  if (tab === 'goal-progress') return <><div className="pms-content-grid pms-grid-two"><TrendChart data={trend} granularity={granularity} onGranularityChange={onGranularityChange} /><DepartmentPerformance rows={data.departmentPerformance} /></div><DashboardTable rows={data.departmentGoals} /></>
  if (tab === 'appraisal-status') return <div className="pms-content-grid pms-grid-two"><AppraisalPanel data={data.appraisalStatus} /><DashboardDeadlines rows={data.deadlines} /></div>
  if (tab === 'rating-distribution') return <div className="pms-content-grid pms-grid-two"><RatingDistribution rows={data.ratingDistribution} /><DepartmentPerformance rows={data.departmentPerformance} /><div className="pms-span-two"><TopPerformers rows={data.topPerformers} /></div></div>
  if (tab === 'department-performance') return <div className="pms-content-grid pms-grid-two"><DepartmentPerformance rows={data.departmentPerformance} /><DashboardTable rows={data.departmentGoals} /></div>
  if (tab === 'pip-overview') return <div className="pms-content-grid pms-grid-two"><PipPanel data={data.pip} /><section className="pms-pip-summary"><span><CircleGauge size={20} /></span><strong>{data.pip.active}</strong><small>Active improvement plans</small><p>{data.pip.improved} of {data.pip.completed} completed plans marked improved.</p></section></div>
  if (tab === 'promotion-increment') return <PromotionEmpty />
  return <>
    <div className="pms-content-grid pms-grid-two"><TrendChart data={trend} granularity={granularity} onGranularityChange={onGranularityChange} /><AppraisalPanel data={data.appraisalStatus} /></div>
    <div className="pms-content-grid pms-grid-three"><RatingDistribution rows={data.ratingDistribution} /><DepartmentPerformance rows={data.departmentPerformance} /><TopPerformers rows={data.topPerformers} /></div>
    <div className="pms-content-grid pms-grid-three"><PipPanel data={data.pip} /><DashboardDeadlines rows={data.deadlines} /><DashboardTable rows={data.departmentGoals} /></div>
    <DashboardQuickActions onNavigate={onNavigate} />
  </>
}

export default function PMSDashboardPage() {
  const { tab: routeTab } = useParams()
  const navigate = useNavigate()
  const activeTab = tabs.some((item) => item.id === routeTab) ? routeTab : 'overview'
  const [range, setRange] = useState(defaultRange)
  const [department, setDepartment] = useState('')
  const [overview, setOverview] = useState(null)
  const [trend, setTrend] = useState(null)
  const [granularity, setGranularity] = useState('DAILY')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  const [exporting, setExporting] = useState(false)
  const params = useMemo(() => ({ ...range, ...(department ? { department } : {}) }), [range, department])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    Promise.all([pmsService.dashboard(params, controller.signal), pmsService.trend({ ...params, granularity }, controller.signal)])
      .then(([dashboard, points]) => { if (!controller.signal.aborted) { setOverview(dashboard); setTrend(points); setLoading(false) } })
      .catch((e) => { if (!controller.signal.aborted) { setError(getApiErrorMessage(e, 'PMS dashboard could not be loaded.')); setLoading(false) } })
    return () => controller.abort()
  }, [params, granularity, revision])

  async function exportReport() {
    setExporting(true)
    try { await pmsService.exportDashboard(params) } catch (e) { setError(getApiErrorMessage(e, 'Report export failed.')) } finally { setExporting(false) }
  }

  return <PageLayout eyebrow="PERFORMANCE MANAGEMENT" title="PMS Dashboard" description="Performance at a glance across goals, appraisals and teams." className="pms-page">
    <PageControls range={range} onRangeChange={setRange} department={department} onDepartmentChange={setDepartment} onExport={exportReport} exporting={exporting} tabs={tabs} activeTab={activeTab} onTabChange={(next) => navigate(`/performance/pms-dashboard/${next}`)} />
    {loading ? <div className="pms-loading-grid"><div /><div /><div /><div /></div> : error ? <div className="pms-error"><strong>Performance data unavailable</strong><p>{error}</p><button className="pms-button pms-button-secondary" onClick={() => setRevision((v) => v + 1)}>Retry</button></div> : overview && <>
      <MetricCards values={overview.kpis} labels={metricLabels} />
      <DashboardContent tab={activeTab} data={overview} trend={trend} granularity={granularity} onGranularityChange={setGranularity} onNavigate={navigate} />
      <PageFooter />
    </>}
  </PageLayout>
}
