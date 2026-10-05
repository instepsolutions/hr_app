import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import {
  Activity, BadgeCheck, BookOpenCheck, CalendarDays, Check, CircleHelp, ClipboardCheck, Download,
  FileText, Lightbulb, MessageSquareText, Search, Send, Star, Target, UsersRound,
} from 'lucide-react'
import { DataTable, EmptyState, ErrorState, LoadingState, Modal, PageLayout, StatCard } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { pmsService } from '../services/pmsService'
import './pms-workspace.css'

const tabs = [
  { id: 'overview', label: 'Overview' }, { id: 'submission-status', label: 'Submission Status' },
  { id: 'rating-analysis', label: 'Self Rating Analysis' }, { id: 'department-view', label: 'Department View' },
  { id: 'my-appraisal', label: 'My Appraisal' }, { id: 'feedback', label: 'Feedback & Comments' },
  { id: 'timeline', label: 'Appraisal Timeline' },
]
const ratingLabels = ['Strongly disagree · Needs significant improvement', 'Disagree', 'Neutral · Meets some expectations', 'Agree · Meets expectations', 'Strongly agree · Exceeds expectations']
const colors = ['#d45e58', '#e79739', '#718aa2', '#3180cf', '#1d9a74']
const emptyForm = { achievements: '', challenges: '', keyContributions: '', developmentNeeds: '', overallRating: '', employeeComments: '', competencies: [], goals: [], supportingDocuments: [] }
const number = (value) => Number(value || 0).toLocaleString()
const dateTime = (value) => value ? new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : '—'
const displayDate = (value) => value ? new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(`${String(value).slice(0, 10)}T00:00:00`)) : '—'

function Panel({ title, children, className = '', action }) {
  return <section className={`pms-panel ${className}`}><header className="pms-panel-heading"><h2>{title}</h2>{action}</header>{children}</section>
}

function Status({ value }) {
  const normalized = String(value || 'NOT_STARTED').toLowerCase().replaceAll('_', '-')
  return <span className={`pms-status status-${normalized}`}>{String(value || 'NOT_STARTED').replaceAll('_', ' ')}</span>
}

function RatingPicker({ value, onChange, disabled }) {
  return <div className="appraisal-rating-picker" role="radiogroup" aria-label="Rating from one to five">
    {[1, 2, 3, 4, 5].map((rating) => <button key={rating} type="button" role="radio" aria-checked={Number(value) === rating} className={Number(value) === rating ? 'is-selected' : ''} onClick={() => onChange(String(rating))} disabled={disabled} title={ratingLabels[rating - 1]}><Star size={15} fill={Number(value) >= rating ? 'currentColor' : 'none'} />{rating}</button>)}
  </div>
}

function AppraisalChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return <div className="pms-workspace-tooltip"><strong>{label || payload[0].name}</strong>{payload.map((item) => <span key={item.dataKey}>{item.name}: {number(item.value)}</span>)}</div>
}

function MyAppraisalCard({ appraisal, onOpen }) {
  if (!appraisal) return <Panel title="My self appraisal"><EmptyState title="No appraisal is assigned" detail="Contact HR to check the active cycle for your department." /></Panel>
  const data = appraisal.data || {}
  const goals = Array.isArray(data.goals) ? data.goals : []
  const reviewed = goals.filter((goal) => goal.reviewed).length
  const competencies = Array.isArray(data.competencies) ? data.competencies.filter((item) => item.rating).length : 0
  return <Panel title="My self appraisal" className="pms-workspace-my-card" action={<button className="pms-button pms-button-primary" onClick={onOpen}>{appraisal.status === 'NOT_STARTED' ? 'Start appraisal' : appraisal.status === 'SUBMITTED' ? 'View appraisal' : 'Continue appraisal'}</button>}>
    <div className="appraisal-employee-summary"><span className="appraisal-avatar">{appraisal.employeeName?.split(' ').map((part) => part[0]).slice(0, 2).join('')}</span><div><strong>{appraisal.employeeName}</strong><small>{appraisal.employeeCode} · {appraisal.designation}</small><small>{appraisal.department}</small></div><Status value={appraisal.status} /></div>
    <div className="appraisal-summary-stats"><div><Target size={15} /><span>Goals reviewed</span><strong>{reviewed}/{goals.length || (appraisal.goals || []).length}</strong></div><div><BadgeCheck size={15} /><span>Competencies</span><strong>{competencies}/4</strong></div><div><MessageSquareText size={15} /><span>Overall comments</span><strong>{data.employeeComments?.trim() ? 'Added' : 'Pending'}</strong></div></div>
    {appraisal.submittedAt && <small className="appraisal-submitted-at">Submitted {dateTime(appraisal.submittedAt)}</small>}
  </Panel>
}

