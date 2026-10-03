import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Activity, ArrowDownRight, ArrowRight, ArrowUpRight, BellRing, BookOpenCheck,
  BriefcaseBusiness, CalendarDays, CheckCheck, CircleDollarSign, ClipboardCheck,
  FilePlus2, FileText, HeartPulse, HelpCircle, Megaphone, MessageCircle, ShieldCheck,
  UserRoundPlus, UsersRound,
} from 'lucide-react'
import {
  Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from 'recharts'
import { ErrorState } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { dashboardService } from '../services/dashboardService'
import './DashboardPage.css'

const distributionColors = ['#3172c9', '#26a18a', '#d9953e', '#9271bf', '#d16e66', '#91a0b1']
const approvalIcons = {
  LEAVE_REQUEST: CalendarDays,
  EXPENSE_CLAIM: CircleDollarSign,
  TIMESHEET: ClipboardCheck,
  DOCUMENT: FileText,
  EXIT_CLEARANCE: ShieldCheck,
}

const quickActions = [
  { label: 'Add Employee', icon: UserRoundPlus, route: '/employee-management/directory', tone: 'blue' },
  { label: 'Apply Leave', icon: CalendarDays, route: null, tone: 'green' },
  { label: 'Attendance Regularization', icon: CheckCheck, route: null, tone: 'amber' },
  { label: 'Request Approval', icon: ClipboardCheck, target: 'approvals', tone: 'rose' },
  { label: 'Add Document', icon: FilePlus2, route: '/employee-management/manage-profiles', tone: 'blue' },
  { label: 'Payroll Process', icon: CircleDollarSign, route: null, tone: 'violet' },
  { label: 'Create Announcement', icon: Megaphone, route: null, tone: 'teal' },
  { label: 'Performance Review', icon: BookOpenCheck, route: '/employee-management/employee-lifecycle', tone: 'green' },
]

function localDateValue(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function displayDate(value, options = { day: 'numeric', month: 'short', year: 'numeric' }) {
  if (!value) return ''
  return new Intl.DateTimeFormat('en-GB', options).format(new Date(`${value.slice(0, 10)}T00:00:00`))
}

function employeeInitials(name = '') {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function SectionHeading({ title, action, actionLabel = 'View all', actionNode }) {
  return <div className="dashboard-section-heading"><h2>{title}</h2>{actionNode || (action && <button className="dashboard-view-all" onClick={action}>{actionLabel}<ArrowRight size={14} /></button>)}</div>
}

function DashboardCard({ title, action, actionLabel, actionNode, className = '', children }) {
  return <section className={`dashboard-card ${className}`}><SectionHeading title={title} action={action} actionLabel={actionLabel} actionNode={actionNode} />{children}</section>
}

function LoadingDashboard() {
  return <div className="dashboard-loading" role="status" aria-label="Loading dashboard">
    <div className="dashboard-kpi-grid">{Array.from({ length: 5 }, (_, index) => <div className="skeleton-block skeleton-kpi" key={index} />)}</div>
    <div className="dashboard-chart-grid"><div className="skeleton-block skeleton-panel" /><div className="skeleton-block skeleton-panel" /><div className="skeleton-block skeleton-panel" /></div>
    <div className="skeleton-block skeleton-panel skeleton-row" />
  </div>
}

function KpiCard({ title, value, note, icon: Icon, tone, trend, trendDirection }) {
  const TrendIcon = trendDirection === 'down' ? ArrowDownRight : ArrowUpRight
  return <article className={`dashboard-kpi kpi-${tone}`}>
    <div className="kpi-heading"><span>{title}</span><span className="kpi-icon"><Icon size={19} strokeWidth={1.9} /></span></div>
    <div className="kpi-number">{Number(value || 0).toLocaleString()}</div>
    <div className="kpi-footer"><span>{note}</span>{trend !== null && trend !== undefined && <span className={`kpi-trend ${trendDirection === 'down' ? 'trend-down' : 'trend-up'}`}><TrendIcon size={13} />{Math.abs(trend).toFixed(1)}%</span>}</div>
  </article>
}

export default function DashboardPage({ session, notify }) {
  const navigate = useNavigate()
  const [selectedDate, setSelectedDate] = useState(() => localDateValue(new Date()))
  const [monthRange, setMonthRange] = useState(6)
  const [overview, setOverview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const personName = session?.username?.toLowerCase() === 'admin'
    ? 'Admin'
    : (session?.username || 'Admin').replace(/^./, (letter) => letter.toUpperCase())
  const greeting = new Date().getHours() < 12 ? 'Good Morning' : new Date().getHours() < 17 ? 'Good Afternoon' : 'Good Evening'

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    dashboardService.overview(selectedDate, monthRange, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setOverview(data)
          setLoading(false)
        }
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(getApiErrorMessage(requestError, 'Dashboard data could not be loaded.'))
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [selectedDate, monthRange, retry])

  const totalApprovals = overview?.pendingApprovals?.reduce((total, item) => total + item.count, 0) || 0
  const greetingDate = useMemo(() => displayDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }), [selectedDate])

  function onQuickAction(action) {
    if (action.route) {
      navigate(action.route)
      return
    }
    if (action.target === 'approvals') {
      document.querySelector('#pending-approvals')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      return
    }
    notify({ type: 'error', message: `${action.label} is not available in the current Employee Management release.` })
  }

  return <main className="page-content dashboard-page">
    <header className="dashboard-welcome page-heading">
      <div>
        <div className="page-eyebrow">PEOPLE OPERATIONS</div>
        <h1>{greeting}, {personName} <span aria-hidden="true">👋</span></h1>
        <p>Here’s what’s happening in your organization today.</p>
      </div>
      <label className="dashboard-date-control"><CalendarDays size={16} /><span>{greetingDate}</span><input aria-label="Dashboard date" type="date" value={selectedDate} onChange={(event) => setSelectedDate(event.target.value)} /></label>
    </header>

    {error ? <section className="dashboard-error"><ErrorState message={error} onRetry={() => setRetry((value) => value + 1)} /></section> : loading || !overview ? <LoadingDashboard /> : <>
      <section className="dashboard-kpi-grid" aria-label="Organization metrics">
        <KpiCard title="Total Employees" value={overview.summary.totalEmployees} note="Employees on record" icon={UsersRound} tone="blue" />
        <KpiCard title="Active Employees" value={overview.summary.activeEmployees} note={`${overview.summary.totalEmployees ? (overview.summary.activeEmployees * 100 / overview.summary.totalEmployees).toFixed(1) : 0}% of workforce`} icon={Activity} tone="green" />
        <KpiCard title={`New Joiners (${displayDate(selectedDate, { month: 'short' })})`} value={overview.summary.newJoiners} note="Joined this month" icon={UserRoundPlus} tone="amber" trend={overview.summary.joinerChangePercent} trendDirection={overview.summary.joinerChangePercent < 0 ? 'down' : 'up'} />
        <KpiCard title="Pending Approvals" value={overview.summary.pendingApprovals} note="Awaiting action" icon={ClipboardCheck} tone="violet" />
        <KpiCard title="On Leave Today" value={overview.summary.onLeaveToday} note="Current employee status" icon={CalendarDays} tone="rose" />
      </section>

      <section className="dashboard-primary-grid">
        <DashboardCard title="Employee Distribution" className="distribution-card">
          <div className="distribution-layout">
            <div className="distribution-chart"><ResponsiveContainer width="100%" height="100%"><PieChart>
              <Pie data={overview.employeeDistribution} dataKey="employeeCount" nameKey="department" innerRadius="63%" outerRadius="91%" paddingAngle={2} stroke="none">
                {overview.employeeDistribution.map((item, index) => <Cell key={item.department} fill={distributionColors[index % distributionColors.length]} />)}
              </Pie>
              <Tooltip formatter={(value, name) => [`${Number(value).toLocaleString()} employees`, name]} />
            </PieChart></ResponsiveContainer><div className="distribution-center"><strong>{Number(overview.summary.totalEmployees).toLocaleString()}</strong><span>Employees</span></div></div>
            <div className="distribution-legend">{overview.employeeDistribution.map((item, index) => <div className="distribution-legend-row" key={item.department}><span className="legend-name"><i style={{ backgroundColor: distributionColors[index % distributionColors.length] }} />{item.department}</span><strong>{item.percentage.toFixed(1)}%</strong><small>{item.employeeCount.toLocaleString()}</small></div>)}</div>
          </div>
        </DashboardCard>

        <DashboardCard title="Monthly Joinees vs Exits" className="movement-card" actionNode={<select className="dashboard-range-select" value={monthRange} onChange={(event) => setMonthRange(Number(event.target.value))} aria-label="Monthly chart range"><option value={6}>Last 6 Months</option><option value={12}>Last 12 Months</option></select>}>
          <div className="chart-period-label"><span><i className="legend-joiners" />Joinees</span><span><i className="legend-exits" />Exits</span></div>
          <div className="movement-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={overview.monthlyMovements} margin={{ top: 10, right: 6, left: -22, bottom: 0 }} barGap={3}>
            <CartesianGrid vertical={false} stroke="#edf0f4" />
            <XAxis dataKey="month" tickLine={false} axisLine={false} tick={{ fill: '#8793a4', fontSize: 9 }} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fill: '#98a3b1', fontSize: 9 }} />
            <Tooltip cursor={{ fill: '#f5f8fc' }} />
            <Bar dataKey="joiners" name="Joinees" fill="#3473ca" radius={[3, 3, 0, 0]} maxBarSize={18} />
            <Bar dataKey="exits" name="Exits" fill="#e1a24d" radius={[3, 3, 0, 0]} maxBarSize={18} />
          </BarChart></ResponsiveContainer></div>
        </DashboardCard>
      </section>

      <section className="dashboard-secondary-grid">
        <DashboardCard title={`Attendance Overview (${displayDate(selectedDate, { month: 'long', year: 'numeric' })})`} className="attendance-card">
          <div className="attendance-layout">
            <div className="attendance-gauge"><ResponsiveContainer width="100%" height="100%"><PieChart>
              <Pie data={[{ name: 'Attended', value: overview.attendance.present + overview.attendance.late }, { name: 'Absent', value: overview.attendance.absent }]} dataKey="value" startAngle={180} endAngle={0} cx="50%" cy="88%" innerRadius="63%" outerRadius="92%" paddingAngle={2} stroke="none">
                <Cell fill="#2fa486" /><Cell fill="#e7a04d" />
              </Pie>
            </PieChart></ResponsiveContainer><div className="attendance-gauge-label"><strong>{overview.attendance.attendancePercent.toFixed(1)}%</strong><span>Average Attendance</span></div></div>
            <div className="attendance-legend">
              <div><span><i className="attendance-present" />Present</span><strong>{overview.attendance.present.toLocaleString()} <small>({overview.attendance.present + overview.attendance.late + overview.attendance.absent ? (overview.attendance.present * 100 / (overview.attendance.present + overview.attendance.late + overview.attendance.absent)).toFixed(1) : 0}%)</small></strong></div>
              <div><span><i className="attendance-late" />Late</span><strong>{overview.attendance.late.toLocaleString()} <small>({overview.attendance.present + overview.attendance.late + overview.attendance.absent ? (overview.attendance.late * 100 / (overview.attendance.present + overview.attendance.late + overview.attendance.absent)).toFixed(1) : 0}%)</small></strong></div>
              <div><span><i className="attendance-absent" />Absent</span><strong>{overview.attendance.absent.toLocaleString()} <small>({overview.attendance.present + overview.attendance.late + overview.attendance.absent ? (overview.attendance.absent * 100 / (overview.attendance.present + overview.attendance.late + overview.attendance.absent)).toFixed(1) : 0}%)</small></strong></div>
            </div>
          </div>
        </DashboardCard>

        <DashboardCard title="Quick Actions" className="quick-actions-card">
          <div className="quick-action-grid">{quickActions.map(({ label, icon: Icon, tone, ...action }) => <button className="quick-action" key={label} onClick={() => onQuickAction({ label, ...action })}>
            <span className={`quick-action-icon action-${tone}`}><Icon size={18} strokeWidth={1.8} /></span><span>{label}</span>
          </button>)}</div>
        </DashboardCard>

        <DashboardCard title="Pending Approvals" className="approvals-card" action={() => notify({ type: 'error', message: 'Approval processing is not part of the current Employee Management release.' })}>
            <div id="pending-approvals" className="approval-list">{overview.pendingApprovals.map((item) => {
              const Icon = approvalIcons[item.type] || HelpCircle
              return <button className="approval-row" key={item.type} onClick={() => notify({ type: 'error', message: `${item.label} processing is not part of the current Employee Management release.` })}><span className={`approval-icon approval-${item.type.toLowerCase()}`}><Icon size={16} /></span><span className="approval-label">{item.label}</span><strong className="approval-count">{item.count}</strong></button>
          })}</div>
        </DashboardCard>
      </section>

      <DashboardCard title="Recent Joiners" className="recent-joiners-card" action={() => navigate('/employee-management/directory')}>
        <div className="recent-joiners-list">{overview.recentJoiners.length ? overview.recentJoiners.map((employee) => <button className="joiner-row" key={employee.employeeId} onClick={() => navigate(`/employee-management/employees/${employee.employeeId}`)}>
          {employee.photoUrl ? <img className="employee-avatar joiner-avatar" src={employee.photoUrl} alt="" /> : <span className="employee-avatar joiner-avatar">{employeeInitials(employee.displayName)}</span>}
          <span className="joiner-name"><strong>{employee.displayName}</strong><small>{employee.subtitle}</small></span><span className="joiner-date">Joined {displayDate(employee.date, { day: 'numeric', month: 'short' })}</span>
        </button>) : <div className="dashboard-empty-row">No joiners recorded for this period.</div>}</div>
      </DashboardCard>

      <section className="dashboard-bottom-grid">
        <DashboardCard title="Important Announcements" className="announcements-card" action={() => notify({ type: 'error', message: 'Announcement management is not part of the current release.' })}>
          <div className="announcement-list">{overview.announcements.length ? overview.announcements.map((item) => <article className="announcement-row" key={item.id}><span className="announcement-mark"><Megaphone size={16} /></span><div><strong>{item.title}{item.isNew && <span className="announcement-new">NEW</span>}</strong><p>{item.summary}</p><time>{displayDate(item.publishedAt, { day: 'numeric', month: 'short', year: 'numeric' })}</time></div></article>) : <div className="dashboard-empty-row">No announcements are available.</div>}</div>
        </DashboardCard>
        <DashboardCard title="Upcoming Birthdays" className="birthdays-card" action={() => navigate('/employee-management/directory')}>
          <div className="birthday-list">{overview.birthdays.length ? overview.birthdays.map((employee) => <button className="birthday-row" key={employee.employeeId} onClick={() => navigate(`/employee-management/employees/${employee.employeeId}`)}>
            <span className="employee-avatar birthday-avatar">{employeeInitials(employee.displayName)}</span><span className="birthday-name"><strong>{employee.displayName}</strong><small>{employee.subtitle}</small></span><span className="birthday-date">{displayDate(employee.date, { day: 'numeric', month: 'short' })}</span>
          </button>) : <div className="dashboard-empty-row">No birthdays found.</div>}</div>
        </DashboardCard>
        <DashboardCard title="Upcoming Events" className="events-card" action={() => notify({ type: 'error', message: 'Event management is not part of the current release.' })}>
          <div className="event-list">{overview.events.length ? overview.events.map((event) => <article className="dashboard-event-row" key={event.id}><span className="event-date-block"><strong>{displayDate(event.startDate, { day: 'numeric' })}</strong><small>{displayDate(event.startDate, { month: 'short' })}</small></span><div><strong>{event.title}</strong><small>{event.startDate === event.endDate ? displayDate(event.startDate) : `${displayDate(event.startDate, { day: 'numeric', month: 'short' })} - ${displayDate(event.endDate, { day: 'numeric', month: 'short', year: 'numeric' })}`}</small><small>{event.location}</small></div></article>) : <div className="dashboard-empty-row">No upcoming events.</div>}</div>
        </DashboardCard>
      </section>
      <footer className="dashboard-footer">Northstar HRMS <span>·</span> Employee Management <span>·</span> © {new Date().getFullYear()}</footer>
    </>}
  </main>
}
