import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import {
  Activity, AlertTriangle, BadgeCheck, CalendarDays, Check, ClipboardCheck, Download,
  FileText, Lightbulb, MessageSquareText, Search, Send, ShieldCheck, Star, UsersRound,
} from 'lucide-react'
import { DataTable, EmptyState, ErrorState, LoadingState, Modal, PageLayout, StatCard } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { pmsService } from '../services/pmsService'
import './pms.css'
import './pms-workspace.css'
import './review-workspace.css'

const managerTabs = [
  ['overview', 'Overview'], ['pending-reviews', 'Pending Reviews'], ['completed-reviews', 'Completed Reviews'],
  ['team-performance', 'Team Performance'], ['rating-analysis', 'Rating Analysis'], ['review-status', 'Review Status'], ['review-timeline', 'Review Timeline'],
]
const hrTabs = [
  ['overview', 'Overview'], ['pending-reviews', 'Pending Reviews'], ['completed-reviews', 'Completed Reviews'],
  ['team-performance', 'Team / Department Performance'], ['rating-analysis', 'Rating Analysis'], ['review-status', 'Review Status'], ['review-timeline', 'Review Timeline'],
]
const competencies = ['Communication', 'Leadership', 'Teamwork', 'Problem Solving', 'Ownership', 'Customer Focus', 'Adaptability', 'Technical Skills']
const chartColors = ['#178365', '#e69a36', '#aeb9c4']
const ratingLabels = ['Poor', 'Below Average', 'Average', 'Good', 'Excellent']
const number = (value) => Number(value || 0).toLocaleString()
const score = (value) => value === '' || value == null ? '—' : `${Number(value).toFixed(2)} / 5`
const showDate = (value) => value ? new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(`${String(value).slice(0, 10)}T00:00:00`)) : '—'
const managerDefaults = () => ({ overallRating: '', strengths: '', areasForImprovement: '', developmentRecommendations: '', trainingRecommendations: '', promotionRecommendation: 'NO_CHANGE', pipRecommendation: 'NO', managerComments: '', kraRatings: [], kpiRatings: [], competencies: competencies.map((name) => ({ name, rating: '', comments: '' })) })
const hrDefaults = () => ({ hrRating: '', finalRating: '', underCalibration: false, calibrationReason: '', finalRecommendation: 'NO_CHANGE', incrementRecommendation: '', trainingRecommendation: '', successionPotential: '', hrComments: '', finalRemarks: '' })

function Panel({ title, children, className = '', action }) {
  return <section className={`pms-panel ${className}`}><header className="pms-panel-heading"><h2>{title}</h2>{action}</header>{children}</section>
}

function ReviewStatus({ value }) {
  const normalized = String(value || 'PENDING').toLowerCase().replaceAll('_', '-')
  return <span className={`pms-status review-status-${normalized}`}>{String(value || 'PENDING').replaceAll('_', ' ')}</span>
}

function RatingControl({ label, value, onChange, disabled = false }) {
  return <label className="review-field review-rating-field"><span>{label}</span><select value={value ?? ''} onChange={(event) => onChange(event.target.value)} disabled={disabled}><option value="">Select rating</option>{[1, 2, 3, 4, 5].map((rating) => <option key={rating} value={rating}>{rating} · {ratingLabels[rating - 1]}</option>)}</select></label>
}

function ReviewTextField({ label, value, onChange, rows = 3, disabled = false, required = false }) {
  return <label className="review-field"><span>{label}{required && <b> *</b>}</span><textarea rows={rows} value={value || ''} onChange={(event) => onChange(event.target.value)} disabled={disabled} /></label>
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return <div className="pms-workspace-tooltip"><strong>{label || payload[0].name}</strong>{payload.map((item) => <span key={item.dataKey}>{item.name}: {item.value}</span>)}</div>
}

function ReadonlyTrail({ title, data, empty = 'No appraisal information submitted.' }) {
  const rows = Object.entries(data || {}).filter(([, value]) => value !== '' && value != null && !Array.isArray(value) && typeof value !== 'object')
  return <Panel title={title} className="review-trail-panel"><div className="review-trail-grid">{rows.map(([key, value]) => <div key={key}><small>{key.replaceAll(/([A-Z])/g, ' $1').replaceAll('_', ' ')}</small><strong>{String(value)}</strong></div>)}{!rows.length && <EmptyState title={empty} />}</div></Panel>
}

function SelfGoalTrail({ record }) {
  const selfGoals = record.selfAppraisal?.goals || []
  const goals = (record.goals || []).map((goal) => {
    const self = selfGoals.find((item) => String(item.goalId) === String(goal.goalId)) || {}
    return { ...goal, actual: self.actual ?? '', rating: self.rating ?? '', comments: self.comments ?? '' }
  })
  return <Panel title="Self-appraised goals, KRAs and KPIs" className="review-form-panel"><DataTable minWidth="800px" className="review-table" columns={[{ key: 'goal', label: 'Goal / KRA / KPI' }, { key: 'target', label: 'Target' }, { key: 'actual', label: 'Achievement' }, { key: 'weight', label: 'Weightage' }, { key: 'rating', label: 'Self rating' }, { key: 'comments', label: 'Employee comments' }]}>{goals.map((goal) => <tr key={goal.goalId}><td>{goal.title || goal.kra || 'Goal'}<small className="review-table-subline">{goal.kra || 'Unlinked KRA'} · {goal.kpi || 'Unlinked KPI'}</small></td><td>{goal.target ?? '—'} {goal.unit}</td><td>{goal.actual || '—'}{Number(goal.target) ? ` · ${Math.min(200, Number(goal.actual || 0) * 100 / Number(goal.target)).toFixed(0)}%` : ''}</td><td>{goal.weightage ?? '—'}%</td><td>{score(goal.rating)}</td><td className="review-wrap-cell">{goal.comments || '—'}</td></tr>)}{!goals.length && <tr><td colSpan="6">No goal assignments.</td></tr>}</DataTable></Panel>
}

function reviewRowsForGoal(record, kind) {
  const selfGoals = record.selfAppraisal?.goals || []
  return (record.goals || []).map((goal) => {
    const self = selfGoals.find((item) => String(item.goalId) === String(goal.goalId)) || {}
    return { ...goal, actual: self.actual ?? goal.progress ?? '', selfRating: self.rating ?? '', employeeComments: self.comments ?? '' }
  }).filter((goal) => kind !== 'kpi' || goal.kpi || goal.kpiId)
}