function Overview({ report, appraisal, onTab, onAction }) {
  const summary = report?.summary || {}
  const statusRows = [{ name: 'Submitted', value: summary.submitted || 0 }, { name: 'Pending', value: summary.pending || 0 }, { name: 'Not started', value: summary.notStarted || 0 }]
  const ratingRows = report?.ratingDistribution || []
  const departments = report?.departments || []
  const recent = report?.recent || []
  const cycle = report?.cycles?.[0]
  const stages = cycle?.stages || []
  return <>
    <div className="pms-content-grid pms-grid-three appraisal-top-grid">
      <Panel title="Submission status" className="pms-workspace-donut"><div className="pms-workspace-donut-layout"><div className="pms-workspace-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={statusRows} dataKey="value" nameKey="name" innerRadius="62%" outerRadius="88%" paddingAngle={2} stroke="none">{statusRows.map((row, index) => <Cell key={row.name} fill={['#1d9a74', '#ec9a35', '#aab5bf'][index]} />)}</Pie><Tooltip content={<AppraisalChartTooltip />} /></PieChart></ResponsiveContainer><div className="pms-donut-center"><strong>{number(summary.totalEmployees)}</strong><span>Employees</span></div></div><div className="pms-legend">{statusRows.map((row, index) => <div className="pms-legend-row" key={row.name}><span><i style={{ background: ['#1d9a74', '#ec9a35', '#aab5bf'][index] }} />{row.name}</span><strong>{number(row.value)}</strong></div>)}</div></div></Panel>
      <Panel title="Self rating distribution"><div className="pms-chart-area"><ResponsiveContainer width="100%" height="100%"><BarChart data={ratingRows} margin={{ top: 12, right: 8, left: -18, bottom: 4 }}><CartesianGrid vertical={false} stroke="#edf0f3" /><XAxis dataKey="rating" tick={{ fill: '#718092', fontSize: 9 }} tickLine={false} axisLine={false} /><YAxis allowDecimals={false} tick={{ fill: '#8793a1', fontSize: 9 }} tickLine={false} axisLine={false} /><Tooltip content={<AppraisalChartTooltip />} /><Bar dataKey="count" name="Employees" fill="#397bd0" radius={[3, 3, 0, 0]} barSize={23} /></BarChart></ResponsiveContainer></div><div className="appraisal-rating-axis">{[1, 2, 3, 4, 5].map((rating) => <small key={rating}>{rating}: {ratingLabels[rating - 1].split(' · ')[0]}</small>)}</div></Panel>
      <Panel title="Average self rating by department"><div className="pms-chart-area pms-workspace-bar"><ResponsiveContainer width="100%" height="100%"><BarChart data={departments} layout="vertical" margin={{ top: 6, right: 16, left: 1, bottom: 0 }}><CartesianGrid horizontal={false} stroke="#edf0f3" /><XAxis type="number" domain={[0, 5]} tick={{ fontSize: 8 }} tickLine={false} axisLine={false} /><YAxis type="category" dataKey="department" width={92} tick={{ fill: '#637589', fontSize: 8 }} tickLine={false} axisLine={false} /><Tooltip content={<AppraisalChartTooltip />} /><Bar dataKey="averageRating" name="Average rating" fill="#1d9a74" radius={[0, 3, 3, 0]} barSize={12} /></BarChart></ResponsiveContainer></div></Panel>
    </div>
    <div className="pms-content-grid pms-grid-three appraisal-table-grid">
      <Panel title="Department submission details" className="appraisal-wide-panel"><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>Department</th><th>Total</th><th>Submitted</th><th>Pending</th><th>Submission rate</th></tr></thead><tbody>{departments.map((row) => <tr key={row.department}><td>{row.department}</td><td>{number(row.totalEmployees)}</td><td>{number(row.submitted)}</td><td>{number(row.pending)}</td><td>{Number(row.submissionRate || 0).toFixed(1)}%</td></tr>)}</tbody></table></div></Panel>
      <Panel title="Recent submissions" className="appraisal-wide-panel"><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>Employee</th><th>Department</th><th>Submission date</th><th>Status</th></tr></thead><tbody>{recent.map((row, index) => <tr key={`${row.employeeName}-${index}`}><td>{row.employeeName}</td><td>{row.department}</td><td>{dateTime(row.submittedAt)}</td><td><Status value={row.status} /></td></tr>)}</tbody></table></div><button className="pms-text-button" onClick={() => onTab('submission-status')}>View submissions</button></Panel>
      <Panel title="Appraisal timeline"><div className="appraisal-timeline-list">{stages.map((stage, index) => <div key={stage.stageCode} className={index === 0 ? 'is-current' : ''}><i /><span><strong>{stage.stageCode.replaceAll('_', ' ')}</strong><small>{displayDate(stage.startDate)} – {displayDate(stage.endDate)}</small></span></div>)}{!stages.length && <EmptyState title="No active cycle" />}</div></Panel>
    </div>
    <div className="pms-content-grid appraisal-bottom-grid">
      <MyAppraisalCard appraisal={appraisal} onOpen={() => onTab('my-appraisal')} />
      <Panel title="Key insights" className="appraisal-insights-panel"><div>{(report?.insights || []).map((insight) => <p key={insight.label}><Lightbulb size={13} /><span>{insight.label}</span><strong>{insight.value}</strong></p>)}</div></Panel>
      <Panel title="Quick actions"><div className="pms-workspace-actions"><button onClick={() => onTab('my-appraisal')}><ClipboardCheck size={15} />{appraisal?.status === 'SUBMITTED' ? 'View my appraisal' : appraisal?.status === 'DRAFT' ? 'Continue self appraisal' : 'Start self appraisal'}</button><button onClick={() => onAction('goals')}><Target size={15} />View my goals</button><button onClick={() => onTab('feedback')}><MessageSquareText size={15} />Add comments</button><button onClick={() => onAction('download')}><Download size={15} />Download self appraisal report</button><button onClick={() => onAction('guidelines')}><BookOpenCheck size={15} />View guidelines</button><button onClick={() => onAction('support')}><CircleHelp size={15} />Contact HR support</button></div></Panel>
    </div>
  </>
}

