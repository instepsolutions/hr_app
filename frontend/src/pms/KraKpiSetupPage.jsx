import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Activity, ArrowDownUp, Building2, Check, CirclePlus, ClipboardList, Copy, Download,
  Eye, FileSpreadsheet, Filter, Link2, Pencil, Search, Target, Trash2, UsersRound, Workflow,
} from 'lucide-react'
import {
  Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from 'recharts'
import { DataTable, EmptyState, ErrorState, LoadingState, Modal, PageLayout, StatCard } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { defaultRange } from './PmsUI'
import { pmsService } from '../services/pmsService'
import './pms-workspace.css'

const tabs = [
  { id: 'overview', label: 'Overview' }, { id: 'kra-library', label: 'KRA Library' },
  { id: 'kpi-library', label: 'KPI Library' }, { id: 'department-mapping', label: 'Department Mapping' },
  { id: 'employee-mapping', label: 'Employee Mapping' }, { id: 'alignment', label: 'KRA/KPI Alignment' },
  { id: 'history', label: 'Setup History' },
]
const chartColors = ['#256bd1', '#1d9a74', '#ec9a35', '#8b64bd', '#d25e5c', '#6e8fa8', '#8792a0']
const kraStatuses = ['ACTIVE', 'UNDER_REVIEW', 'DRAFT', 'INACTIVE']
const ownerOptions = [['MANAGER', 'Manager'], ['EMPLOYEE', 'Employee'], ['SHARED', 'Shared'], ['HR', 'HR Owner']]
const measurementOptions = [['NUMBER', 'Number'], ['PERCENTAGE', 'Percentage'], ['CURRENCY', 'Currency'], ['RATING', 'Rating'], ['BOOLEAN', 'Boolean'], ['RATIO', 'Ratio']]
const frequencyOptions = [['DAILY', 'Daily'], ['WEEKLY', 'Weekly'], ['MONTHLY', 'Monthly'], ['QUARTERLY', 'Quarterly'], ['HALF_YEARLY', 'Half-Yearly'], ['YEARLY', 'Yearly']]
const today = new Date().toISOString().slice(0, 10)
const blankKra = { kraName: '', categoryId: '', description: '', departmentId: '', ownerType: 'SHARED', ownerEmployeeId: '', weightage: 0, status: 'DRAFT', effectiveDate: today }
const blankKpi = { kraId: '', kpiName: '', measurementType: 'NUMBER', targetValue: '', unit: '', weightage: 0, departmentId: '', ownerType: 'SHARED', ownerEmployeeId: '', frequency: 'QUARTERLY', status: 'DRAFT' }
const number = (value) => Number(value || 0).toLocaleString()
const displayDate = (value) => value ? new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(`${String(value).slice(0, 10)}T00:00:00`)) : '—'

function Panel({ title, children, className = '', action }) {
  return <section className={`pms-panel ${className}`}><header className="pms-panel-heading"><h2>{title}</h2>{action}</header>{children}</section>
}

function Status({ value }) {
  const status = String(value || 'DRAFT').toLowerCase().replaceAll('_', '-')
  return <span className={`pms-status status-${status}`}>{String(value || 'DRAFT').replaceAll('_', ' ')}</span>
}

function KpiCard({ label, value, icon: Icon, accent }) {
  return <StatCard label={label} value={number(value)} note="Current setup" icon={Icon} accent={accent} />
}