function RatingRows({ title, rows, ratingKey, commentsKey, values, onChange, disabled }) {
  return <Panel title={title} className="review-form-panel"><div className="review-rating-rows">{rows.map((item, index) => {
    const existing = (values[ratingKey] || []).find((row) => String(row.goalId) === String(item.goalId)) || {}
    return <article key={`${item.goalId}-${index}`}><div className="review-row-title"><strong>{item.kra || item.kpi || item.title || `Goal ${index + 1}`}</strong><small>{item.description || item.title || item.unit || '—'}</small></div><span className="review-row-data"><small>Target</small><strong>{item.target ?? '—'} {item.unit}</strong></span><span className="review-row-data"><small>Actual / achievement</small><strong>{item.actual || '—'}{Number(item.target) ? ` · ${Math.min(200, Number(item.actual || 0) * 100 / Number(item.target)).toFixed(0)}%` : ''}</strong></span><span className="review-row-data"><small>Self rating</small><strong>{score(item.selfRating)}</strong></span><RatingControl label="Manager rating" value={existing.rating ?? ''} disabled={disabled} onChange={(rating) => onChange(ratingKey, item.goalId, 'rating', rating)} /><label className="review-field review-row-comment"><span>Manager comments</span><input value={existing.comments || ''} onChange={(event) => onChange(ratingKey, item.goalId, 'comments', event.target.value)} disabled={disabled} /></label><small className="review-employee-comment">Employee note: {item.employeeComments || '—'}</small></article>
  })}{!rows.length && <EmptyState title="No goals or KPI assignments" detail="Assignments from Goal Management will appear here." />}</div></Panel>
}

