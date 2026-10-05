import { useMemo, useState } from 'react'
import { Area, AreaChart, Bar, BarChart, Cell, CartesianGrid, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Activity, ArrowDownRight, ArrowUpRight, CalendarDays, Download, Filter, RefreshCw } from 'lucide-react'
import { EmptyState, ErrorState, LoadingState } from '../components/ui'
import './pms.css'

export const DEPARTMENTS = ['IT', 'HR', 'Sales & Marketing', 'Operations', 'Finance', 'Others']
export const STATUS_COLORS = { COMPLETED: '#18866d', IN_PROGRESS: '#3975ca', BEHIND: '#d48a32', NOT_STARTED: '#9da8b5' }
export const STATUS_LABELS = { COMPLETED: 'Completed', IN_PROGRESS: 'In Progress', BEHIND: 'Behind', NOT_STARTED: 'Not Started' }
const TREND_SERIES = [
  { key: 'completed', label: 'Completed', color: STATUS_COLORS.COMPLETED },
  { key: 'inProgress', label: 'In Progress', color: STATUS_COLORS.IN_PROGRESS },
  { key: 'behind', label: 'Behind', color: STATUS_COLORS.BEHIND },
  { key: 'notStarted', label: 'Not Started', color: STATUS_COLORS.NOT_STARTED },
]
export const localDate = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
export const defaultRange = () => ({ startDate: localDate(new Date(Date.now() - 25 * 86400000)), endDate: localDate(new Date()) })
export const moneyNumber = (value) => Number(value || 0).toLocaleString('en-IN', { maximumFractionDigits: 1 })

export function GoalStatus({ status }) {
  return <span className={`pms-status status-${String(status || '').toLowerCase().replaceAll('_', '-')}`}>{STATUS_LABELS[status] || String(status || '').replaceAll('_', ' ')}</span>
}

export function ProgressBar({ value = 0 }) {
  const n = Math.max(0, Math.min(100, Number(value) || 0))
  return <span className="pms-progress"><i className="pms-progress-track"><b style={{ width: `${n}%` }} /></i><strong>{n.toFixed(n % 1 ? 1 : 0)}%</strong></span>
}

export function PageControls({ range, onRangeChange, department, onDepartmentChange, onExport, exporting, tabs, activeTab, onTabChange }) {
  const [filtersOpen, setFiltersOpen] = useState(false)
  return <>
    <div className="pms-heading-controls">
      <div className="pms-period-control"><CalendarDays size={16} /><input aria-label="From date" type="date" value={range.startDate} max={range.endDate} onChange={(e) => onRangeChange({ ...range, startDate: e.target.value })} /><span>to</span><input aria-label="To date" type="date" value={range.endDate} min={range.startDate} max={localDate(new Date())} onChange={(e) => onRangeChange({ ...range, endDate: e.target.value })} /></div>
      <div className="pms-filter-wrap"><button className={`pms-button pms-button-secondary ${department ? 'has-filter' : ''}`} onClick={() => setFiltersOpen((v) => !v)}><Filter size={15} /> Filters</button>{filtersOpen && <div className="pms-filter-popover"><label>Department group<select value={department} onChange={(e) => onDepartmentChange(e.target.value)}><option value="">All Departments</option>{DEPARTMENTS.map((d) => <option key={d}>{d}</option>)}</select></label><button className="pms-text-button" onClick={() => onDepartmentChange('')}>Clear filter</button></div>}</div>
      <button className="pms-button pms-button-primary" onClick={onExport} disabled={exporting}><Download size={15} />{exporting ? 'Exporting' : 'Export report'}</button>
    </div>
    <nav className="pms-tabs" aria-label="Performance views">{tabs.map((tab) => <button key={tab.id} onClick={() => onTabChange(tab.id)} className={activeTab === tab.id ? 'is-active' : ''}>{tab.label}</button>)}</nav>
  </>
}

export function Panel({ title, action, className = '', children }) {
  return <section className={`pms-panel ${className}`}><header className="pms-panel-heading"><h2>{title}</h2>{action}</header>{children}</section>
}

