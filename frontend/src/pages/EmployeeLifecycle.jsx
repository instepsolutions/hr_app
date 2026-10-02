import { useEffect, useMemo, useState } from 'react'
import { Activity, ArrowUpRight, Award, BookOpenCheck, BriefcaseBusiness, CalendarDays, LogOut, UserRoundPlus, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ErrorState, LoadingState, PageLayout, StatCard } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { lifecycleService } from '../services/lifecycleService'

const lifecycleStages = [
  { name: 'Attract & Recruit', icon: UserRoundPlus, tone: 'blue' },
  { name: 'Onboard', icon: BriefcaseBusiness, tone: 'green' },
  { name: 'Engage & Manage', icon: Activity, tone: 'amber' },
  { name: 'Develop', icon: BookOpenCheck, tone: 'rose' },
  { name: 'Retain & Reward', icon: Award, tone: 'blue' },
  { name: 'Exit', icon: LogOut, tone: 'muted' },
]

function eventLabel(type) {
  return (type || 'EVENT').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
}

function dateLabel(date) {
  if (!date) return '—'
  return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(`${date}T00:00:00`))
}

export default function EmployeeLifecycle() {
  const [statistics, setStatistics] = useState(null)
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    Promise.all([lifecycleService.statistics(controller.signal), lifecycleService.events(controller.signal)])
      .then(([summary, timeline]) => {
        setStatistics(summary)
        setEvents(timeline)
        setError('')
        setLoading(false)
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(getApiErrorMessage(requestError, 'Lifecycle data could not be loaded.'))
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [refresh])

  const recentEvents = useMemo(() => [...events].sort((first, second) => (second.eventDate || '').localeCompare(first.eventDate || '')).slice(0, 14), [events])
  const stageCounts = statistics?.stageCounts || {}

  return <PageLayout eyebrow="PEOPLE OPERATIONS / LIFECYCLE" title="Employee lifecycle" description="Follow each stage of the employee experience, from first contact to exit." action={<span className="lifecycle-live"><i />LIVE WORKFORCE DATA</span>}>
    {error ? <section className="directory-panel"><ErrorState message={error} onRetry={() => setRefresh((value) => value + 1)} /></section> : loading ? <section className="directory-panel"><LoadingState label="Loading lifecycle statistics and events" /></section> : <>
      <section className="lifecycle-stat-grid" aria-label="Employee lifecycle statistics">
        <StatCard label="New hires" value={statistics.newHires} note="Joined in the last 30 days" icon={UserRoundPlus} accent="blue" />
        <StatCard label="Onboarding" value={statistics.onboarding} note="Employees in probation" icon={BriefcaseBusiness} accent="green" />
        <StatCard label="Active employees" value={statistics.activeEmployees} note="Currently active" icon={UsersRound} accent="blue" />
        <StatCard label="In development" value={statistics.inDevelopment} note="Training or performance activity" icon={BookOpenCheck} accent="amber" />
        <StatCard label="Due for confirmation" value={statistics.dueForConfirmation} note="Within the next 30 days" icon={CalendarDays} accent="rose" />
        <StatCard label="Exits" value={statistics.exits} note="Resigned or exited" icon={LogOut} accent="amber" />
      </section>
      <section className="lifecycle-stage-section">
        <div className="section-heading"><div><span className="page-eyebrow">EMPLOYEE EXPERIENCE</span><h2>Lifecycle stages</h2></div><span className="section-source">Counts from recorded lifecycle events</span></div>
        <div className="lifecycle-stages">{lifecycleStages.map(({ name, icon: Icon, tone }, index) => <article className={`lifecycle-stage stage-${tone}`} key={name}>
          <div className="stage-step">0{index + 1}</div><span className="stage-icon"><Icon size={18} /></span><div className="stage-name">{name}</div><strong>{Number(stageCounts[name] || 0).toLocaleString()}</strong><small>employees with events</small>{index < lifecycleStages.length - 1 && <ArrowUpRight className="stage-connector" size={15} />}
        </article>)}</div>
      </section>
      <section className="directory-panel lifecycle-events-panel">
        <div className="directory-panel-heading"><div><h2>Recent lifecycle events</h2><span>{events.length.toLocaleString()} recorded events</span></div><span className="directory-live"><i />DATABASE EVENTS</span></div>
        {recentEvents.length === 0 ? <div className="empty-state"><span className="empty-mark">—</span><strong>No lifecycle events</strong><p>Events will appear when recorded in the employee system.</p></div> : <div className="event-feed">{recentEvents.map((event) => <article className="event-row" key={event.eventId}>
          <span className={`event-dot event-${(event.eventType || '').toLowerCase()}`}><Activity size={15} /></span><span className="event-main"><strong>{eventLabel(event.eventType)}</strong><small>{event.description || 'Lifecycle event recorded'}</small></span><Link className="event-employee" to={`/employee-management/employees/${event.employeeId}`}>Employee #{event.employeeId}<ArrowUpRight size={13} /></Link><time>{dateLabel(event.eventDate)}</time>
        </article>)}</div>}
      </section>
    </>}
  </PageLayout>
}