function ReviewEditor({ type, record, onSave, onSubmit, onSendBack, onClarification, onCancel, busy }) {
  const persisted = type === 'manager' ? record.managerReview || {} : record.hrReview || {}
  const [values, setValues] = useState(() => ({ ...(type === 'manager' ? managerDefaults() : hrDefaults()), ...persisted }))
  const isLocked = type === 'manager'
    ? ['SUBMITTED', 'COMPLETED'].includes(record.managerStatus)
    : ['COMPLETED', 'FINALIZED'].includes(record.hrStatus)
  useEffect(() => setValues({ ...(type === 'manager' ? managerDefaults() : hrDefaults()), ...persisted }), [record.appraisalId, type, record.managerReview, record.hrReview])
  const setValue = (key, value) => setValues((current) => ({ ...current, [key]: value }))
  const setRatingRow = (list, goalId, key, value) => setValues((current) => {
    const rows = current[list] || []
    const index = rows.findIndex((row) => String(row.goalId) === String(goalId))
    const next = index < 0 ? [...rows, { goalId, [key]: value }] : rows.map((row, rowIndex) => rowIndex === index ? { ...row, [key]: value } : row)
    return { ...current, [list]: next }
  })
  const self = record.selfAppraisal || {}
  const manager = record.managerReview || {}
  const goals = reviewRowsForGoal(record, 'kra')
  const kpis = reviewRowsForGoal(record, 'kpi')
  const managerRows = (manager.kraRatings || []).map((row) => ({ ...row, title: `KRA ${row.goalId}` }))
  return <div className="review-editor">
    <Panel title="Employee and cycle" className="review-form-panel"><div className="review-employee-grid"><div><small>Employee</small><strong>{record.employeeName}</strong></div><div><small>Employee ID</small><strong>{record.employeeCode || '—'}</strong></div><div><small>Designation</small><strong>{record.designation || '—'}</strong></div><div><small>Department</small><strong>{record.department || '—'}</strong></div><div><small>Manager</small><strong>{record.manager || '—'}</strong></div><div><small>Location</small><strong>{record.location || '—'}</strong></div><div><small>Joining date</small><strong>{showDate(record.joiningDate)}</strong></div><div><small>Appraisal cycle</small><strong>{record.cycleName || '—'}</strong></div></div></Panel>
    <div className="review-trail-steps"><span>Employee</span><i /><span>Self Appraisal</span><i /><span>Manager Review</span><i /><span>HR Review</span></div>
    <ReadonlyTrail title="Self appraisal · employee submitted" data={{ achievements: self.achievements, challenges: self.challenges, keyContributions: self.keyContributions, developmentNeeds: self.developmentNeeds, employeeComments: self.employeeComments, selfRating: score(record.selfRating) }} />
    <SelfGoalTrail record={record} />
    {type === 'manager' ? <>
      <RatingRows title="KRA review" rows={goals} ratingKey="kraRatings" values={values} disabled={isLocked} onChange={setRatingRow} />
      <RatingRows title="KPI review" rows={kpis} ratingKey="kpiRatings" values={values} disabled={isLocked} onChange={setRatingRow} />
      <Panel title="Competency review" className="review-form-panel"><div className="review-competency-list">{(values.competencies || []).map((item, index) => <article key={item.name || index}><strong>{item.name || `Competency ${index + 1}`}</strong><RatingControl label="Rating" value={item.rating} disabled={isLocked} onChange={(rating) => setValue('competencies', values.competencies.map((row, rowIndex) => rowIndex === index ? { ...row, rating } : row))} /><label className="review-field"><span>Comments</span><input value={item.comments || ''} onChange={(event) => setValue('competencies', values.competencies.map((row, rowIndex) => rowIndex === index ? { ...row, comments: event.target.value } : row))} disabled={isLocked} /></label></article>)}</div></Panel>
      <Panel title="Overall manager assessment" className="review-form-panel"><div className="review-form-grid"><RatingControl label="Overall rating" value={values.overallRating} disabled={isLocked} onChange={(value) => setValue('overallRating', value)} /><label className="review-field"><span>Promotion recommendation</span><select value={values.promotionRecommendation || 'NO_CHANGE'} disabled={isLocked} onChange={(event) => setValue('promotionRecommendation', event.target.value)}><option value="NO_CHANGE">No change</option><option value="PROMOTION">Recommend promotion</option></select></label><label className="review-field"><span>PIP recommendation</span><select value={values.pipRecommendation || 'NO'} disabled={isLocked} onChange={(event) => setValue('pipRecommendation', event.target.value)}><option value="NO">No</option><option value="YES">Recommend PIP</option></select></label><ReviewTextField label="Strengths" value={values.strengths} disabled={isLocked} onChange={(value) => setValue('strengths', value)} /><ReviewTextField label="Areas for improvement" value={values.areasForImprovement} disabled={isLocked} onChange={(value) => setValue('areasForImprovement', value)} /><ReviewTextField label="Development recommendations" value={values.developmentRecommendations} disabled={isLocked} onChange={(value) => setValue('developmentRecommendations', value)} /><ReviewTextField label="Training recommendations" value={values.trainingRecommendations} disabled={isLocked} onChange={(value) => setValue('trainingRecommendations', value)} /><ReviewTextField label="Manager comments" rows={4} value={values.managerComments} disabled={isLocked} onChange={(value) => setValue('managerComments', value)} /></div></Panel>
    </> : <>
      <ReadonlyTrail title="Manager review · manager submitted" data={{ managerRating: score(record.managerRating), strengths: manager.strengths, areasForImprovement: manager.areasForImprovement, developmentRecommendations: manager.developmentRecommendations, trainingRecommendations: manager.trainingRecommendations, promotionRecommendation: manager.promotionRecommendation, pipRecommendation: manager.pipRecommendation, managerComments: manager.managerComments }} />
      {(managerRows.length || manager.kpiRatings?.length || manager.competencies?.length) > 0 && <Panel title="Manager KRA, KPI and competency ratings" className="review-form-panel"><div className="review-trail-grid">{[...managerRows, ...(manager.kpiRatings || []).map((row) => ({ ...row, title: `KPI ${row.goalId}` })), ...(manager.competencies || []).map((row) => ({ ...row, title: row.name }))].map((row, index) => <div key={`${row.goalId || row.title}-${index}`}><small>{row.title}</small><strong>{score(row.rating)}{row.comments ? ` · ${row.comments}` : ''}</strong></div>)}</div></Panel>}
      <Panel title="HR assessment and recommendations" className="review-form-panel"><div className="review-form-grid"><RatingControl label="HR rating" value={values.hrRating} disabled={isLocked} onChange={(value) => setValue('hrRating', value)} /><RatingControl label="Final recommended rating" value={values.finalRating} disabled={isLocked} onChange={(value) => setValue('finalRating', value)} /><label className="review-field"><span>Final recommendation</span><select value={values.finalRecommendation || 'NO_CHANGE'} disabled={isLocked} onChange={(event) => setValue('finalRecommendation', event.target.value)}><option value="NO_CHANGE">No change</option><option value="PROMOTION">Promotion</option><option value="INCREMENT">Increment</option><option value="PIP">PIP</option></select></label><label className="review-field"><span>Increment recommendation</span><input value={values.incrementRecommendation || ''} disabled={isLocked} onChange={(event) => setValue('incrementRecommendation', event.target.value)} placeholder="e.g. 8%" /></label><label className="review-field"><span>Succession potential</span><select value={values.successionPotential || ''} disabled={isLocked} onChange={(event) => setValue('successionPotential', event.target.value)}><option value="">Not assessed</option><option>Ready now</option><option>Ready in 1-2 years</option><option>Future potential</option><option>Not identified</option></select></label><ReviewTextField label="Training recommendation" value={values.trainingRecommendation} disabled={isLocked} onChange={(value) => setValue('trainingRecommendation', value)} /><label className="review-field review-calibration-toggle"><input type="checkbox" checked={Boolean(values.underCalibration)} disabled={isLocked} onChange={(event) => setValue('underCalibration', event.target.checked)} /><span>Place under calibration</span></label><ReviewTextField label="Calibration reason" value={values.calibrationReason} disabled={isLocked} onChange={(value) => setValue('calibrationReason', value)} required={Number(values.finalRating) !== Number(record.managerRating)} /><ReviewTextField label="HR comments" rows={4} value={values.hrComments} disabled={isLocked} onChange={(value) => setValue('hrComments', value)} /><ReviewTextField label="Final remarks" rows={4} value={values.finalRemarks} disabled={isLocked} onChange={(value) => setValue('finalRemarks', value)} /></div><div className="review-calibration-compare"><span>Self rating <strong>{score(record.selfRating)}</strong></span><span>Manager rating <strong>{score(record.managerRating)}</strong></span><span>HR rating <strong>{score(values.hrRating)}</strong></span><span>Final rating <strong>{score(values.finalRating)}</strong></span></div></Panel>
    </>}
    <Panel title="Review audit trail" className="review-form-panel"><div className="review-history">{(record.history || []).map((item, index) => <article key={`${item.changedAt}-${index}`}><span><strong>{item.action.replaceAll('_', ' ')}</strong><small>{item.user} · {item.role}</small></span><time>{showDate(item.changedAt)}</time><p>{item.reason || [item.oldValue, item.newValue].filter(Boolean).join(' → ')}</p></article>)}{!record.history?.length && <EmptyState title="No review activity recorded" />}</div></Panel>
    <footer className="review-editor-actions">{isLocked ? <ReviewStatus value={type === 'manager' ? record.managerStatus : record.hrStatus} /> : <><button className="pms-button pms-button-secondary" onClick={() => onCancel()} disabled={busy}>Cancel</button><button className="pms-button pms-button-secondary" onClick={() => onSave(values)} disabled={busy}>Save draft</button>{type === 'manager' && <><button className="pms-button pms-button-secondary" onClick={() => onClarification()} disabled={busy}>Request clarification</button><button className="pms-button pms-button-secondary" onClick={() => onSendBack()} disabled={busy}>Send back</button></>}<button className="pms-button pms-button-primary" onClick={() => onSubmit(values)} disabled={busy}>{busy ? 'Saving…' : type === 'manager' ? 'Submit review' : 'Submit HR review'} <Send size={13} /></button></>}</footer>
  </div>
}