export function MetricCards({ values, labels, invert = [] }) {
  return <div className="pms-metric-grid">{labels.map(([key, label, suffix = ''], index) => {
    const metric = values?.[key] || { value: 0, change: 0, changePercent: null }
    const up = Number(metric.change) >= 0
    const positive = invert.includes(key) ? !up : up
    const Icon = up ? ArrowUpRight : ArrowDownRight
    return <article key={key} className={`pms-metric metric-tone-${index % 5}`}><div className="pms-metric-top"><span>{label}</span><span className="pms-metric-icon"><Activity size={17} /></span></div><strong>{moneyNumber(metric.value)}{suffix}</strong><div className="pms-metric-foot"><span>{metric.changePercent == null ? `${up ? '+' : ''}${moneyNumber(metric.change)} vs previous` : 'vs previous period'}</span><span className={positive ? 'metric-good' : 'metric-bad'}><Icon size={13} />{metric.changePercent == null ? moneyNumber(Math.abs(metric.change)) : `${Math.abs(metric.changePercent)}%`}</span></div></article>
  })}</div>
}

const tooltipStyle = { border: '1px solid #e3e9ee', borderRadius: 5, fontSize: 11, boxShadow: '0 7px 20px #23364b18' }
export function TrendChart({ data, mode = 'counts', granularity, onGranularityChange }) {
  const chartData = useMemo(() => data || [], [data])
  const series = mode === 'counts' ? TREND_SERIES : [{ key: 'averageProgress', label: 'Average progress', color: '#3975ca' }]
  return <Panel title="Goal progress trend" className="pms-trend-panel" action={onGranularityChange && <select className="pms-compact-select" value={granularity} onChange={(e) => onGranularityChange(e.target.value)} aria-label="Trend granularity"><option value="DAILY">Daily</option><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option></select>}>
    {!chartData.length ? <EmptyState title="No trend data" /> : <div className="pms-chart-area"><ResponsiveContainer width="100%" height="100%"><AreaChart data={chartData} margin={{ top: 12, right: 10, left: -18, bottom: 0 }}><CartesianGrid stroke="#edf0f3" vertical={false} /><XAxis dataKey="label" tick={{ fill: '#8793a1', fontSize: 9 }} tickLine={false} axisLine={false} minTickGap={25} /><YAxis tick={{ fill: '#8793a1', fontSize: 9 }} tickLine={false} axisLine={false} /><Tooltip contentStyle={tooltipStyle} />{series.map((item) => <Area key={item.key} type="monotone" dataKey={item.key} name={item.label} stroke={item.color} fill={`${item.color}18`} strokeWidth={2} />)}</AreaChart></ResponsiveContainer></div>}
  </Panel>
}

export function DonutChart({ title, rows, nameKey, valueKey, colors, centerValue, centerLabel, className = '' }) {
  const total = rows?.reduce((sum, row) => sum + Number(row[valueKey] || 0), 0) || 0
  return <Panel title={title} className={className}><div className="pms-donut-layout"><div className="pms-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={rows || []} dataKey={valueKey} nameKey={nameKey} innerRadius="64%" outerRadius="86%" paddingAngle={2} stroke="none">{(rows || []).map((row, index) => <Cell key={`${row[nameKey]}-${index}`} fill={colors[index % colors.length]} />)}</Pie><Tooltip contentStyle={tooltipStyle} /></PieChart></ResponsiveContainer><div className="pms-donut-center"><strong>{centerValue ?? moneyNumber(total)}</strong><span>{centerLabel || 'Total'}</span></div></div><div className="pms-legend">{(rows || []).map((row, index) => <div className="pms-legend-row" key={`${row[nameKey]}-${index}`}><span><i style={{ background: colors[index % colors.length] }} />{row[nameKey]}</span><strong>{moneyNumber(row[valueKey])}</strong><small>{total ? `${(Number(row[valueKey] || 0) * 100 / total).toFixed(1)}%` : '0%'}</small></div>)}</div></div></Panel>
}

export function BarChartPanel({ title, rows, categoryKey, valueKey, color = '#3975ca', formatter = moneyNumber, className = '' }) {
  return <Panel title={title} className={className}><div className="pms-bar-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={rows || []} layout="vertical" margin={{ top: 8, right: 28, left: 2, bottom: 0 }}><CartesianGrid stroke="#edf0f3" horizontal={false} /><XAxis type="number" tick={{ fill: '#8994a2', fontSize: 9 }} tickLine={false} axisLine={false} /><YAxis type="category" dataKey={categoryKey} width={112} tick={{ fill: '#5d6b7e', fontSize: 9 }} tickLine={false} axisLine={false} /><Tooltip contentStyle={tooltipStyle} formatter={formatter} /><Bar dataKey={valueKey} fill={color} radius={[0, 4, 4, 0]} barSize={14} /></BarChart></ResponsiveContainer></div></Panel>
}