function KraForm({ item, lookups, departments, onClose, onSave }) {
  const [values, setValues] = useState(() => ({ ...blankKra, ...(item || {}), categoryId: item?.categoryId ?? '', departmentId: item?.departmentId ?? '', ownerEmployeeId: item?.ownerEmployeeId ?? '' }))
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  function set(field, value) { setValues((current) => ({ ...current, [field]: value })) }
  async function submit(event) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await onSave({ ...values, categoryId: values.categoryId ? Number(values.categoryId) : null,
        departmentId: values.departmentId ? Number(values.departmentId) : null,
        ownerEmployeeId: values.ownerEmployeeId ? Number(values.ownerEmployeeId) : null,
        weightage: Number(values.weightage), effectiveDate: values.effectiveDate || today })
      onClose()
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'KRA could not be saved.')) }
    finally { setSaving(false) }
  }
  return <Modal title={item ? 'Edit KRA' : 'Create KRA'} description="Maintain the shared performance framework." onClose={onClose} size="large">
    <form className="pms-workspace-form" onSubmit={submit}>
      {error && <p className="pms-form-error">{error}</p>}
      <label>KRA title<input required maxLength="160" value={values.kraName} onChange={(event) => set('kraName', event.target.value)} /></label>
      <label>Category<select value={values.categoryId} onChange={(event) => set('categoryId', event.target.value)}><option value="">Uncategorized</option>{lookups.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
      <label>Department<select value={values.departmentId} onChange={(event) => set('departmentId', event.target.value)}><option value="">All departments</option>{departments.map((item) => <option key={item.departmentId} value={item.departmentId}>{item.departmentName}</option>)}</select></label>
      <label>Owner<select value={values.ownerType} onChange={(event) => set('ownerType', event.target.value)}>{ownerOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label>Owner employee ID<input type="number" min="1" value={values.ownerEmployeeId} onChange={(event) => set('ownerEmployeeId', event.target.value)} disabled={values.ownerType !== 'EMPLOYEE'} required={values.ownerType === 'EMPLOYEE'} /></label>
      <label>Weightage (%)<input type="number" min="0" max="100" step="0.1" required value={values.weightage} onChange={(event) => set('weightage', event.target.value)} /></label>
      <label>Status<select value={values.status} onChange={(event) => set('status', event.target.value)}>{kraStatuses.map((status) => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select></label>
      <label>Effective date<input type="date" required value={values.effectiveDate || today} onChange={(event) => set('effectiveDate', event.target.value)} /></label>
      <label className="field-span-two">Description<textarea rows="3" maxLength="500" value={values.description || ''} onChange={(event) => set('description', event.target.value)} /></label>
      <footer className="pms-form-actions"><button type="button" className="pms-button pms-button-secondary" onClick={onClose}>Cancel</button><button className="pms-button pms-button-primary" disabled={saving}>{saving ? 'Saving…' : 'Save KRA'}</button></footer>
    </form>
  </Modal>
}

function KpiForm({ item, kras, departments, onClose, onSave }) {
  const [values, setValues] = useState(() => ({ ...blankKpi, ...(item || {}), kraId: item?.kraId ?? '', departmentId: item?.departmentId ?? '', ownerEmployeeId: item?.ownerEmployeeId ?? '' }))
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  function set(field, value) { setValues((current) => ({ ...current, [field]: value })) }
  async function submit(event) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await onSave({ ...values, kraId: Number(values.kraId), targetValue: Number(values.targetValue),
        weightage: Number(values.weightage), departmentId: values.departmentId ? Number(values.departmentId) : null,
        ownerEmployeeId: values.ownerEmployeeId ? Number(values.ownerEmployeeId) : null })
      onClose()
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'KPI could not be saved.')) }
    finally { setSaving(false) }
  }
  return <Modal title={item ? 'Edit KPI' : 'Create KPI'} description="Link a measurable outcome to a KRA." onClose={onClose} size="large">
    <form className="pms-workspace-form" onSubmit={submit}>
      {error && <p className="pms-form-error">{error}</p>}
      <label>KPI name<input required maxLength="160" value={values.kpiName} onChange={(event) => set('kpiName', event.target.value)} /></label>
      <label>Linked KRA<select required value={values.kraId} onChange={(event) => set('kraId', event.target.value)}><option value="">Select KRA</option>{kras.map((kra) => <option key={kra.kraId} value={kra.kraId}>{kra.kraName}</option>)}</select></label>
      <label>Measurement type<select value={values.measurementType} onChange={(event) => set('measurementType', event.target.value)}>{measurementOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label>Target<input type="number" min="0.01" step="any" required value={values.targetValue} onChange={(event) => set('targetValue', event.target.value)} /></label>
      <label>Unit<input maxLength="30" value={values.unit || ''} onChange={(event) => set('unit', event.target.value)} placeholder="%, INR, count" /></label>
      <label>Weightage (%)<input type="number" min="0" max="100" step="0.1" required value={values.weightage} onChange={(event) => set('weightage', event.target.value)} /></label>
      <label>Department<select value={values.departmentId} onChange={(event) => set('departmentId', event.target.value)}><option value="">Inherit from KRA</option>{departments.map((item) => <option key={item.departmentId} value={item.departmentId}>{item.departmentName}</option>)}</select></label>
      <label>Owner<select value={values.ownerType} onChange={(event) => set('ownerType', event.target.value)}>{ownerOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label>Owner employee ID<input type="number" min="1" value={values.ownerEmployeeId} onChange={(event) => set('ownerEmployeeId', event.target.value)} disabled={values.ownerType !== 'EMPLOYEE'} required={values.ownerType === 'EMPLOYEE'} /></label>
      <label>Frequency<select value={values.frequency} onChange={(event) => set('frequency', event.target.value)}>{frequencyOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label>Status<select value={values.status} onChange={(event) => set('status', event.target.value)}>{kraStatuses.map((status) => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select></label>
      <footer className="pms-form-actions"><button type="button" className="pms-button pms-button-secondary" onClick={onClose}>Cancel</button><button className="pms-button pms-button-primary" disabled={saving}>{saving ? 'Saving…' : 'Save KPI'}</button></footer>
    </form>
  </Modal>
}

function ChartTooltipContent({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return <div className="pms-workspace-tooltip"><strong>{label || payload[0].name}</strong>{payload.map((entry) => <span key={entry.dataKey}>{entry.name}: {number(entry.value)}</span>)}</div>
}

function Overview({ data, onTab, onCreate, kras, kpis, onExport }) {
  const summary = data?.summary || {}
  const distribution = data?.kraDistribution || []
  const status = data?.kpiStatus || []
  const categories = data?.categories || []
  const alignment = data?.alignment || {}
  const owners = data?.ownership || []
  return <>
    <div className="pms-content-grid pms-grid-three pms-workspace-charts">
      <Panel title="KRA distribution by department" className="pms-workspace-donut">
        <div className="pms-workspace-donut-layout"><div className="pms-workspace-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={distribution} dataKey="count" nameKey="department" innerRadius="62%" outerRadius="88%" paddingAngle={2} stroke="none">{distribution.map((row, index) => <Cell key={row.department} fill={chartColors[index % chartColors.length]} />)}</Pie><Tooltip content={<ChartTooltipContent />} /></PieChart></ResponsiveContainer><div className="pms-donut-center"><strong>{number(summary.totalKras)}</strong><span>Total KRAs</span></div></div><div className="pms-legend">{distribution.map((row, index) => <div className="pms-legend-row" key={row.department}><span><i style={{ background: chartColors[index % chartColors.length] }} />{row.department}</span><strong>{number(row.count)}</strong></div>)}</div></div>
      </Panel>
      <Panel title="KPI status overview" className="pms-workspace-donut">
        <div className="pms-workspace-donut-layout"><div className="pms-workspace-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={status} dataKey="count" nameKey="status" innerRadius="62%" outerRadius="88%" paddingAngle={2} stroke="none">{status.map((row, index) => <Cell key={row.status} fill={chartColors[index % chartColors.length]} />)}</Pie><Tooltip content={<ChartTooltipContent />} /></PieChart></ResponsiveContainer><div className="pms-donut-center"><strong>{number(summary.totalKpis)}</strong><span>Total KPIs</span></div></div><div className="pms-legend">{status.map((row, index) => <div className="pms-legend-row" key={row.status}><span><i style={{ background: chartColors[index % chartColors.length] }} />{row.status.replaceAll('_', ' ')}</span><strong>{number(row.count)}</strong></div>)}</div></div>
      </Panel>
      <Panel title="KRA vs KPI trend" className="pms-workspace-trend"><div className="pms-chart-area"><ResponsiveContainer width="100%" height="100%"><LineChart data={data?.trend || []} margin={{ top: 10, right: 10, left: -18, bottom: 0 }}><CartesianGrid vertical={false} stroke="#edf0f3" /><XAxis dataKey="label" tick={{ fill: '#8793a1', fontSize: 9 }} tickLine={false} axisLine={false} /><YAxis tick={{ fill: '#8793a1', fontSize: 9 }} tickLine={false} axisLine={false} /><Tooltip content={<ChartTooltipContent />} /><Line dataKey="kra" name="KRAs" stroke="#256bd1" strokeWidth={2} dot={false} /><Line dataKey="kpi" name="KPIs" stroke="#1d9a74" strokeWidth={2} dot={false} /></LineChart></ResponsiveContainer></div></Panel>
    </div>
    <div className="pms-content-grid pms-grid-three pms-workspace-lower">
      <Panel title="Top KRA categories"><div className="pms-chart-area pms-workspace-bar"><ResponsiveContainer width="100%" height="100%"><BarChart data={categories.slice(0, 7)} layout="vertical" margin={{ left: 3, right: 18, top: 3, bottom: 0 }}><CartesianGrid horizontal={false} stroke="#edf0f3" /><XAxis type="number" tick={{ fontSize: 8 }} tickLine={false} axisLine={false} /><YAxis type="category" dataKey="category" width={105} tick={{ fill: '#657589', fontSize: 8 }} tickLine={false} axisLine={false} /><Tooltip content={<ChartTooltipContent />} /><Bar dataKey="count" name="KRAs" fill="#256bd1" radius={[0, 3, 3, 0]} barSize={12} /></BarChart></ResponsiveContainer></div></Panel>
      <Panel title="Recent KRA & KPI setup" className="pms-workspace-recent"><div className="pms-workspace-recent-list">{(data?.recent || []).map((row) => <div key={`${row.type}-${row.title}`}><span><strong>{row.title}</strong><small>{row.type} · {row.department}</small></span><Status value={row.status} /><time>{displayDate(row.setupDate)}</time></div>)}{!data?.recent?.length && <EmptyState title="No setup activity yet" />}</div><button className="pms-text-button" onClick={() => onTab('history')}>View setup history</button></Panel>
      <Panel title="KPI setup by status"><div className="pms-workspace-status-bars">{status.map((row, index) => <div key={row.status}><span>{row.status.replaceAll('_', ' ')}</span><i><b style={{ width: `${summary.totalKpis ? Number(row.count) * 100 / summary.totalKpis : 0}%`, background: chartColors[index] }} /></i><strong>{number(row.count)}</strong></div>)}</div></Panel>
    </div>
    <div className="pms-content-grid pms-grid-three pms-workspace-summary-row">
      <Panel title="KRA/KPI alignment summary"><div className="pms-workspace-alignment">{[['Aligned', alignment.aligned], ['Partially aligned', alignment.partiallyAligned], ['Not aligned', alignment.notAligned], ['Not mapped', alignment.notMapped]].map(([label, value]) => <div key={label}><small>{label}</small><strong>{number(value)}</strong></div>)}</div><div className="pms-workspace-score"><span>Alignment score</span><strong>{Number(alignment.alignmentScore || 0).toFixed(1)}%</strong><i><b style={{ width: `${Math.min(100, Number(alignment.alignmentScore || 0))}%` }} /></i></div><button className="pms-text-button" onClick={() => onTab('alignment')}>Review alignment</button></Panel>
      <Panel title="KRA ownership" className="pms-workspace-donut"><div className="pms-workspace-donut-layout"><div className="pms-workspace-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={owners} dataKey="count" nameKey="owner" innerRadius="62%" outerRadius="88%" paddingAngle={2} stroke="none">{owners.map((row, index) => <Cell key={row.owner} fill={chartColors[index % chartColors.length]} />)}</Pie><Tooltip content={<ChartTooltipContent />} /></PieChart></ResponsiveContainer><div className="pms-donut-center"><strong>{number(summary.totalKras)}</strong><span>KRA owners</span></div></div><div className="pms-legend">{owners.map((row, index) => <div className="pms-legend-row" key={row.owner}><span><i style={{ background: chartColors[index % chartColors.length] }} />{row.owner}</span><strong>{number(row.count)}</strong></div>)}</div></div></Panel>
      <Panel title="Quick actions"><div className="pms-workspace-actions"><button onClick={() => onCreate('kra')}><CirclePlus size={15} />Create new KRA</button><button onClick={() => onCreate('kpi')}><Target size={15} />Create new KPI</button><button onClick={() => onTab('department-mapping')}><Building2 size={15} />Map KRA to department</button><button onClick={() => onTab('employee-mapping')}><UsersRound size={15} />Map KPI to employee</button><button onClick={() => onTab('alignment')}><Link2 size={15} />KRA/KPI alignment</button><button onClick={onExport}><Download size={15} />Download setup report</button></div></Panel>
    </div>
    <div className="pms-workspace-hidden-counts" aria-hidden="true">{kras.length + kpis.length}</div>
  </>
}

function LibraryTable({ rows, type, onEdit, onDuplicate, onDelete, onToggle }) {
  if (!rows.length) return <EmptyState title={`No ${type}s found`} detail="Adjust the filters or create a new item." />
  return <div className="pms-table-scroll"><table className="pms-table pms-workspace-library-table"><thead><tr>{type === 'KRA' ? <><th>ID</th><th>KRA title</th><th>Category</th><th>Department</th><th>Description</th><th>Owner</th><th>Status</th><th>KPIs linked</th><th>Created</th></> : <><th>ID</th><th>KPI name</th><th>Linked KRA</th><th>Measurement</th><th>Target</th><th>Weightage</th><th>Department</th><th>Owner</th><th>Frequency</th><th>Status</th></>}<th>Actions</th></tr></thead><tbody>{rows.map((row) => <tr key={type === 'KRA' ? row.kraId : row.kpiId}>
    {type === 'KRA' ? <><td>KRA-{row.kraId}</td><td><strong>{row.kraName}</strong></td><td>{row.categoryName || '—'}</td><td>{row.department || '—'}</td><td className="pms-truncate-cell" title={row.description}>{row.description || '—'}</td><td>{row.owner || row.ownerType}</td><td><Status value={row.status} /></td><td>{row.kpisLinked}</td><td>{displayDate(row.createdDate)}</td></> : <><td>KPI-{row.kpiId}</td><td><strong>{row.kpiName}</strong></td><td>{row.kraName}</td><td>{row.measurementType}</td><td>{number(row.targetValue)} {row.unit}</td><td>{Number(row.weightage || 0)}%</td><td>{row.department || '—'}</td><td>{row.owner || row.ownerType}</td><td>{row.frequency?.replaceAll('_', ' ')}</td><td><Status value={row.status} /></td></>}
    <td><div className="pms-row-actions"><button title="View" aria-label={`View ${row.kraName || row.kpiName}`} onClick={() => onEdit(row, true)}><Eye size={14} /></button><button title="Edit" aria-label={`Edit ${row.kraName || row.kpiName}`} onClick={() => onEdit(row)}><Pencil size={14} /></button><button title="Duplicate" aria-label={`Duplicate ${row.kraName || row.kpiName}`} onClick={() => onDuplicate(row)}><Copy size={14} /></button><button title={row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'} aria-label={`${row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'} ${row.kraName || row.kpiName}`} onClick={() => onToggle(row)}><Activity size={14} /></button><button title="Delete" aria-label={`Delete ${row.kraName || row.kpiName}`} onClick={() => onDelete(row)}><Trash2 size={14} /></button></div></td>
  </tr>)}</tbody></table></div>
}

function MappingPicker({ title, rows, selected, onChange, nameKey, idKey, weightages = {}, setWeightages, showWeightage = false }) {
  return <div className={`pms-mapping-picker ${showWeightage ? 'has-weight' : ''}`}><h3>{title} <span>{selected.size} selected</span></h3><div>{rows.map((row) => {
    const id = String(row[idKey])
    return <label key={id}><input type="checkbox" checked={selected.has(id)} onChange={() => onChange(id)} /><span><strong>{row[nameKey]}</strong><small>{row.department || row.categoryName || row.kraName || row.status}</small></span>{showWeightage && <input type="number" aria-label={`${row[nameKey]} weightage`} min="0" max="100" step="0.1" value={weightages[id] ?? row.weightage ?? 0} onChange={(event) => setWeightages((current) => ({ ...current, [id]: event.target.value }))} disabled={!selected.has(id)} />}</label>
  })}{!rows.length && <EmptyState title={`No ${title.toLowerCase()} available`} />}</div></div>
}

export default function KraKpiSetupPage({ notify }) {
  const { tab: routeTab } = useParams()
  const navigate = useNavigate()
  const activeTab = tabs.some((tab) => tab.id === routeTab) ? routeTab : 'overview'
  const [range, setRange] = useState(defaultRange)
  const [departmentId, setDepartmentId] = useState('')
  const [departments, setDepartments] = useState([])
  const [categories, setCategories] = useState([])
  const [overview, setOverview] = useState(null)
  const [kras, setKras] = useState([])
  const [kpis, setKpis] = useState([])
  const [alignment, setAlignment] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  const [search, setSearch] = useState('')
  const [filters, setFilters] = useState({ status: '', categoryId: '', owner: '' })
  const [sortDescending, setSortDescending] = useState(false)
  const [modal, setModal] = useState(null)
  const [selectedDepartment, setSelectedDepartment] = useState('')
  const [departmentMap, setDepartmentMap] = useState({ kras: new Set(), kpis: new Set() })
  const [employeeSearch, setEmployeeSearch] = useState('')
  const [employeeOptions, setEmployeeOptions] = useState([])
  const [selectedEmployee, setSelectedEmployee] = useState('')
  const [employeeMap, setEmployeeMap] = useState({ kras: new Set(), kpis: new Set(), kraWeights: {}, kpiWeights: {} })
  const [savingMapping, setSavingMapping] = useState(false)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    const query = activeTab === 'kra-library'
      ? { search, sort: 'kraName', direction: sortDescending ? 'desc' : 'asc', ...(departmentId ? { departmentId: Number(departmentId) } : {}), ...(filters.status ? { status: filters.status } : {}), ...(filters.categoryId ? { categoryId: Number(filters.categoryId) } : {}), ...(filters.owner ? { owner: filters.owner } : {}) }
      : activeTab === 'kpi-library'
        ? { search, sort: 'kpiName', direction: sortDescending ? 'desc' : 'asc', ...(departmentId ? { departmentId: Number(departmentId) } : {}), ...(filters.status ? { status: filters.status } : {}), ...(filters.owner ? { owner: filters.owner } : {}) }
        : { ...(departmentId ? { departmentId: Number(departmentId) } : {}) }
    Promise.all([
      pmsService.setupOverview({ ...range, ...(departmentId ? { departmentId: Number(departmentId) } : {}) }, controller.signal),
      pmsService.listKras(activeTab === 'kra-library' ? query : {}, controller.signal),
      pmsService.listKpis(activeTab === 'kpi-library' ? query : {}, controller.signal),
      pmsService.departments(controller.signal),
      pmsService.lookups(controller.signal),
      activeTab === 'alignment' ? pmsService.setupAlignment(controller.signal) : Promise.resolve(null),
      activeTab === 'history' ? pmsService.setupHistory({ limit: 100 }, controller.signal) : Promise.resolve([]),
    ]).then(([summary, kraRows, kpiRows, departmentRows, lookupRows, alignmentRows, historyRows]) => {
      if (controller.signal.aborted) return
      setOverview(summary); setKras(kraRows || []); setKpis(kpiRows || []); setDepartments(departmentRows || [])
      setCategories(lookupRows.categories || []); setAlignment(alignmentRows); setHistory(historyRows || []); setLoading(false)
    }).catch((requestError) => {
      if (!controller.signal.aborted) { setError(getApiErrorMessage(requestError, 'KRA/KPI setup could not be loaded.')); setLoading(false) }
    })
    return () => controller.abort()
  }, [activeTab, range, departmentId, search, filters, sortDescending, revision])

  useEffect(() => {
    if (activeTab !== 'department-mapping' || !selectedDepartment) return undefined
    const controller = new AbortController()
    pmsService.departmentMappings(selectedDepartment, controller.signal).then((mapping) => {
      if (controller.signal.aborted) return
      setDepartmentMap({ kras: new Set(mapping.kras.map((row) => String(row.kraId))), kpis: new Set(mapping.kpis.map((row) => String(row.kpiId))) })
    }).catch((requestError) => notify({ type: 'error', message: getApiErrorMessage(requestError, 'Department mappings could not be loaded.') }))
    return () => controller.abort()
  }, [activeTab, selectedDepartment, revision, notify])

  useEffect(() => {
    if (activeTab !== 'employee-mapping') return undefined
    const controller = new AbortController()
    const timer = window.setTimeout(() => pmsService.employees(employeeSearch, controller.signal)
      .then((result) => { if (!controller.signal.aborted) setEmployeeOptions(result.content || []) })
      .catch((requestError) => { if (!controller.signal.aborted) notify({ type: 'error', message: getApiErrorMessage(requestError, 'Employees could not be loaded.') }) }), 250)
    return () => { window.clearTimeout(timer); controller.abort() }
  }, [activeTab, employeeSearch, notify])

  useEffect(() => {
    if (!selectedEmployee || activeTab !== 'employee-mapping') return undefined
    const controller = new AbortController()
    pmsService.employeeMappings(selectedEmployee, controller.signal).then((mapping) => {
      if (controller.signal.aborted) return
      const kraWeights = Object.fromEntries(mapping.kras.map((row) => [String(row.kraId), row.weightage || 0]))
      const kpiWeights = Object.fromEntries(mapping.kpis.map((row) => [String(row.kpiId), row.weightage || 0]))
      setEmployeeMap({ kras: new Set(mapping.kras.map((row) => String(row.kraId))), kpis: new Set(mapping.kpis.map((row) => String(row.kpiId))), kraWeights, kpiWeights })
    }).catch((requestError) => notify({ type: 'error', message: getApiErrorMessage(requestError, 'Employee mappings could not be loaded.') }))
    return () => controller.abort()
  }, [selectedEmployee, activeTab, revision, notify])

  const visibleEmployees = useMemo(() => employeeOptions.filter((employee) => !departmentId || String(employee.departmentId) === departmentId), [employeeOptions, departmentId])
  const selectedEmployeeRecord = employeeOptions.find((employee) => String(employee.employeeId) === selectedEmployee)

  function refreshData() { setRevision((current) => current + 1) }
  function goTab(tab) { navigate(`/performance/kra-kpi/${tab}`) }
  function openCreate(type) { setModal({ type }) }
  function toggleSet(key, id, value) { setDepartmentMap((current) => ({ ...current, [key]: new Set(current[key].has(id) ? [...current[key]].filter((item) => item !== id) : [...current[key], id]) })) }
  function toggleEmployeeSet(key, id) { setEmployeeMap((current) => ({ ...current, [key]: new Set(current[key].has(id) ? [...current[key]].filter((item) => item !== id) : [...current[key], id]) })) }

  async function saveKra(payload) {
    if (modal.item) await pmsService.updateKra(modal.item.kraId, payload)
    else await pmsService.createKra(payload)
    notify({ type: 'success', message: modal.item ? 'KRA updated.' : 'KRA created.' }); refreshData()
  }
  async function saveKpi(payload) {
    if (modal.item) await pmsService.updateKpi(modal.item.kpiId, payload)
    else await pmsService.createKpi(payload)
    notify({ type: 'success', message: modal.item ? 'KPI updated.' : 'KPI created.' }); refreshData()
  }
  async function duplicate(row, type) {
    try { type === 'KRA' ? await pmsService.duplicateKra(row.kraId) : await pmsService.duplicateKpi(row.kpiId); notify({ type: 'success', message: `${type} duplicated as a draft.` }); refreshData() }
    catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, `${type} could not be duplicated.`) }) }
  }
  async function remove(row, type) {
    const title = row.kraName || row.kpiName
    if (!window.confirm(`Delete “${title}”?`)) return
    try { type === 'KRA' ? await pmsService.deleteKra(row.kraId) : await pmsService.deleteKpi(row.kpiId); notify({ type: 'success', message: `${type} deleted.` }); refreshData() }
    catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, `${type} could not be deleted.`) }) }
  }
  async function toggleStatus(row, type) {
    const active = row.status === 'ACTIVE'
    try {
      if (type === 'KRA') await pmsService.updateKra(row.kraId, { kraName: row.kraName, categoryId: row.categoryId, description: row.description, departmentId: row.departmentId, ownerType: row.ownerType, ownerEmployeeId: row.ownerEmployeeId, weightage: row.weightage, status: active ? 'INACTIVE' : 'ACTIVE', effectiveDate: row.effectiveDate || today })
      else await pmsService.updateKpi(row.kpiId, { kraId: row.kraId, kpiName: row.kpiName, measurementType: row.measurementType, targetValue: row.targetValue, unit: row.unit, weightage: row.weightage, departmentId: row.departmentId, ownerType: row.ownerType, ownerEmployeeId: row.ownerEmployeeId, frequency: row.frequency, status: active ? 'INACTIVE' : 'ACTIVE' })
      notify({ type: 'success', message: `${type} ${active ? 'deactivated' : 'activated'}.` }); refreshData()
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, `${type} status could not be changed.`) }) }
  }
  async function exportReport() {
    setExporting(true)
    try { await pmsService.exportSetup({ ...(departmentId ? { departmentId } : {}) }) }
    catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'KRA/KPI report could not be exported.') }) }
    finally { setExporting(false) }
  }
  async function saveDepartmentMap() {
    if (!selectedDepartment) return
    setSavingMapping(true)
    try {
      await pmsService.saveDepartmentMappings(selectedDepartment, { kraIds: [...departmentMap.kras].map(Number), kpiIds: [...departmentMap.kpis].map(Number) })
      notify({ type: 'success', message: 'Department mappings saved.' }); refreshData()
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Department mappings could not be saved.') }) }
    finally { setSavingMapping(false) }
  }
  async function saveEmployeeMap() {
    if (!selectedEmployee) return
    setSavingMapping(true)
    try {
      await pmsService.saveEmployeeMappings(selectedEmployee, {
        kras: [...employeeMap.kras].map((kraId) => ({ kraId: Number(kraId), weightage: Number(employeeMap.kraWeights[kraId] || 0) })),
        kpis: [...employeeMap.kpis].map((kpiId) => ({ kpiId: Number(kpiId), weightage: Number(employeeMap.kpiWeights[kpiId] || 0) })),
      })
      notify({ type: 'success', message: 'Employee mappings saved.' }); refreshData()
    } catch (requestError) { notify({ type: 'error', message: getApiErrorMessage(requestError, 'Employee mappings could not be saved.') }) }
    finally { setSavingMapping(false) }
  }

  const summary = overview?.summary || {}
  const kraRows = kras.filter((row) => !search || `${row.kraName} ${row.categoryName} ${row.department}`.toLowerCase().includes(search.toLowerCase()))
  const kpiRows = kpis.filter((row) => !search || `${row.kpiName} ${row.kraName} ${row.department}`.toLowerCase().includes(search.toLowerCase()))
  const actionForRow = (row, viewOnly = false, type) => { if (viewOnly) setModal({ type, item: row, view: true }); else setModal({ type, item: row }) }
  const currentPeriod = <div className="pms-period-control"><span>Reporting period</span><input aria-label="Report start date" type="date" value={range.startDate} max={range.endDate} onChange={(event) => setRange((current) => ({ ...current, startDate: event.target.value }))} /><span>to</span><input aria-label="Report end date" type="date" value={range.endDate} min={range.startDate} onChange={(event) => setRange((current) => ({ ...current, endDate: event.target.value }))} /></div>

  return <PageLayout eyebrow="PERFORMANCE MANAGEMENT" title="KRA & KPI Setup" description="Define measurable outcomes and map them across departments and employees." className="pms-page pms-workspace-page">
    <div className="pms-workspace-toolbar">{currentPeriod}<label className="pms-workspace-dept-filter"><Filter size={14} /><select aria-label="Department filter" value={departmentId} onChange={(event) => setDepartmentId(event.target.value)}><option value="">All departments</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.departmentName}</option>)}</select></label><button className="pms-button pms-button-secondary" onClick={exportReport} disabled={exporting}><Download size={14} />{exporting ? 'Exporting…' : 'Export report'}</button></div>
    <nav className="pms-tabs pms-workspace-tabs" aria-label="KRA and KPI views">{tabs.map((tab) => <button key={tab.id} className={activeTab === tab.id ? 'is-active' : ''} onClick={() => goTab(tab.id)}>{tab.label}</button>)}</nav>
    {loading ? <LoadingState label="Loading KRA and KPI setup" /> : error ? <ErrorState message={error} onRetry={refreshData} /> : <>
      <section className="pms-metric-grid pms-workspace-metrics">
        <KpiCard label="Total KRAs" value={summary.totalKras} icon={Target} accent="blue" />
        <KpiCard label="Total KPIs" value={summary.totalKpis} icon={Activity} accent="green" />
        <KpiCard label="Departments" value={summary.departments} icon={Building2} accent="amber" />
        <KpiCard label="Active KRA sets" value={summary.activeKraSets} icon={Check} accent="green" />
        <KpiCard label="Active KPI sets" value={summary.activeKpiSets} icon={Workflow} accent="blue" />
        <KpiCard label="Employees mapped" value={summary.employeesMapped} icon={UsersRound} accent="rose" />
      </section>
      {activeTab === 'overview' && <Overview data={overview} onTab={goTab} onCreate={openCreate} kras={kras} kpis={kpis} onExport={exportReport} />}
      {(activeTab === 'kra-library' || activeTab === 'kpi-library') && <Panel title={activeTab === 'kra-library' ? `KRA Library · ${number(kraRows.length)}` : `KPI Library · ${number(kpiRows.length)}`} className="pms-workspace-library" action={<button className="pms-button pms-button-primary" onClick={() => openCreate(activeTab === 'kra-library' ? 'kra' : 'kpi')}><CirclePlus size={14} />Create {activeTab === 'kra-library' ? 'KRA' : 'KPI'}</button>}>
        <div className="pms-workspace-library-filters"><label className="pms-search"><Search size={14} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder={`Search ${activeTab === 'kra-library' ? 'KRAs, categories or departments' : 'KPIs, linked KRAs or departments'}`} /></label><select aria-label="Filter by status" value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}><option value="">All statuses</option>{kraStatuses.map((status) => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select>{activeTab === 'kra-library' && <select aria-label="Filter by category" value={filters.categoryId} onChange={(event) => setFilters((current) => ({ ...current, categoryId: event.target.value }))}><option value="">All categories</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select>}<select aria-label="Filter by owner" value={filters.owner} onChange={(event) => setFilters((current) => ({ ...current, owner: event.target.value }))}><option value="">All owners</option>{ownerOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select><button className="pms-icon-button" title={`Sort ${sortDescending ? 'ascending' : 'descending'}`} aria-label={`Sort ${sortDescending ? 'ascending' : 'descending'}`} onClick={() => setSortDescending((current) => !current)}><ArrowDownUp size={15} /></button></div>
        <LibraryTable rows={activeTab === 'kra-library' ? kraRows : kpiRows} type={activeTab === 'kra-library' ? 'KRA' : 'KPI'} onEdit={(row, view) => actionForRow(row, view, activeTab === 'kra-library' ? 'kra' : 'kpi')} onDuplicate={(row) => duplicate(row, activeTab === 'kra-library' ? 'KRA' : 'KPI')} onDelete={(row) => remove(row, activeTab === 'kra-library' ? 'KRA' : 'KPI')} onToggle={(row) => toggleStatus(row, activeTab === 'kra-library' ? 'KRA' : 'KPI')} />
      </Panel>}
      {activeTab === 'department-mapping' && <Panel title="Department mapping" className="pms-workspace-mapping"><div className="pms-workspace-mapping-toolbar"><label>Department<select value={selectedDepartment} onChange={(event) => setSelectedDepartment(event.target.value)}><option value="">Select department</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.departmentName}</option>)}</select></label><span>{departmentMap.kras.size} KRAs · {departmentMap.kpis.size} KPIs mapped</span><button className="pms-button pms-button-primary" disabled={!selectedDepartment || savingMapping} onClick={saveDepartmentMap}>{savingMapping ? 'Saving…' : 'Save mappings'}</button></div>{selectedDepartment ? <div className="pms-workspace-mapping-grid"><MappingPicker title="KRAs" rows={kras} selected={departmentMap.kras} onChange={(id) => toggleSet('kras', id)} nameKey="kraName" idKey="kraId" /><MappingPicker title="KPIs" rows={kpis} selected={departmentMap.kpis} onChange={(id) => toggleSet('kpis', id)} nameKey="kpiName" idKey="kpiId" /></div> : <EmptyState title="Choose a department" detail="Its mapped KRAs and KPIs will appear here." />}</Panel>}
      {activeTab === 'employee-mapping' && <Panel title="Employee mapping" className="pms-workspace-mapping"><div className="pms-workspace-mapping-toolbar"><label className="pms-search"><Search size={14} /><input value={employeeSearch} onChange={(event) => setEmployeeSearch(event.target.value)} placeholder="Search name or employee ID" /></label><label>Department<select value={departmentId} onChange={(event) => setDepartmentId(event.target.value)}><option value="">All departments</option>{departments.map((department) => <option key={department.departmentId} value={department.departmentId}>{department.departmentName}</option>)}</select></label><label>Employee<select value={selectedEmployee} onChange={(event) => setSelectedEmployee(event.target.value)}><option value="">Choose employee</option>{visibleEmployees.map((employee) => <option key={employee.employeeId} value={employee.employeeId}>{employee.displayName} · {employee.employeeCode}</option>)}</select></label>{selectedEmployeeRecord && <span>{selectedEmployeeRecord.departmentName || selectedEmployeeRecord.department || 'Department not set'} · {selectedEmployeeRecord.designationName || selectedEmployeeRecord.designation || 'Designation not set'}</span>}<button className="pms-button pms-button-primary" disabled={!selectedEmployee || savingMapping} onClick={saveEmployeeMap}>{savingMapping ? 'Saving…' : 'Save assignment'}</button></div>{selectedEmployee ? <div className="pms-workspace-mapping-grid"><MappingPicker title="Assigned KRAs" rows={kras} selected={employeeMap.kras} onChange={(id) => toggleEmployeeSet('kras', id)} nameKey="kraName" idKey="kraId" weightages={employeeMap.kraWeights} setWeightages={(value) => setEmployeeMap((current) => ({ ...current, kraWeights: typeof value === 'function' ? value(current.kraWeights) : value }))} showWeightage /><MappingPicker title="Assigned KPIs" rows={kpis} selected={employeeMap.kpis} onChange={(id) => toggleEmployeeSet('kpis', id)} nameKey="kpiName" idKey="kpiId" weightages={employeeMap.kpiWeights} setWeightages={(value) => setEmployeeMap((current) => ({ ...current, kpiWeights: typeof value === 'function' ? value(current.kpiWeights) : value }))} showWeightage /></div> : <EmptyState title="Choose an employee" detail="Search by employee name or ID, then assign KRAs and KPIs." />}</Panel>}
      {activeTab === 'alignment' && <Panel title="KRA → KPI → Employee → Target → Measurement → Weightage" className="pms-workspace-alignment-panel"><div className="pms-workspace-alignment-top"><div><strong>{number(alignment?.summary?.aligned)}</strong><span>Aligned</span></div><div><strong>{number(alignment?.summary?.partiallyAligned)}</strong><span>Partially aligned</span></div><div><strong>{number(alignment?.summary?.notAligned)}</strong><span>Not aligned</span></div><div><strong>{number(alignment?.summary?.notMapped)}</strong><span>Not mapped</span></div><strong>Score {Number(alignment?.summary?.alignmentScore || 0).toFixed(1)}%</strong></div><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>KRA</th><th>KPI</th><th>Employees</th><th>Target</th><th>Measurement</th><th>Weightage</th><th>Status</th><th>Action</th></tr></thead><tbody>{(alignment?.rows || []).map((row) => { const kpi = kpis.find((item) => item.kpiId === row.kpiId); const isAligned = row.employees > 0; return <tr key={`${row.kraId}-${row.kpiId || 'none'}`}><td>{row.kra}</td><td>{row.kpi}</td><td>{number(row.employees)}</td><td>{kpi ? `${number(kpi.targetValue)} ${kpi.unit || ''}` : '—'}</td><td>{kpi?.measurementType || '—'}</td><td>{kpi ? `${Number(kpi.weightage || 0)}%` : '—'}</td><td><Status value={row.kpiId ? isAligned ? 'ACTIVE' : 'UNDER_REVIEW' : 'DRAFT'} /></td><td><button className="pms-text-button" onClick={() => goTab('employee-mapping')}>Fix mapping</button></td></tr> })}</tbody></table></div></Panel>}
      {activeTab === 'history' && <Panel title="Setup history" className="pms-workspace-library"><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>Date</th><th>User</th><th>Action</th><th>Module</th><th>KRA/KPI</th><th>Old value</th><th>New value</th></tr></thead><tbody>{history.map((row) => <tr key={row.historyId}><td>{displayDate(row.changedAt)}</td><td>{row.changedBy || '—'}</td><td>{row.actionType.replaceAll('_', ' ')}</td><td>{row.moduleName}</td><td>{row.itemTitle || row.itemId || '—'}</td><td>{row.oldValue || '—'}</td><td>{row.newValue || '—'}</td></tr>)}</tbody></table></div>{!history.length && <EmptyState title="No setup history yet" detail="Create or map a KRA/KPI to start the audit trail." />}</Panel>}
      <footer className="pms-footer">Northstar People Operations <span>·</span> Performance Management</footer>
    </>}
    {modal && modal.type === 'kra' && !modal.view && <KraForm item={modal.item} lookups={categories} departments={departments} onClose={() => setModal(null)} onSave={saveKra} />}
    {modal && modal.type === 'kpi' && !modal.view && <KpiForm item={modal.item} kras={kras} departments={departments} onClose={() => setModal(null)} onSave={saveKpi} />}
    {modal?.view && <Modal title={modal.item.kraName || modal.item.kpiName} description={modal.type === 'kra' ? `KRA-${modal.item.kraId}` : `KPI-${modal.item.kpiId}`} onClose={() => setModal(null)}><div className="pms-workspace-detail">{Object.entries(modal.item).map(([key, value]) => <div key={key}><small>{key.replace(/[A-Z]/g, (letter) => ` ${letter.toLowerCase()}`)}</small><strong>{String(value ?? '—')}</strong></div>)}</div></Modal>}
  </PageLayout>
}