function ReviewOverview({ type, report, onTab, onOpen }) {
  const summary = report?.summary || {}
  const statusRows = [{ name: 'Completed', value: summary.completed || 0 }, { name: 'In Progress', value: summary.inProgress || 0 }, { name: 'Pending', value: summary.pending || 0 }]
  const ratings = report?.ratingDistribution || []
  const departments = report?.departments || []
  const pendingOwners = type === 'manager' ? report?.pendingByManager || [] : report?.pendingByReviewer || []
  const timeline = report?.timeline?.[0]?.stages || []
  const workload = type === 'manager' ? report?.team || {} : report?.workload || {}
  const dueCount = summary.pending || 0
  return <>
    <div className="pms-content-grid pms-grid-three review-chart-grid">
      <Panel title={`${type === 'manager' ? 'Manager' : 'HR'} review status`}><div className="review-donut-layout"><div className="review-donut"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={statusRows} dataKey="value" nameKey="name" innerRadius="65%" outerRadius="88%" paddingAngle={2} stroke="none">{statusRows.map((row, index) => <Cell key={row.name} fill={chartColors[index]} />)}</Pie><Tooltip content={<ChartTooltip />} /></PieChart></ResponsiveContainer><div className="review-donut-center"><strong>{number(summary.totalEmployees)}</strong><span>Employees</span></div></div><div className="review-legend">{statusRows.map((row, index) => <div key={row.name}><span><i style={{ background: chartColors[index] }} />{row.name}</span><strong>{number(row.value)} <small>{summary.totalEmployees ? `(${(row.value * 100 / summary.totalEmployees).toFixed(1)}%)` : '(0.0%)'}</small></strong></div>)}</div></div></Panel>
      <Panel title="Rating distribution"><div className="review-chart-area"><ResponsiveContainer width="100%" height="100%"><BarChart data={ratings} margin={{ top: 12, right: 8, left: -18, bottom: 1 }}><CartesianGrid vertical={false} stroke="#edf0f3" /><XAxis dataKey="rating" tickLine={false} axisLine={false} tick={{ fontSize: 9 }} /><YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fontSize: 8 }} /><Tooltip content={<ChartTooltip />} /><Bar dataKey="count" name="Employees" fill="#3578d4" radius={[3, 3, 0, 0]} barSize={24} /></BarChart></ResponsiveContainer></div><div className="review-rating-captions">{ratingLabels.map((label, index) => <small key={label}>{index + 1} · {label}</small>)}</div></Panel>
      <Panel title="Average rating by department"><div className="review-chart-area review-department-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={departments} layout="vertical" margin={{ top: 5, right: 12, left: 0, bottom: 0 }}><CartesianGrid horizontal={false} stroke="#edf0f3" /><XAxis type="number" domain={[0, 5]} tickLine={false} axisLine={false} tick={{ fontSize: 8 }} /><YAxis type="category" dataKey="department" width={95} tickLine={false} axisLine={false} tick={{ fontSize: 8 }} /><Tooltip content={<ChartTooltip />} /><Bar dataKey="averageRating" name="Average rating" fill="#16846b" radius={[0, 3, 3, 0]} barSize={12} /></BarChart></ResponsiveContainer></div></Panel>
    </div>
    <div className="pms-content-grid pms-grid-three review-overview-grid">
      <Panel title={`${type === 'manager' ? 'Team' : 'Department'} review summary`} className="review-table-panel"><DataTable minWidth="610px" className="review-table" columns={[{ key: 'department', label: 'Department' }, { key: 'total', label: 'Total' }, { key: 'completed', label: 'Completed' }, { key: 'progress', label: 'In progress' }, { key: 'pending', label: 'Pending' }, { key: 'rating', label: 'Avg. rating' }]}>{departments.map((row) => <tr key={row.department}><td>{row.department}</td><td>{number(row.totalEmployees)}</td><td>{number(row.completed)}</td><td>{number(row.inProgress)}</td><td>{number(row.pending)}</td><td>{Number(row.averageRating || 0).toFixed(2)}</td></tr>)}{!departments.length && <tr><td colSpan="6">No department review data.</td></tr>}</DataTable></Panel>
      <Panel title={type === 'manager' ? 'Pending reviews by manager' : 'Pending reviews by HR reviewer'} className="review-table-panel" action={<button className="pms-text-button" onClick={() => onTab('pending-reviews')}>View all</button>}><DataTable minWidth="420px" className="review-table" columns={[{ key: 'owner', label: type === 'manager' ? 'Manager name' : 'HR reviewer' }, { key: 'department', label: 'Department' }, { key: 'pending', label: 'Pending reviews' }]}>{pendingOwners.map((row, index) => <tr key={`${row.managerId || row.reviewerId || index}`}><td>{row.managerName || row.reviewerName}</td><td>{row.department}</td><td>{number(row.pending)}</td></tr>)}{!pendingOwners.length && <tr><td colSpan="3">No pending reviews.</td></tr>}</DataTable></Panel>
      <Panel title="Review timeline"><div className="appraisal-timeline-list">{timeline.map((stage, index) => <div key={stage.stageCode} className={index === 0 ? 'is-current' : ''}><i /><span><strong>{stage.stageCode.replaceAll('_', ' ')}</strong><small>{showDate(stage.startDate)} – {showDate(stage.endDate)}</small></span></div>)}{!timeline.length && <EmptyState title="No cycle dates configured" detail="Set the appraisal dates in Self Appraisal." />}</div></Panel>
    </div>
    <div className="pms-content-grid pms-grid-three review-bottom-grid">
      <Panel title={type === 'manager' ? 'My team overview' : 'My HR review overview'} action={<button className="pms-button pms-button-primary" onClick={() => onTab('pending-reviews')}>{type === 'manager' ? 'View my team' : 'View my reviews'}</button>}><div className="review-workload"><div className="review-progress-ring" style={{ '--progress': `${Number(workload.completionRate || 0)}%` }}><strong>{Number(workload.completionRate || 0).toFixed(0)}%</strong></div><div className="review-workload-stats"><span>Total assigned<strong>{number(workload.total)}</strong></span><span>Completed<strong>{number(workload.completed)}</strong></span><span>In progress<strong>{number(workload.inProgress)}</strong></span><span>Pending<strong>{number(workload.pending)}</strong></span><span>Average rating<strong>{Number(workload.averageRating || 0).toFixed(2)} / 5</strong></span></div></div></Panel>
      <Panel title="Key insights" className="review-insights"><div>{(report?.insights || []).map((insight, index) => <p key={`${insight.label}-${index}`}><Lightbulb size={13} /><span>{insight.label}</span><strong>{insight.value}</strong></p>)}{dueCount > 0 && <p><AlertTriangle size={13} /><span>Pending reviews remain before cycle close.</span><strong>{number(dueCount)}</strong></p>}</div></Panel>
      <Panel title="Quick actions"><div className="pms-workspace-actions">{type === 'manager' ? <><button onClick={() => onOpen('team')}><ClipboardCheck size={14} />Review team members</button><button onClick={() => onOpen('feedback')}><MessageSquareText size={14} />Provide feedback</button><button onClick={() => onOpen('guidelines')}><FileText size={14} />Review guidelines</button><button onClick={() => onOpen('download')}><Download size={14} />Download review report</button><button onClick={() => onOpen('export')}><Download size={14} />Export review data</button></> : <><button onClick={() => onOpen('reminder')}><Send size={14} />Send review reminder</button><button onClick={() => onOpen('reassign')}><UsersRound size={14} />Reassign pending reviews</button><button onClick={() => onOpen('download')}><Download size={14} />Download review report</button><button onClick={() => onOpen('analytics')}><Activity size={14} />View review analytics</button><button onClick={() => onOpen('export')}><Download size={14} />Export review data</button></>}</div></Panel>
    </div>
  </>
}