export function DashboardTable({ rows }) {
  return <Panel title="Department goal performance" className="pms-table-panel"><div className="pms-table-scroll"><table className="pms-table"><thead><tr><th>Department</th><th>Total</th><th>Completed</th><th>In progress</th><th>Behind</th><th>Progress</th></tr></thead><tbody>{rows?.map((row) => <tr key={row.department}><td>{row.department}</td><td>{row.total}</td><td>{row.completed}</td><td>{row.inProgress}</td><td>{row.behind}</td><td><ProgressBar value={row.averageProgress} /></td></tr>)}</tbody><tfoot><tr><th>Total</th><th>{rows?.reduce((a, x) => a + x.total, 0)}</th><th>{rows?.reduce((a, x) => a + x.completed, 0)}</th><th>{rows?.reduce((a, x) => a + x.inProgress, 0)}</th><th>{rows?.reduce((a, x) => a + x.behind, 0)}</th><th /></tr></tfoot></table></div></Panel>
}

export function DashboardDeadlines({ rows }) {
  return <Panel title="Upcoming deadlines">{rows?.length ? <div className="pms-deadline-list">{rows.map((row) => <div className="pms-deadline" key={`${row.name}-${row.dueDate}`}><span className="pms-deadline-date"><b>{new Date(`${row.dueDate}T00:00:00`).getDate()}</b><small>{new Date(`${row.dueDate}T00:00:00`).toLocaleString('en', { month: 'short' })}</small></span><span className="pms-deadline-copy"><strong>{row.name}</strong><small>{row.department} · {row.pending} pending of {row.employees}</small></span><span className="pms-days-left">{row.daysLeft}d</span></div>)}</div> : <EmptyState title="No upcoming deadlines" />}</Panel>
}

export function TopPerformers({ rows }) {
  return <Panel title="Top performers"><div className="pms-performer-list">{rows?.map((row, i) => <div className="pms-performer" key={row.employeeId}><span className={`pms-rank rank-${i + 1}`}>{String(i + 1).padStart(2, '0')}</span><span className="pms-performer-name"><strong>{row.name}</strong><small>{row.department} · {row.designation}</small></span><b>{Number(row.rating).toFixed(2)}</b></div>)}</div></Panel>
}

export function DepartmentPerformance({ rows }) {
  return <BarChartPanel title="Average rating by department" rows={rows} categoryKey="department" valueKey="averageRating" formatter={(value) => [Number(value).toFixed(2), 'Rating']} color="#19876d" />
}

export function AppraisalPanel({ data }) {
  const rows = [
    { name: 'Completed', value: data?.completed || 0 }, { name: 'HR Review', value: data?.hrReview || 0 },
    { name: 'Manager Review', value: data?.managerReview || 0 }, { name: 'Self Appraisal', value: data?.selfAppraisal || 0 },
    { name: 'Yet to Start', value: data?.yetToStart || 0 },
  ]
  return <DonutChart title="Appraisal progress" rows={rows} nameKey="name" valueKey="value" colors={['#19866c', '#4b94b3', '#3975ca', '#d88a35', '#aab3bd']} centerValue={moneyNumber(data?.totalEmployees)} centerLabel="Employees" />
}

export function PipPanel({ data }) {
  const rows = [{ name: 'Active', value: data?.active || 0 }, { name: 'Improved', value: data?.improved || 0 }, { name: 'Not improved', value: data?.notImproved || 0 }]
  return <DonutChart title="Performance improvement plans" rows={rows} nameKey="name" valueKey="value" colors={['#3975ca', '#18866d', '#d47647']} centerValue={`${Number(data?.successRate || 0).toFixed(1)}%`} centerLabel="Success rate" />
}

export function LoadingPanel({ error, onRetry }) {
  if (error) return <ErrorState message={error} onRetry={onRetry} />
  return <LoadingState label="Loading performance data" />
}

export function PageFooter() {
  return <footer className="pms-footer">Northstar People Operations <span>·</span> Performance Management</footer>
}

export function RefreshButton({ onClick }) {
  return <button className="pms-icon-button" onClick={onClick} title="Refresh data" aria-label="Refresh data"><RefreshCw size={15} /></button>
}