function SelfAppraisalForm({ appraisal, onSaved, onSubmitted, onNotify }) {
  const [values, setValues] = useState(() => ({ ...emptyForm, ...(appraisal?.data || {}), goals: (appraisal?.data?.goals || appraisal?.goals || []).map((goal) => ({ ...goal })), competencies: appraisal?.data?.competencies || [] }))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [confirmSubmit, setConfirmSubmit] = useState(false)
  const [files, setFiles] = useState([])
  const locked = appraisal?.status === 'SUBMITTED'
  const overallRating = Number(values.overallRating || 0)
  const weightedScore = useMemo(() => {
    const rated = values.goals.filter((goal) => Number(goal.rating) >= 1 && Number(goal.rating) <= 5)
    const totalWeight = rated.reduce((sum, goal) => sum + Number(goal.weightage || 0), 0)
    return totalWeight ? rated.reduce((sum, goal) => sum + Number(goal.rating) * Number(goal.weightage || 0), 0) / totalWeight : 0
  }, [values.goals])
  const updateField = (field, value) => setValues((current) => ({ ...current, [field]: value }))
  const updateGoal = (index, field, value) => setValues((current) => ({ ...current, goals: current.goals.map((goal, goalIndex) => goalIndex === index ? { ...goal, [field]: value } : goal) }))
  const achievement = (goal) => Number(goal.target) ? Math.min(200, Number(goal.actual || 0) * 100 / Number(goal.target)) : 0
  async function saveDraft() {
    setSaving(true); setError('')
    try {
      const response = await pmsService.saveMyAppraisal({ ...values, supportingDocuments: [...new Set([...(values.supportingDocuments || []), ...files.map((file) => file.name)])] })
      setValues((current) => ({ ...current, supportingDocuments: [...new Set([...(current.supportingDocuments || []), ...files.map((file) => file.name)])] }))
      setFiles([])
      onSaved(response)
      onNotify({ type: 'success', message: 'Appraisal draft saved.' })
      return true
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Appraisal draft could not be saved.')); return false }
    finally { setSaving(false) }
  }
  async function submit() {
    setSaving(true); setError('')
    try { if (!await saveDraft()) return; const response = await pmsService.submitMyAppraisal(); onSubmitted(response); setConfirmSubmit(false) }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Appraisal could not be submitted.')) }
    finally { setSaving(false) }
  }
  return <>
    <Panel title="Employee information" className="pms-workspace-mapping"><div className="appraisal-employee-fields"><div><small>Employee</small><strong>{appraisal?.employeeName || 'Employee'}</strong></div><div><small>Employee ID</small><strong>{appraisal?.employeeCode || '—'}</strong></div><div><small>Designation</small><strong>{appraisal?.designation || '—'}</strong></div><div><small>Department</small><strong>{appraisal?.department || '—'}</strong></div><div><small>Status</small><Status value={appraisal?.status} /></div><div><small>Manager</small><strong>{appraisal?.manager || '—'}</strong></div></div></Panel>
    {error && <div className="pms-form-error" role="alert">{error}</div>}
    <Panel title="Goals, KRA and KPI review" className="pms-workspace-mapping"><div className="appraisal-goal-list">{values.goals.map((goal, index) => <article className="appraisal-goal-item" key={goal.goalId || index}><header><div><strong>{goal.title || goal.goal || `Goal ${index + 1}`}</strong><small>{goal.kra || 'KRA not linked'} · {goal.kpi || 'KPI not linked'}</small></div><label className="appraisal-reviewed"><input type="checkbox" checked={Boolean(goal.reviewed)} onChange={(event) => updateGoal(index, 'reviewed', event.target.checked)} disabled={locked} />Reviewed</label></header><div className="appraisal-goal-fields"><label>Target<input type="number" value={goal.target ?? ''} readOnly /></label><label>Actual<input type="number" value={goal.actual ?? ''} onChange={(event) => updateGoal(index, 'actual', event.target.value)} disabled={locked} /></label><label>Achievement %<output>{achievement(goal).toFixed(1)}%</output></label><label>Weightage %<input type="number" value={goal.weightage ?? 0} onChange={(event) => updateGoal(index, 'weightage', event.target.value)} disabled={locked} /></label><label>Rating<RatingPicker value={goal.rating} onChange={(value) => updateGoal(index, 'rating', value)} disabled={locked} /></label><label className="appraisal-goal-comment">Employee comments<textarea rows="2" maxLength="500" value={goal.comments || ''} onChange={(event) => updateGoal(index, 'comments', event.target.value)} disabled={locked} /></label></div></article>)}{!values.goals.length && <EmptyState title="No active goals are assigned" detail="Goal assignments from Goal Management will appear here." />}</div></Panel>
    <Panel title="Achievements and reflection" className="pms-workspace-mapping"><div className="pms-workspace-form appraisal-reflection-form"><label>Achievements<textarea rows="3" value={values.achievements} onChange={(event) => updateField('achievements', event.target.value)} disabled={locked} /></label><label>Challenges<textarea rows="3" value={values.challenges} onChange={(event) => updateField('challenges', event.target.value)} disabled={locked} /></label><label>Key contributions<textarea rows="3" value={values.keyContributions} onChange={(event) => updateField('keyContributions', event.target.value)} disabled={locked} /></label><label>Training & development needs<textarea rows="3" value={values.developmentNeeds} onChange={(event) => updateField('developmentNeeds', event.target.value)} disabled={locked} /></label></div></Panel>
    <Panel title="Skills and competencies" className="pms-workspace-mapping"><div className="appraisal-competencies">{(values.competencies.length ? values.competencies : ['Communication', 'Collaboration', 'Problem solving', 'Leadership']).map((competency, index) => { const item = typeof competency === 'string' ? { name: competency, rating: '' } : competency; return <div key={item.name}><strong>{item.name}</strong><RatingPicker value={item.rating} onChange={(rating) => updateField('competencies', (values.competencies.length ? values.competencies : ['Communication', 'Collaboration', 'Problem solving', 'Leadership'].map((name) => ({ name, rating: '' }))).map((entry, entryIndex) => entryIndex === index ? { ...(typeof entry === 'string' ? { name: entry } : entry), rating } : (typeof entry === 'string' ? { name: entry, rating: '' } : entry)))} disabled={locked} /></div> })}</div></Panel>
    <Panel title="Overall self rating" className="pms-workspace-mapping"><div className="appraisal-overall-rating"><div><strong>{overallRating || '—'}<small> / 5</small></strong><span>{overallRating ? ratingLabels[overallRating - 1] : 'Select your overall rating'}</span><RatingPicker value={values.overallRating} onChange={(value) => updateField('overallRating', value)} disabled={locked} /></div><div><small>Weighted score</small><strong>{weightedScore ? `${weightedScore.toFixed(2)} / 5` : '—'}</strong><small>Calculated from rated goals</small></div></div><label className="appraisal-comments-field">Employee comments<textarea rows="4" maxLength="2000" value={values.employeeComments} onChange={(event) => updateField('employeeComments', event.target.value)} disabled={locked} /></label></Panel>
    <Panel title="Supporting documents" className="pms-workspace-mapping"><div className="appraisal-documents"><label className="pms-button pms-button-secondary"><FileText size={14} />Attach files<input type="file" multiple onChange={(event) => setFiles((current) => [...current, ...Array.from(event.target.files || [])])} disabled={locked} /></label><div>{[...(values.supportingDocuments || []), ...files.map((file) => file.name)].map((name, index) => <span key={`${name}-${index}`}><FileText size={13} />{name}</span>)}</div><small>File names are saved with the appraisal; file contents are not uploaded to the current backend.</small></div></Panel>
    <div className="appraisal-form-actions">{locked ? <><Status value="SUBMITTED" /><button className="pms-button pms-button-secondary" onClick={async () => { try { const response = await pmsService.withdrawMyAppraisal(); onSaved(response); onNotify({ type: 'success', message: 'Submission withdrawn and reopened for editing.' }) } catch (requestError) { setError(getApiErrorMessage(requestError, 'This appraisal cannot be withdrawn now.')) } }}>Withdraw submission</button></> : <><button className="pms-button pms-button-secondary" onClick={saveDraft} disabled={saving}>{saving ? 'Saving…' : 'Save draft'}</button><button className="pms-button pms-button-primary" onClick={() => setConfirmSubmit(true)} disabled={saving || !overallRating}>Submit appraisal <Send size={14} /></button></>}</div>
    {confirmSubmit && <Modal title="Submit self appraisal?" description="Your responses will be locked after submission." onClose={() => setConfirmSubmit(false)}><div className="confirm-content"><span className="confirm-symbol"><ClipboardCheck size={19} /></span><p>Review your ratings and comments. Once submitted, you can only edit this appraisal by withdrawing it during the self-appraisal window.</p></div><footer className="modal-actions"><button className="button button-secondary" disabled={saving} onClick={() => setConfirmSubmit(false)}>Go back</button><button className="button button-primary" disabled={saving} onClick={submit}>{saving ? 'Submitting…' : 'Confirm submission'}</button></footer></Modal>}
  </>
}

function CommentsPanel({ appraisal, comments, onAdd }) {
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  async function submit(event) {
    event.preventDefault()
    if (!text.trim() || !appraisal?.appraisalId) return
    setSending(true)
    try { await onAdd(text); setText('') }
    finally { setSending(false) }
  }
  return <Panel title="Feedback & comments" className="pms-workspace-mapping"><div className="appraisal-comment-thread">{comments.map((comment) => <article key={comment.commentId}><span className={`comment-role role-${comment.authorRole.toLowerCase()}`}>{comment.authorRole}</span><div><header><strong>{comment.authorName}</strong><time>{dateTime(comment.createdAt)}</time></header><p>{comment.commentText}</p></div></article>)}{!comments.length && <EmptyState title="No feedback yet" detail="Employee, manager and HR comments will appear here." />}</div><form className="appraisal-comment-form" onSubmit={submit}><textarea rows="3" maxLength="2000" value={text} onChange={(event) => setText(event.target.value)} placeholder="Add a comment" disabled={!appraisal?.appraisalId} /><button className="pms-button pms-button-primary" disabled={sending || !text.trim() || !appraisal?.appraisalId}>{sending ? 'Adding…' : 'Add comment'}<Send size={13} /></button></form></Panel>
}

export default function SelfAppraisalPage({ notify }) {
  const { tab: routeTab } = useParams()
  const navigate = useNavigate()
  const activeTab = tabs.some((tab) => tab.id === routeTab) ? routeTab : 'overview'
  const [departmentId, setDepartmentId] = useState('')
  const [departments, setDepartments] = useState([])
  const [report, setReport] = useState(null)
  const [appraisal, setAppraisal] = useState(null)
  const [comments, setComments] = useState([])
  const [cycleId, setCycleId] = useState('')
  const [timeline, setTimeline] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  const [modal, setModal] = useState(null)
  const [search, setSearch] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true); setError('')
    Promise.all([
      pmsService.appraisalOverview({ ...(departmentId ? { departmentId: Number(departmentId) } : {}) }, controller.signal),
      pmsService.myAppraisal(controller.signal).catch((requestError) => {
        if (requestError.response?.status === 404) return null
        throw requestError
      }),
      pmsService.departments(controller.signal),
    ]).then(([overview, myAppraisal, departmentRows]) => {
      if (controller.signal.aborted) return
      setReport(overview); setAppraisal(myAppraisal); setDepartments(departmentRows || [])
      setComments(myAppraisal.comments || [])
      const cycles = overview.cycles || []
      const chosen = cycles.find((cycle) => String(cycle.cycleId) === cycleId) || cycles[0]
      if (chosen) { setCycleId(String(chosen.cycleId)); setTimeline(chosen.stages || []) }
      setLoading(false)
    }).catch((requestError) => {
      if (!controller.signal.aborted) { setError(getApiErrorMessage(requestError, 'Self appraisal data could not be loaded.')); setLoading(false) }
    })
    return () => controller.abort()
  }, [departmentId, revision])

  const selectedCycle = (report?.cycles || []).find((cycle) => String(cycle.cycleId) === cycleId) || report?.cycles?.[0]
  const visibleDepartments = (report?.departments || []).filter((row) => !search || row.department.toLowerCase().includes(search.toLowerCase()))
  const visibleRecent = (report?.recent || []).filter((row) => !search || `${row.employeeName} ${row.department}`.toLowerCase().includes(search.toLowerCase()))
  const goTab = (tab) => navigate(`/performance/self-appraisal/${tab}`)
  const refresh = () => setRevision((value) => value + 1)

  async function action(actionName) {
    if (actionName === 'goals') { navigate('/performance/goal-management/my'); return }
    if (actionName === 'download') {
      try { await pmsService.exportAppraisals({ ...(departmentId ? { departmentId } : {}) }) }
      catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Self appraisal report could not be exported.') }) }
      return
    }
    if (actionName === 'guidelines') { setModal('guidelines'); return }
    notify({ type: 'success', message: 'Contact your HR administrator through your organization’s support channel.' })
  }

  async function addComment(text) {
    try {
      await pmsService.addAppraisalComment(appraisal.appraisalId, { commentText: text })
      const next = await pmsService.appraisalComments(appraisal.appraisalId)
      setComments(next); notify({ type: 'success', message: 'Comment added.' })
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Comment could not be added.') }); throw requestError }
  }

  async function saveTimeline() {
    try {
      const updated = await pmsService.updateAppraisalTimeline(selectedCycle.cycleId, timeline)
      setTimeline(updated); notify({ type: 'success', message: 'Appraisal timeline updated.' }); refresh()
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Timeline could not be saved.') }) }
  }

  function updateTimeline(index, field, value) {
    setTimeline((current) => current.map((stage, stageIndex) => stageIndex === index ? { ...stage, [field]: value } : stage))
  }

  const summary = report?.summary || {}
  return <PageLayout eyebrow="PERFORMANCE MANAGEMENT" title="Self Appraisal" description="Reflect on your performance and track submission progress across the current cycle." className="pms-page pms-workspace-page">
    <div className="pms-workspace-toolbar"><label className="pms-search"><Search size={14} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search employees or departments" /></label><label className="pms-workspace-dept-filter"><UsersRound size={14} /><select aria-label="Appraisal department filter" value={departmentId} onChange={(event) => setDepartmentId(event.target.value)}><option value="">All departments</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.departmentName}</option>)}</select></label><label className="pms-workspace-dept-filter"><CalendarDays size={14} /><select aria-label="Appraisal cycle" value={cycleId} onChange={(event) => { setCycleId(event.target.value); const cycle = report?.cycles?.find((item) => String(item.cycleId) === event.target.value); if (cycle) setTimeline(cycle.stages || []) }}><option value="">Select cycle</option>{(report?.cycles || []).map((cycle) => <option key={cycle.cycleId} value={cycle.cycleId}>{cycle.cycleName}</option>)}</select></label><button className="pms-button pms-button-secondary" onClick={() => action('download')}><Download size={14} />Export report</button></div>
    <nav className="pms-tabs pms-workspace-tabs" aria-label="Self appraisal views">{tabs.map((tab) => <button key={tab.id} className={activeTab === tab.id ? 'is-active' : ''} onClick={() => goTab(tab.id)}>{tab.label}</button>)}</nav>
    {loading ? <LoadingState label="Loading appraisal cycle and employee data" /> : error ? <ErrorState message={error} onRetry={refresh} /> : <>
      <section className="pms-metric-grid pms-workspace-metrics appraisal-metrics">
        <StatCard label="Total employees" value={number(summary.totalEmployees)} note="Eligible workforce" icon={UsersRound} accent="blue" />
        <StatCard label="Self appraisals submitted" value={number(summary.submitted)} note="Submitted in active cycles" icon={Check} accent="green" />
        <StatCard label="Pending submissions" value={number(summary.pending)} note="Draft or not started" icon={CalendarDays} accent="amber" />
        <StatCard label="Submission rate" value={`${Number(summary.submissionRate || 0).toFixed(1)}%`} note="Submitted / eligible" icon={Activity} accent="blue" />
        <StatCard label="Average self rating" value={`${Number(summary.averageSelfRating || 0).toFixed(2)} / 5`} note="Submitted ratings" icon={Star} accent="rose" />
      </section>
      {activeTab === 'overview' && <Overview report={report} appraisal={appraisal} onTab={goTab} onAction={action} />}
      {activeTab === 'submission-status' && <div className="pms-content-grid pms-grid-two"><Panel title="Submission status by department"><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>Department</th><th>Total employees</th><th>Submitted</th><th>Pending</th><th>Submission rate</th></tr></thead><tbody>{visibleDepartments.map((row) => <tr key={row.department}><td>{row.department}</td><td>{number(row.totalEmployees)}</td><td>{number(row.submitted)}</td><td>{number(row.pending)}</td><td>{Number(row.submissionRate || 0).toFixed(1)}%</td></tr>)}</tbody></table></div></Panel><Panel title="Recent submissions"><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>Employee</th><th>Department</th><th>Submitted</th><th>Status</th></tr></thead><tbody>{visibleRecent.map((row, index) => <tr key={`${row.employeeName}-${index}`}><td>{row.employeeName}</td><td>{row.department}</td><td>{dateTime(row.submittedAt)}</td><td><Status value={row.status} /></td></tr>)}</tbody></table></div></Panel></div>}
      {activeTab === 'rating-analysis' && <div className="pms-content-grid pms-grid-two"><Panel title="Rating distribution"><div className="pms-chart-area"><ResponsiveContainer width="100%" height="100%"><BarChart data={report?.ratingDistribution || []}><CartesianGrid vertical={false} stroke="#edf0f3" /><XAxis dataKey="rating" tickLine={false} axisLine={false} /><YAxis allowDecimals={false} tickLine={false} axisLine={false} /><Tooltip content={<AppraisalChartTooltip />} /><Bar dataKey="count" name="Employees" radius={[3, 3, 0, 0]}>{(report?.ratingDistribution || []).map((row, index) => <Cell key={row.rating} fill={colors[index]} />)}</Bar></BarChart></ResponsiveContainer></div></Panel><Panel title="Average rating by department"><div className="pms-chart-area pms-workspace-bar"><ResponsiveContainer width="100%" height="100%"><BarChart data={report?.departments || []} layout="vertical" margin={{ left: 0, right: 12 }}><CartesianGrid horizontal={false} stroke="#edf0f3" /><XAxis type="number" domain={[0, 5]} /><YAxis type="category" dataKey="department" width={100} tick={{ fontSize: 8 }} /><Tooltip content={<AppraisalChartTooltip />} /><Bar dataKey="averageRating" name="Average rating" fill="#1d9a74" radius={[0, 3, 3, 0]} /></BarChart></ResponsiveContainer></div></Panel></div>}
      {activeTab === 'department-view' && <Panel title="Department appraisal view"><div className="pms-workspace-library-filters"><label className="pms-search"><Search size={14} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Filter departments" /></label><span>{visibleDepartments.length} departments</span></div><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>Department</th><th>Total employees</th><th>Submitted</th><th>Pending</th><th>Submission rate</th><th>Average rating</th></tr></thead><tbody>{visibleDepartments.map((row) => <tr key={row.department}><td>{row.department}</td><td>{number(row.totalEmployees)}</td><td>{number(row.submitted)}</td><td>{number(row.pending)}</td><td>{Number(row.submissionRate || 0).toFixed(1)}%</td><td>{Number(row.averageRating || 0).toFixed(2)}</td></tr>)}</tbody></table></div></Panel>}
      {activeTab === 'my-appraisal' && (appraisal ? <SelfAppraisalForm key={appraisal.appraisalId || 'mine'} appraisal={appraisal} onSaved={() => refresh()} onSubmitted={() => { notify({ type: 'success', message: 'Self appraisal submitted.' }); refresh() }} onNotify={notify} /> : <Panel title="My appraisal"><EmptyState title="No employee profile is linked" detail="Ask HR to link your login to an employee record before starting a self appraisal." /></Panel>)}
      {activeTab === 'feedback' && <CommentsPanel appraisal={appraisal} comments={comments} onAdd={addComment} />}
      {activeTab === 'timeline' && <Panel title={`${selectedCycle?.cycleName || 'Appraisal'} · Timeline`} className="pms-workspace-mapping" action={<button className="pms-button pms-button-primary" onClick={saveTimeline} disabled={!selectedCycle}>Save timeline</button>}><div className="appraisal-timeline-editor">{timeline.map((stage, index) => <article key={stage.stageCode}><span className="timeline-step">{index + 1}</span><strong>{stage.stageCode.replaceAll('_', ' ')}</strong><label>Start<input type="date" value={String(stage.startDate).slice(0, 10)} onChange={(event) => updateTimeline(index, 'startDate', event.target.value)} /></label><label>End<input type="date" value={String(stage.endDate).slice(0, 10)} onChange={(event) => updateTimeline(index, 'endDate', event.target.value)} /></label></article>)}{!timeline.length && <EmptyState title="Select a cycle with a configured timeline" />}</div></Panel>}
      <footer className="pms-footer">Northstar People Operations <span>·</span> Performance Management</footer>
    </>}
    {modal === 'guidelines' && <Modal title="Self appraisal guidelines" description={selectedCycle?.cycleName || 'Current appraisal cycle'} onClose={() => setModal(null)}><div className="appraisal-guidelines"><p>Use specific examples and measurable outcomes for each response.</p><p>Rate yourself using the five-point scale and add context for ratings that need explanation.</p><p>Save a draft before submitting. Submitted responses are locked unless withdrawn during the self-appraisal period.</p><p>Manager and HR feedback will appear in Feedback & Comments.</p></div></Modal>}
  </PageLayout>
}