function summarizeQueue(rows, type, baseReport) {
  const hr = type === 'hr'
  const statusKey = hr ? 'hrStatus' : 'managerStatus'
  const ratingKey = hr ? 'hrRating' : 'managerRating'
  const completedStatuses = hr ? ['COMPLETED', 'FINALIZED'] : ['SUBMITTED', 'COMPLETED']
  const inProgressStatuses = hr ? ['IN_PROGRESS', 'UNDER_CALIBRATION'] : ['IN_PROGRESS']
  const completed = rows.filter((row) => completedStatuses.includes(row[statusKey])).length
  const inProgress = rows.filter((row) => inProgressStatuses.includes(row[statusKey])).length
  const departments = [...new Set(rows.map((row) => row.department || 'Others'))].map((department) => {
    const members = rows.filter((row) => (row.department || 'Others') === department)
    const finished = members.filter((row) => completedStatuses.includes(row[statusKey])).length
    const active = members.filter((row) => inProgressStatuses.includes(row[statusKey])).length
    const rated = members.filter((row) => row[ratingKey] != null)
    return { department, totalEmployees: members.length, completed: finished, inProgress: active, pending: members.length - finished - active, completionRate: members.length ? finished * 100 / members.length : 0, averageRating: rated.length ? rated.reduce((sum, row) => sum + Number(row[ratingKey]), 0) / rated.length : 0 }
  })
  const ratedRows = rows.filter((row) => row[ratingKey] != null)
  const ratingDistribution = [1, 2, 3, 4, 5].map((rating) => ({ rating, count: ratedRows.filter((row) => Math.round(Number(row[ratingKey])) === rating).length }))
  const averageRating = ratedRows.length ? ratedRows.reduce((sum, row) => sum + Number(row[ratingKey]), 0) / ratedRows.length : 0
  const summary = { totalEmployees: rows.length, completed, inProgress, pending: Math.max(0, rows.length - completed - inProgress), completionRate: rows.length ? completed * 100 / rows.length : 0, averageRating }
  const ownerKey = hr ? 'reviewer' : 'manager'
  const pendingByOwner = [...new Set(rows.map((row) => row[ownerKey] || 'Unassigned'))].map((name) => ({ [hr ? 'reviewerName' : 'managerName']: name, department: 'Multiple', pending: rows.filter((row) => (row[ownerKey] || 'Unassigned') === name && !completedStatuses.includes(row[statusKey])).length })).filter((row) => row.pending)
  return { ...baseReport, summary, departments, ratingDistribution, insights: [{ label: 'Completion rate in filtered reviews', value: `${summary.completionRate.toFixed(1)}%` }, { label: 'Pending reviews', value: summary.pending }], ...(hr ? { pendingByReviewer: pendingByOwner, workload: { total: rows.length, completed, inProgress, pending: summary.pending, completionRate: summary.completionRate, averageRating } } : { pendingByManager: pendingByOwner, team: { total: rows.length, completed, inProgress, pending: summary.pending, completionRate: summary.completionRate, averageRating } }) }
}

function ReviewTable({ type, rows, reviewers, onOpen, onRemind, onReassign }) {
  const hr = type === 'hr'
  const columns = hr
    ? [{ key: 'employee', label: 'Employee' }, { key: 'department', label: 'Department' }, { key: 'manager', label: 'Manager' }, { key: 'managerRating', label: 'Manager rating' }, { key: 'status', label: 'HR review status' }, { key: 'due', label: 'Due date' }, { key: 'reviewer', label: 'Assigned reviewer' }, { key: 'actions', label: 'Action' }]
    : [{ key: 'employee', label: 'Employee' }, { key: 'id', label: 'Employee ID' }, { key: 'designation', label: 'Designation' }, { key: 'department', label: 'Department' }, { key: 'selfStatus', label: 'Self appraisal' }, { key: 'selfRating', label: 'Self rating' }, { key: 'status', label: 'Review status' }, { key: 'due', label: 'Due date' }, { key: 'days', label: 'Days remaining' }, { key: 'actions', label: 'Action' }]
  return <Panel title={hr ? 'HR review queue' : 'Manager review queue'} className="review-queue-panel"><DataTable columns={columns} minWidth={hr ? '1060px' : '1190px'} className="review-table review-queue-table"><>{rows.map((row) => <tr key={row.appraisalId}><td><button className="review-employee-link" onClick={() => onOpen(row.appraisalId)}><span className="review-avatar">{row.employeeName?.split(' ').map((part) => part[0]).slice(0, 2).join('')}</span><span><strong>{row.employeeName}</strong><small>{row.employeeCode}</small></span></button></td>{hr ? <><td>{row.department || '—'}</td><td>{row.manager || '—'}</td><td>{score(row.managerRating)}</td><td><ReviewStatus value={row.hrStatus} /></td><td>{showDate(row.dueDate)}</td><td>{row.reviewer || 'Unassigned'}</td><td><div className="review-row-actions"><button className="pms-button pms-button-primary" onClick={() => onOpen(row.appraisalId)}>{row.hrStatus === 'PENDING' ? 'Start HR review' : 'Continue review'}</button><button className="pms-button pms-button-secondary" onClick={() => onRemind(row.appraisalId)}>Remind</button><button className="pms-button pms-button-secondary" onClick={() => onReassign(row)}>Reassign</button><button className="pms-button pms-button-secondary" onClick={() => onOpen(row.appraisalId)}>View trail</button></div></td></> : <><td>{row.employeeCode || '—'}</td><td>{row.designation || '—'}</td><td>{row.department || '—'}</td><td><ReviewStatus value={row.selfStatus} /></td><td>{score(row.selfRating)}</td><td><ReviewStatus value={row.managerStatus} /></td><td>{showDate(row.dueDate)}</td><td>{row.daysRemaining}</td><td><div className="review-row-actions"><button className="pms-button pms-button-primary" onClick={() => onOpen(row.appraisalId)}>{row.managerStatus === 'IN_PROGRESS' ? 'Continue review' : 'Start review'}</button><button className="pms-button pms-button-secondary" onClick={() => onRemind(row.appraisalId)}>Remind</button><button className="pms-button pms-button-secondary" onClick={() => onOpen(row.appraisalId)}>View self appraisal</button></div></td></>}</tr>)}{!rows.length && <tr><td colSpan={columns.length}><EmptyState title="No reviews match these filters" detail="Try another cycle, status, or search term." /></td></tr>}</></DataTable></Panel>
}

export default function ReviewPage({ type, notify }) {
  const isHr = type === 'hr'
  const title = isHr ? 'HR Review' : 'Manager Review'
  const tabs = isHr ? hrTabs : managerTabs
  const { tab: routeTab } = useParams()
  const navigate = useNavigate()
  const activeTab = tabs.some(([id]) => id === routeTab) ? routeTab : 'overview'
  const [report, setReport] = useState(null)
  const [rows, setRows] = useState([])
  const [departments, setDepartments] = useState([])
  const [reviewers, setReviewers] = useState([])
  const [cycleId, setCycleId] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [reviewerId, setReviewerId] = useState('')
  const [managerId, setManagerId] = useState('')
  const [designationFilter, setDesignationFilter] = useState('')
  const [dueBefore, setDueBefore] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [rating, setRating] = useState('')
  const [selected, setSelected] = useState(null)
  const [actionModal, setActionModal] = useState(null)
  const [reason, setReason] = useState('')
  const [selectedReviewer, setSelectedReviewer] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    const params = { ...(cycleId ? { cycleId: Number(cycleId) } : {}), ...(departmentId ? { departmentId: Number(departmentId) } : {}) }
    const queueParams = { ...params, ...(search ? { search } : {}), ...(status ? { status } : {}), ...(rating ? { rating: Number(rating) } : {}), ...(isHr && reviewerId ? { reviewerId: Number(reviewerId) } : {}), ...(isHr && managerId ? { managerId: Number(managerId) } : {}) }
    Promise.all([
      (isHr ? pmsService.hrReviewOverview(params, controller.signal) : pmsService.managerReviewOverview(params, controller.signal)),
      (isHr ? pmsService.hrReviewQueue(queueParams, controller.signal) : pmsService.managerReviewQueue(queueParams, controller.signal)),
      pmsService.departments(controller.signal),
      isHr ? pmsService.hrReviewers(controller.signal) : Promise.resolve([]),
    ]).then(([overview, queue, departmentRows, reviewerRows]) => {
      if (controller.signal.aborted) return
      setReport(overview)
      setRows(queue || [])
      setDepartments(departmentRows || [])
      setReviewers(reviewerRows || [])
      if (!cycleId && overview.cycles?.length) setCycleId(String(overview.cycles[0].cycleId))
      setLoading(false)
    }).catch((requestError) => {
      if (!controller.signal.aborted) { setError(getApiErrorMessage(requestError, `${title} data could not be loaded.`)); setLoading(false) }
    })
    return () => controller.abort()
  }, [type, title, cycleId, departmentId, reviewerId, managerId, search, status, rating, revision, isHr])

  const filteredRows = useMemo(() => rows.filter((row) => (!designationFilter || row.designation === designationFilter)
    && (!dueBefore || (row.dueDate && row.dueDate <= dueBefore))), [rows, designationFilter, dueBefore])
  const hasQueueFilters = Boolean(search || status || rating || designationFilter || dueBefore || (isHr && (reviewerId || managerId)))
  const displayReport = useMemo(() => hasQueueFilters ? summarizeQueue(filteredRows, type, report || {}) : report, [hasQueueFilters, filteredRows, type, report])
  const summary = displayReport?.summary || {}
  const summaryCards = useMemo(() => [
    { label: 'Total employees', value: number(summary.totalEmployees), note: 'Eligible in selected cycle', icon: UsersRound, accent: 'blue' },
    { label: isHr ? 'Completed HR reviews' : 'Completed reviews', value: number(summary.completed), note: 'Submitted for this cycle', icon: Check, accent: 'green' },
    { label: isHr ? 'Pending HR reviews' : 'Pending reviews', value: number(summary.pending), note: `${number(summary.inProgress)} in progress`, icon: CalendarDays, accent: 'amber' },
    { label: 'Completion rate', value: `${Number(summary.completionRate || 0).toFixed(1)}%`, note: 'Completed / total assigned', icon: Activity, accent: 'blue' },
    { label: 'Average rating', value: `${Number(summary.averageRating || 0).toFixed(2)} / 5`, note: isHr ? 'HR review rating' : 'Manager review rating', icon: Star, accent: 'rose' },
  ], [summary, isHr])
  const visibleRows = useMemo(() => filteredRows.filter((row) => activeTab === 'completed-reviews'
    ? (isHr ? ['COMPLETED', 'FINALIZED'].includes(row.hrStatus) : ['SUBMITTED', 'COMPLETED'].includes(row.managerStatus))
    : activeTab === 'pending-reviews'
      ? (isHr ? !['COMPLETED', 'FINALIZED'].includes(row.hrStatus) : !['SUBMITTED', 'COMPLETED'].includes(row.managerStatus))
      : true), [rows, activeTab, isHr])
  const cycleOptions = report?.cycles || []
  const goTab = (next) => navigate(`/performance/${isHr ? 'hr-review' : 'manager-review'}/${next}`)
  const refresh = () => setRevision((value) => value + 1)

  async function openReview(appraisalId) {
    setBusy(true)
    try {
      const detail = isHr ? await pmsService.getHrReview(appraisalId) : await pmsService.getManagerReview(appraisalId)
      setSelected(detail)
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Review details could not be opened.') }) }
    finally { setBusy(false) }
  }

  async function saveDraft(values) {
    setBusy(true)
    try {
      const detail = isHr ? await pmsService.saveHrReview(selected.appraisalId, values) : await pmsService.saveManagerReview(selected.appraisalId, values)
      setSelected(detail); refresh(); notify({ type: 'success', message: 'Review draft saved.' })
      return true
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Review draft could not be saved.') }); return false }
    finally { setBusy(false) }
  }

  async function prepareSubmit(values) {
    if (await saveDraft(values)) setActionModal({ kind: 'submit' })
  }

  async function performAction() {
    if (!actionModal) return
    setBusy(true)
    try {
      if (actionModal.kind === 'submit') {
        if (isHr) await pmsService.submitHrReview(selected.appraisalId)
        else await pmsService.submitManagerReview(selected.appraisalId)
        notify({ type: 'success', message: isHr ? 'HR review finalized. Appraisal summary is ready.' : 'Manager review submitted. HR review is now pending.' })
        setSelected(null)
      } else if (actionModal.kind === 'send-back' || actionModal.kind === 'clarification') {
        if (actionModal.kind === 'send-back') await pmsService.sendBackManagerReview(selected.appraisalId, reason)
        else await pmsService.requestManagerClarification(selected.appraisalId, reason)
        notify({ type: 'success', message: actionModal.kind === 'send-back' ? 'Review sent back to the employee.' : 'Clarification requested from the employee.' })
        setSelected(null)
      } else if (actionModal.kind === 'reassign') {
        await pmsService.reassignHrReview(actionModal.row.appraisalId, Number(selectedReviewer))
        notify({ type: 'success', message: 'HR review reassigned.' })
      }
      setActionModal(null); setReason(''); refresh()
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'The review action could not be completed.') }) }
    finally { setBusy(false) }
  }

  async function remind(appraisalId) {
    try {
      if (isHr) await pmsService.remindHrReview(appraisalId)
      else await pmsService.remindManagerReview(appraisalId)
      notify({ type: 'success', message: 'Reminder recorded in the review audit trail.' }); refresh()
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Reminder could not be recorded.') }) }
  }

  async function exportReport() {
    try { await pmsService.exportReviews(isHr ? 'HR' : 'MANAGER', { ...(cycleId ? { cycleId } : {}), ...(departmentId ? { departmentId } : {}) }) }
    catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Review report could not be exported.') }) }
  }

  async function quickAction(action) {
    if (action === 'export' || action === 'download') { await exportReport(); return }
    if (action === 'team') { goTab('pending-reviews'); return }
    if (action === 'analytics') { goTab('rating-analysis'); return }
    if (action === 'guidelines') { setActionModal({ kind: 'guidelines' }); return }
    const candidate = filteredRows.find((row) => isHr
      ? !['COMPLETED', 'FINALIZED'].includes(row.hrStatus)
      : !['SUBMITTED', 'COMPLETED'].includes(row.managerStatus))
    if (!candidate) { notify({ type: 'info', message: 'There are no pending reviews for the current filters.' }); return }
    if (action === 'feedback') { await openReview(candidate.appraisalId); return }
    if (action === 'reminder') { await remind(candidate.appraisalId); return }
    if (action === 'reassign') {
      setSelectedReviewer(String(candidate.reviewerId || ''))
      setActionModal({ kind: 'reassign', row: candidate })
    }
  }

  return <PageLayout eyebrow="PERFORMANCE MANAGEMENT" title={title} description={isHr ? 'Review submitted manager assessments, calibrate outcomes, and finalize appraisals.' : 'Assess submitted self appraisals and provide structured team feedback.'} className="pms-page pms-workspace-page review-workspace-page">
    <div className="pms-workspace-toolbar review-toolbar"><label className="pms-search"><Search size={14} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, ID, department, role, manager, location" /></label><label className="pms-workspace-dept-filter"><CalendarDays size={14} /><select aria-label="Appraisal cycle" value={cycleId} onChange={(event) => setCycleId(event.target.value)}><option value="">All cycles</option>{cycleOptions.map((cycle) => <option key={cycle.cycleId} value={cycle.cycleId}>{cycle.cycleName}</option>)}</select></label><label className="pms-workspace-dept-filter"><UsersRound size={14} /><select aria-label="Department" value={departmentId} onChange={(event) => setDepartmentId(event.target.value)}><option value="">All departments</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.departmentName}</option>)}</select></label>{isHr && <label className="pms-workspace-dept-filter"><ShieldCheck size={14} /><select aria-label="HR reviewer" value={reviewerId} onChange={(event) => setReviewerId(event.target.value)}><option value="">All reviewers</option>{reviewers.map((reviewer) => <option key={reviewer.employeeId} value={reviewer.employeeId}>{reviewer.name}</option>)}</select></label>}<label className="pms-workspace-dept-filter"><span>Status</span><select aria-label="Review status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All statuses</option>{(isHr ? ['PENDING', 'IN_PROGRESS', 'UNDER_CALIBRATION', 'FINALIZED'] : ['PENDING', 'IN_PROGRESS', 'SUBMITTED', 'SENT_BACK']).map((value) => <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>)}</select></label><label className="pms-workspace-dept-filter"><Star size={14} /><select aria-label="Rating" value={rating} onChange={(event) => setRating(event.target.value)}><option value="">All ratings</option>{[1, 2, 3, 4, 5].map((value) => <option key={value} value={value}>{value} · {ratingLabels[value - 1]}</option>)}</select></label><button className="pms-button pms-button-secondary" onClick={exportReport}><Download size={14} />Export report</button></div>
    <div className="review-extra-filters">{isHr && <label className="pms-workspace-dept-filter"><UsersRound size={14} /><select aria-label="Manager" value={managerId} onChange={(event) => setManagerId(event.target.value)}><option value="">All managers</option>{[...new Map(rows.filter((row) => row.managerId).map((row) => [row.managerId, row.manager])).entries()].map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></label>}<label className="pms-workspace-dept-filter"><span>Designation</span><select aria-label="Designation" value={designationFilter} onChange={(event) => setDesignationFilter(event.target.value)}><option value="">All designations</option>{[...new Set(rows.map((row) => row.designation).filter(Boolean))].sort().map((name) => <option key={name} value={name}>{name}</option>)}</select></label><label className="pms-workspace-dept-filter"><CalendarDays size={14} /><span>Due by</span><input aria-label="Due by" type="date" value={dueBefore} onChange={(event) => setDueBefore(event.target.value)} /></label></div>
    <nav className="pms-tabs pms-workspace-tabs" aria-label={`${title} views`}>{tabs.map(([id, label]) => <button key={id} className={activeTab === id ? 'is-active' : ''} onClick={() => goTab(id)}>{label}</button>)}</nav>
    {loading ? <LoadingState label={`Loading ${title.toLowerCase()} data`} /> : error ? <ErrorState message={error} onRetry={refresh} /> : <>
      <section className="pms-metric-grid pms-workspace-metrics review-metrics">{summaryCards.map((card) => <StatCard key={card.label} {...card} />)}</section>
      {selected ? <section className="review-detail-shell"><header><div><span>{selected.cycleName}</span><h2>{selected.employeeName}</h2></div><button className="pms-button pms-button-secondary" onClick={() => setSelected(null)}>Close review</button></header><ReviewEditor key={`${type}-${selected.appraisalId}`} type={type} record={selected} busy={busy} onSave={saveDraft} onSubmit={prepareSubmit} onSendBack={() => setActionModal({ kind: 'send-back' })} onClarification={() => setActionModal({ kind: 'clarification' })} onCancel={() => setSelected(null)} /></section> : <>
        {activeTab === 'overview' && <ReviewOverview type={type} report={displayReport} onTab={goTab} onOpen={quickAction} />}
        {['pending-reviews', 'completed-reviews', 'review-status'].includes(activeTab) && <ReviewTable type={type} rows={visibleRows} reviewers={reviewers} onOpen={openReview} onRemind={remind} onReassign={(row) => { setSelectedReviewer(String(row.reviewerId || '')); setActionModal({ kind: 'reassign', row }) }} />}
        {activeTab === 'team-performance' && <Panel title={isHr ? 'Department performance' : 'Team performance'}><DataTable className="review-table" minWidth="750px" columns={[{ key: 'department', label: 'Department' }, { key: 'total', label: 'Employees' }, { key: 'completed', label: 'Completed' }, { key: 'progress', label: 'In progress' }, { key: 'pending', label: 'Pending' }, { key: 'rate', label: 'Completion rate' }, { key: 'average', label: 'Average rating' }]}>{(displayReport?.departments || []).map((row) => <tr key={row.department}><td>{row.department}</td><td>{number(row.totalEmployees)}</td><td>{number(row.completed)}</td><td>{number(row.inProgress)}</td><td>{number(row.pending)}</td><td>{Number(row.completionRate || 0).toFixed(1)}%</td><td>{Number(row.averageRating || 0).toFixed(2)} / 5</td></tr>)}</DataTable></Panel>}
        {activeTab === 'rating-analysis' && <div className="pms-content-grid pms-grid-two"><Panel title="Rating distribution"><div className="review-chart-area review-large-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={displayReport?.ratingDistribution || []}><CartesianGrid vertical={false} stroke="#edf0f3" /><XAxis dataKey="rating" tickLine={false} axisLine={false} /><YAxis allowDecimals={false} tickLine={false} axisLine={false} /><Tooltip content={<ChartTooltip />} /><Bar dataKey="count" name="Employees" radius={[3, 3, 0, 0]}>{(displayReport?.ratingDistribution || []).map((row, index) => <Cell key={row.rating} fill={['#c75b52', '#dc8c37', '#71869b', '#3578d4', '#16846b'][index]} />)}</Bar></BarChart></ResponsiveContainer></div></Panel><Panel title="Average rating by department"><div className="review-chart-area review-large-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={displayReport?.departments || []} layout="vertical" margin={{ left: 2, right: 18 }}><CartesianGrid horizontal={false} stroke="#edf0f3" /><XAxis type="number" domain={[0, 5]} /><YAxis type="category" dataKey="department" width={110} tick={{ fontSize: 9 }} /><Tooltip content={<ChartTooltip />} /><Bar dataKey="averageRating" name="Average rating" fill="#16846b" radius={[0, 3, 3, 0]} /></BarChart></ResponsiveContainer></div></Panel></div>}
        {activeTab === 'review-timeline' && <Panel title={`${cycleOptions.find((cycle) => String(cycle.cycleId) === cycleId)?.cycleName || 'Appraisal cycle'} · Review timeline`}><div className="appraisal-timeline-list review-full-timeline">{(displayReport?.timeline?.[0]?.stages || []).map((stage, index) => <div key={stage.stageCode} className={index === 0 ? 'is-current' : ''}><i /><span><strong>{stage.stageCode.replaceAll('_', ' ')}</strong><small>{showDate(stage.startDate)} – {showDate(stage.endDate)}</small></span></div>)}{!displayReport?.timeline?.[0]?.stages?.length && <EmptyState title="No timeline configured" detail="Configure appraisal stage dates in Self Appraisal." />}</div></Panel>}
      </>}
      <footer className="pms-footer">Performance Management <span>·</span> {title}</footer>
    </>}
    {actionModal?.kind === 'submit' && <Modal title={isHr ? 'Finalize HR review?' : 'Submit manager review?'} description="This action advances the appraisal to the next workflow stage." onClose={() => setActionModal(null)}><div className="confirm-content"><span className="confirm-symbol"><BadgeCheck size={19} /></span><p>{isHr ? 'The final calibrated rating and recommendation will be saved to the appraisal summary.' : 'The manager assessment will be locked and HR review will become available.'}</p></div><footer className="modal-actions"><button className="button button-secondary" disabled={busy} onClick={() => setActionModal(null)}>Cancel</button><button className="button button-primary" disabled={busy} onClick={performAction}>{busy ? 'Submitting…' : 'Confirm submission'}</button></footer></Modal>}
    {actionModal?.kind === 'guidelines' && <Modal title="Review guidelines" description={`${title} · ${cycleOptions.find((cycle) => String(cycle.cycleId) === cycleId)?.cycleName || 'Current cycle'}`} onClose={() => setActionModal(null)}><div className="review-modal-body review-guidelines"><p>Use evidence from the submitted goals and appraisal trail when assigning ratings.</p><p>Write specific, constructive feedback and explain material rating differences.</p><p>Save a draft before submission. Submitted manager reviews and finalized HR reviews are locked.</p><p>HR must provide a calibration reason when the final rating differs from the manager rating.</p></div></Modal>}
    {(actionModal?.kind === 'send-back' || actionModal?.kind === 'clarification') && <Modal title={actionModal.kind === 'send-back' ? 'Send review back' : 'Request employee clarification'} description="A reason is required and will be recorded in the review history." onClose={() => setActionModal(null)}><div className="review-modal-body"><label className="review-field"><span>Reason</span><textarea rows="4" value={reason} onChange={(event) => setReason(event.target.value)} /></label></div><footer className="modal-actions"><button className="button button-secondary" disabled={busy} onClick={() => setActionModal(null)}>Cancel</button><button className="button button-primary" disabled={busy || !reason.trim()} onClick={performAction}>{busy ? 'Sending…' : 'Confirm'}</button></footer></Modal>}
    {actionModal?.kind === 'reassign' && <Modal title="Reassign HR review" description={actionModal.row.employeeName} onClose={() => setActionModal(null)}><div className="review-modal-body"><label className="review-field"><span>HR reviewer</span><select value={selectedReviewer} onChange={(event) => setSelectedReviewer(event.target.value)}><option value="">Select reviewer</option>{reviewers.map((reviewer) => <option key={reviewer.employeeId} value={reviewer.employeeId}>{reviewer.name} · {reviewer.department}</option>)}</select></label></div><footer className="modal-actions"><button className="button button-secondary" disabled={busy} onClick={() => setActionModal(null)}>Cancel</button><button className="button button-primary" disabled={busy || !selectedReviewer} onClick={performAction}>Reassign review</button></footer></Modal>}
  </PageLayout>
}