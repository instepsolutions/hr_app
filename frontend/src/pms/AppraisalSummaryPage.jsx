import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Activity, BadgeCheck, CalendarDays, Check, CircleAlert, Download, Lightbulb, Search, ShieldCheck, Star, Target, UsersRound } from 'lucide-react'
import { EmptyState, ErrorState, LoadingState, Modal, PageLayout, StatCard } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { pmsService } from '../services/pmsService'
import './pms.css'
import './pms-workspace.css'
import './case-workflows.css'

const tabs = [['overview','Overview'],['department-summary','Department Summary'],['rating-distribution','Rating Distribution'],['performance-by-rating','Performance by Rating'],['reviewer-summary','Reviewer Summary'],['appraisal-timeline','Appraisal Timeline']]
const ratings = ['Poor','Below Average','Average','Good','Excellent']
const colors = ['#c65b52','#dc8b35','#70879c','#367bd0','#17866d']
const number = (value) => Number(value || 0).toLocaleString()
const displayDate = (value) => value ? new Intl.DateTimeFormat('en-GB',{day:'2-digit',month:'short',year:'numeric'}).format(new Date(`${String(value).slice(0,10)}T00:00:00`)) : '—'
const score = (value) => value == null || value === '' ? '—' : `${Number(value).toFixed(2)} / 5`

function Panel({ title, children, action, className = '' }) {
  return <section className={`pms-panel ${className}`}><header className="pms-panel-heading"><h2>{title}</h2>{action}</header>{children}</section>
}

function Table({ columns, rows, onRow, empty = 'No appraisal records match these filters.' }) {
  return <div className="pms-table-scroll"><table className="pms-table case-table"><thead><tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr></thead><tbody>{rows.map((row) => <tr key={row.appraisalId} onClick={() => onRow?.(row)}>{columns.map((column) => <td key={column.key}>{column.render ? column.render(row) : row[column.key] ?? '—'}</td>)}</tr>)}{!rows.length && <tr><td colSpan={columns.length}><EmptyState title={empty} /></td></tr>}</tbody></table></div>
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return <div className="pms-workspace-tooltip"><strong>{label || payload[0].name}</strong>{payload.map((entry) => <span key={entry.dataKey}>{entry.name}: {entry.value}</span>)}</div>
}

function summaryFromRows(rows) {
  const completed = rows.filter((row) => row.finalRating != null || row.stage === 'COMPLETED').length
  const inProgress = rows.filter((row) => row.finalRating == null && ['SELF_APPRAISAL','MANAGER_REVIEW','HR_REVIEW'].includes(row.stage)).length
  const yetToStart = rows.filter((row) => row.finalRating == null && row.stage === 'YET_TO_START').length
  const rated = rows.filter((row) => row.finalRating != null)
  return { totalEmployees: rows.length, completed, inProgress, yetToStart, completionRate: rows.length ? completed * 100 / rows.length : 0, averageRating: rated.length ? rated.reduce((sum,row) => sum + Number(row.finalRating),0) / rated.length : 0 }
}

function EmployeeSummary({ detail, onInitiatePip, onClose }) {
  const data = detail.selfAppraisal || {}
  const manager = detail.managerReview || {}
  const hr = detail.hrReview || {}
  const goals = detail.goals || []
  const recommendation = detail.finalRecommendation || hr.finalRecommendation || detail.recommendation
  const hasPipSignal = Number(detail.finalRating || 5) < Number(detail.lowRatingThreshold || 2.5)
    || String(manager.pipRecommendation).toUpperCase() === 'YES'
    || String(hr.finalRecommendation).toUpperCase() === 'PIP'
    || String(recommendation).toUpperCase() === 'PIP'
  return <Modal title={detail.employeeName || 'Appraisal summary'} description={`${detail.cycleName || 'Appraisal cycle'} · ${detail.employeeCode || ''}`} size="large" onClose={onClose}>
    <div className="case-detail-body">
      <div className="case-detail-header-grid">{[['Employee ID',detail.employeeCode],['Department',detail.department],['Designation',detail.designation],['Manager',detail.manager],['Location',detail.location],['Joining date',displayDate(detail.joiningDate)],['Self rating',score(detail.selfRating)],['Manager rating',score(detail.managerRating)],['HR rating',score(detail.hrRating)],['Final rating',score(detail.finalRating)]].map(([label,value]) => <div key={label}><small>{label}</small><strong>{value || '—'}</strong></div>)}</div>
      <div className="case-trail"><span>Self Appraisal</span><i /><span>Manager Review</span><i /><span>HR Review</span><i /><b>Finalized</b></div>
      <Panel title="Goals, KRA and KPI"><div className="pms-table-scroll"><table className="pms-table case-table"><thead><tr><th>Goal</th><th>KRA / KPI</th><th>Target</th><th>Progress</th><th>Weightage</th></tr></thead><tbody>{goals.map((goal) => <tr key={goal.goalId}><td>{goal.title}</td><td>{goal.kra || '—'} / {goal.kpi || '—'}</td><td>{goal.target ?? '—'} {goal.unit}</td><td>{Number(goal.progress || 0).toFixed(0)}%</td><td>{goal.weightage ?? '—'}%</td></tr>)}{!goals.length && <tr><td colSpan="5">No goal records in this cycle.</td></tr>}</tbody></table></div></Panel>
      <div className="pms-content-grid pms-grid-three"><Panel title="Employee reflection"><div className="case-notes"><p><small>Achievements</small>{data.achievements || '—'}</p><p><small>Comments</small>{data.employeeComments || '—'}</p><p><small>Development needs</small>{data.developmentNeeds || '—'}</p></div></Panel><Panel title="Manager feedback"><div className="case-notes"><p><small>Strengths</small>{manager.strengths || '—'}</p><p><small>Improvement areas</small>{manager.areasForImprovement || '—'}</p><p><small>Comments</small>{manager.managerComments || '—'}</p></div></Panel><Panel title="HR decision"><div className="case-notes"><p><small>Recommendation</small>{recommendation || '—'}</p><p><small>Calibration reason</small>{hr.calibrationReason || '—'}</p><p><small>Final remarks</small>{hr.finalRemarks || '—'}</p></div></Panel></div>
      {detail.pipStatus && <div className="case-inline-state"><ShieldCheck size={15} />PIP status: <strong>{detail.pipStatus}</strong></div>}
      <footer className="case-actions"><button className="pms-button pms-button-secondary" onClick={onClose}>Close</button>{hasPipSignal && !detail.pipStatus && <button className="pms-button pms-button-primary" onClick={() => onInitiatePip({ employeeId: detail.employeeId, employeeName: detail.employeeName, employeeCode: detail.employeeCode, department: detail.department, manager: detail.manager, managerId: detail.managerId, designation: detail.designation, relatedAppraisalId: detail.appraisalId, cycleName: detail.cycleName, finalRating: detail.finalRating, reasonCode: Number(detail.finalRating) < 2.5 ? 'LOW_APPRAISAL_RATING' : 'HR_RECOMMENDATION', reason: `PIP recommended from ${detail.cycleName || 'appraisal'}; final rating ${score(detail.finalRating)}.`, currentPerformance: `Final appraisal rating: ${score(detail.finalRating)}`, expectedPerformance: 'Meet agreed goals and role expectations', performanceGap: manager.areasForImprovement || data.challenges || '', evidence: manager.managerComments || hr.finalRemarks || '', objectives: goals.filter((goal) => Number(goal.progress || 0) < 100).map((goal) => ({ goalId: goal.goalId, kraId: goal.kraId, kpiId: goal.kpiId, kra: goal.kra, kpi: goal.kpi, title: goal.title, currentState: `${goal.progress || 0}% complete`, expectedState: 'Complete target by agreed review date', measurement: goal.kpi || goal.kra || 'Goal progress', targetValue: goal.target, weightage: goal.weightage, progress: goal.progress || 0 })) })}><ShieldCheck size={14} />Initiate PIP</button>}</footer>
    </div>
  </Modal>
}

export default function AppraisalSummaryPage({ notify }) {
  const { tab: routeTab } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const activeTab = tabs.some(([id]) => id === routeTab) ? routeTab : 'overview'
  const [report,setReport] = useState(null)
  const [rows,setRows] = useState([])
  const [departments,setDepartments] = useState([])
  const [cycleId,setCycleId] = useState('')
  const [departmentId,setDepartmentId] = useState('')
  const [search,setSearch] = useState('')
  const [status,setStatus] = useState('')
  const [rating,setRating] = useState('')
  const [threshold,setThreshold] = useState('2.5')
  const [detail,setDetail] = useState(null)
  const [loading,setLoading] = useState(true)
  const [error,setError] = useState('')
  const [revision,setRevision] = useState(0)
  const [openPopover,setOpenPopover] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true); setError('')
    const params = { lowRatingThreshold: Number(threshold), ...(cycleId ? { cycleId } : {}), ...(departmentId ? { departmentId } : {}), ...(search ? { search } : {}) }
    Promise.all([pmsService.appraisalSummaryOverview(params,controller.signal),pmsService.appraisalSummaryList(params,controller.signal),pmsService.departments(controller.signal)])
      .then(([summary,appraisals,departmentRows]) => { if(controller.signal.aborted)return;setReport(summary);setRows(appraisals||[]);setDepartments(departmentRows||[]);if(!cycleId&&summary.cycles?.length)setCycleId(String(summary.cycles[0].cycleId));setLoading(false) })
      .catch((requestError) => { if(!controller.signal.aborted){setError(getApiErrorMessage(requestError,'Appraisal summary could not be loaded.'));setLoading(false)} })
    return () => controller.abort()
  },[cycleId,departmentId,search,threshold,revision])

  const filteredRows = useMemo(() => rows.filter((row) => (!status || (status==='COMPLETED' ? row.finalRating != null : status==='IN_PROGRESS' ? row.finalRating == null && row.stage!=='YET_TO_START' : row.stage==='YET_TO_START'))
    && (!rating || (row.finalRating != null && Math.round(Number(row.finalRating))===Number(rating)))),[rows,status,rating])
  const hasListFilter = Boolean(status || rating)
  const filteredSummary = useMemo(() => summaryFromRows(filteredRows),[filteredRows])
  const filteredDepartments = useMemo(() => [...new Set(filteredRows.map(row=>row.department||'Others'))].map(department=>{
    const members=filteredRows.filter(row=>(row.department||'Others')===department)
    const completed=members.filter(row=>row.finalRating!=null||row.stage==='COMPLETED').length
    const inProgress=members.filter(row=>row.finalRating==null&&['SELF_APPRAISAL','MANAGER_REVIEW','HR_REVIEW'].includes(row.stage)).length
    const rated=members.filter(row=>row.finalRating!=null)
    return {department,totalEmployees:members.length,completed,inProgress,yetToStart:members.length-completed-inProgress,completionRate:members.length?completed*100/members.length:0,averageRating:rated.length?rated.reduce((sum,row)=>sum+Number(row.finalRating),0)/rated.length:0}
  }),[filteredRows])
  const filteredRatings = useMemo(() => [1,2,3,4,5].map(value=>({rating:value,count:filteredRows.filter(row=>row.finalRating!=null&&Math.round(Number(row.finalRating))===value).length})),[filteredRows])
  const filteredCategories = useMemo(() => {
    const ranges=report?.categories||[]
    const bounds={excellent:ranges.find(row=>row.name==='Excellent')?.minimum??4.5,good:ranges.find(row=>row.name==='Good')?.minimum??3.5,average:ranges.find(row=>row.name==='Average')?.minimum??2.5,below:ranges.find(row=>row.name==='Below Average')?.minimum??1.5}
    const counts={'Excellent':0,'Good':0,'Average':0,'Below Average':0,'Poor':0}
    for(const row of filteredRows){if(row.finalRating==null)continue;const value=Number(row.finalRating);counts[value>=bounds.excellent?'Excellent':value>=bounds.good?'Good':value>=bounds.average?'Average':value>=bounds.below?'Below Average':'Poor']++}
    const total=Object.values(counts).reduce((sum,value)=>sum+value,0)
    return Object.entries(counts).map(([name,count])=>({name,count,minimum:name==='Excellent'?bounds.excellent:name==='Good'?bounds.good:name==='Average'?bounds.average:name==='Below Average'?bounds.below:1,maximum:name==='Excellent'?5:name==='Good'?bounds.excellent:name==='Average'?bounds.good:name==='Below Average'?bounds.average:bounds.below,percent:total?count*100/total:0}))
  },[filteredRows,report])
  const summary = hasListFilter ? filteredSummary : report?.summary || filteredSummary
  const stats = [
    {label:'Total employees',value:number(summary.totalEmployees),note:'Employees in selected cycle',icon:UsersRound,accent:'blue'},
    {label:'Appraisals completed',value:number(summary.completed),note:'Final ratings available',icon:Check,accent:'green'},
    {label:'In progress',value:number(summary.inProgress),note:'Self, manager or HR stage',icon:Activity,accent:'amber'},
    {label:'Yet to start',value:number(summary.yetToStart),note:'No submitted self appraisal',icon:CalendarDays,accent:'rose'},
    {label:'Completion rate',value:`${Number(summary.completionRate||0).toFixed(1)}%`,note:'Completed / total',icon:BadgeCheck,accent:'blue'},
    {label:'Overall average rating',value:`${Number(summary.averageRating||0).toFixed(2)} / 5`,note:'Final calibrated ratings',icon:Star,accent:'green'},
  ]
  const goTab = (next) => navigate(`/performance/appraisal-summary/${next}`)
  const refresh = () => setRevision(value=>value+1)
  const exportReport = async () => { try { await pmsService.exportAppraisalSummary({lowRatingThreshold:Number(threshold),...(cycleId?{cycleId}:{}),...(departmentId?{departmentId}:{})}) } catch(requestError){notify({type:'error',message:getApiErrorMessage(requestError,'Appraisal export failed.')})} }
  const openDetail = async (row) => { try { setDetail(await pmsService.appraisalSummaryDetail(row.appraisalId)) } catch(requestError){notify({type:'error',message:getApiErrorMessage(requestError,'Appraisal detail could not be opened.')})} }
  const initiatePip = (values) => navigate('/performance/pip-management/create',{state:{pipDefaults:values}})

  const employeeColumns = [{key:'employeeName',label:'Employee',render:row=><button className="case-employee-link" onClick={(event)=>{event.stopPropagation();openDetail(row)}}><span className="case-avatar">{row.employeeName?.split(' ').map(part=>part[0]).slice(0,2).join('')}</span><span><strong>{row.employeeName}</strong><small>{row.employeeCode}</small></span></button>},{key:'department',label:'Department'},{key:'finalRating',label:'Final rating',render:row=>score(row.finalRating)}]
  const avgDepartments = hasListFilter ? filteredDepartments : report?.departments || []
  const ratingsData = hasListFilter ? filteredRatings : report?.ratingDistribution || []
  const categoryData = hasListFilter ? filteredCategories : report?.categories || []
  const topRows = hasListFilter ? [...filteredRows].filter(row=>row.finalRating!=null).sort((a,b)=>Number(b.finalRating)-Number(a.finalRating)).slice(0,10) : report?.topRated || []
  const lowRows = hasListFilter ? filteredRows.filter(row=>row.finalRating!=null&&Number(row.finalRating)<Number(threshold)).sort((a,b)=>Number(a.finalRating)-Number(b.finalRating)).slice(0,10) : report?.lowRated || []
  const completedRows = filteredRows.filter(row=>row.finalRating!=null)
  const performanceRows = [...completedRows].sort((a,b)=>Number(b.finalRating)-Number(a.finalRating))
  const selectedCycle = report?.cycles?.find(cycle=>String(cycle.cycleId)===String(cycleId))

  return <PageLayout eyebrow="PERFORMANCE MANAGEMENT" title="Appraisal Summary" description="Consolidated results from self appraisal, manager review, HR calibration, and employee goals." className="pms-page pms-workspace-page case-workspace-page">
    <div className="pms-workspace-toolbar case-toolbar"><label className="pms-search"><Search size={14}/><input value={search} onChange={event=>setSearch(event.target.value)} placeholder="Search employee, ID, department, manager"/></label><label className="pms-workspace-dept-filter"><CalendarDays size={14}/><select aria-label="Appraisal cycle" value={cycleId} onChange={event=>setCycleId(event.target.value)}><option value="">All cycles</option>{(report?.cycles||[]).map(cycle=><option key={cycle.cycleId} value={cycle.cycleId}>{cycle.cycleName}</option>)}</select></label><label className="pms-workspace-dept-filter"><UsersRound size={14}/><select aria-label="Department" value={departmentId} onChange={event=>setDepartmentId(event.target.value)}><option value="">All departments</option>{departments.map(department=><option key={department.departmentId} value={department.departmentId}>{department.departmentName}</option>)}</select></label><div className="pms-filter-wrap"><button className={`pms-button pms-button-secondary ${openPopover?'has-filter':''}`} onClick={()=>setOpenPopover(value=>!value)}><CircleAlert size={14}/>Filters</button>{openPopover&&<div className="pms-filter-popover"><label>Completion<select value={status} onChange={event=>setStatus(event.target.value)}><option value="">All statuses</option><option value="COMPLETED">Completed</option><option value="IN_PROGRESS">In progress</option><option value="YET_TO_START">Yet to start</option></select></label><label>Rating<select value={rating} onChange={event=>setRating(event.target.value)}><option value="">All ratings</option>{[1,2,3,4,5].map(value=><option key={value} value={value}>{value} · {ratings[value-1]}</option>)}</select></label><label>Low-rating PIP threshold<input type="number" min="1" max="5" step="0.1" value={threshold} onChange={event=>setThreshold(event.target.value)}/></label><button className="pms-text-button" onClick={()=>{setStatus('');setRating('');setOpenPopover(false)}}>Clear filters</button></div>}</div><button className="pms-button pms-button-secondary" onClick={exportReport}><Download size={14}/>Export report</button></div>
    <nav className="pms-tabs pms-workspace-tabs" aria-label="Appraisal summary views">{tabs.map(([id,label])=><button key={id} className={activeTab===id?'is-active':''} onClick={()=>goTab(id)}>{label}</button>)}</nav>
    {loading?<LoadingState label="Loading appraisal outcomes"/>:error?<ErrorState message={error} onRetry={refresh}/>:<>
      <section className="pms-metric-grid pms-workspace-metrics case-metrics">{stats.map(card=><StatCard key={card.label} {...card}/>)}</section>
      {activeTab==='overview'&&<>
        <div className="pms-content-grid pms-grid-three case-top-grid">
          <Panel title="Appraisal status"><div className="pms-donut-layout"><div className="pms-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={[{name:'Completed',value:summary.completed||0},{name:'In Progress',value:summary.inProgress||0},{name:'Yet to Start',value:summary.yetToStart||0}]} dataKey="value" nameKey="name" innerRadius="63%" outerRadius="88%" paddingAngle={2} stroke="none">{[0,1,2].map(index=><Cell key={index} fill={['#16866d','#e39a37','#bc514d'][index]}/>)}</Pie><Tooltip content={<ChartTooltip/>}/></PieChart></ResponsiveContainer><div className="pms-donut-center"><strong>{number(summary.totalEmployees)}</strong><span>Total employees</span></div></div><div className="pms-legend">{[['Completed',summary.completed,'#16866d'],['In Progress',summary.inProgress,'#e39a37'],['Yet to Start',summary.yetToStart,'#bc514d']].map(([name,value,color])=><div className="pms-legend-row" key={name}><span><i style={{background:color}}/>{name}</span><strong>{number(value)}</strong></div>)}</div></div></Panel>
          <Panel title="Final rating distribution"><div className="pms-chart-area"><ResponsiveContainer width="100%" height="100%"><BarChart data={ratingsData} margin={{top:12,right:8,left:-15,bottom:0}}><CartesianGrid vertical={false} stroke="#edf0f3"/><XAxis dataKey="rating" tickLine={false} axisLine={false}/><YAxis allowDecimals={false} tickLine={false} axisLine={false}/><Tooltip content={<ChartTooltip/>}/><Bar dataKey="count" name="Employees" radius={[3,3,0,0]}>{ratingsData.map((row,index)=><Cell key={row.rating} fill={colors[index]}/>)}</Bar></BarChart></ResponsiveContainer></div></Panel>
          <Panel title="Average rating by department"><div className="pms-chart-area case-dept-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={avgDepartments} layout="vertical" margin={{left:0,right:14,top:4,bottom:0}}><CartesianGrid horizontal={false} stroke="#edf0f3"/><XAxis type="number" domain={[0,5]} tick={{fontSize:8}}/><YAxis type="category" dataKey="department" width={100} tick={{fontSize:8}}/><Tooltip content={<ChartTooltip/>}/><Bar dataKey="averageRating" name="Average rating" fill="#347bd0" radius={[0,3,3,0]} barSize={11}/></BarChart></ResponsiveContainer></div></Panel>
        </div>
        <div className="pms-content-grid pms-grid-three case-mid-grid">
          <Panel title="Department appraisal summary" className="case-wide-panel"><div className="pms-table-scroll"><table className="pms-table case-table"><thead><tr><th>Department</th><th>Total</th><th>Completed</th><th>In progress</th><th>Yet to start</th><th>Completion</th><th>Avg. rating</th></tr></thead><tbody>{avgDepartments.map(row=><tr key={row.department}><td>{row.department}</td><td>{number(row.totalEmployees)}</td><td>{number(row.completed)}</td><td>{number(row.inProgress)}</td><td>{number(row.yetToStart)}</td><td>{Number(row.completionRate||0).toFixed(1)}%</td><td>{Number(row.averageRating||0).toFixed(2)}</td></tr>)}</tbody></table></div></Panel>
          <Panel title="Top rated employees" action={<button className="pms-text-button" onClick={()=>goTab('performance-by-rating')}>View all</button>}><Table columns={employeeColumns} rows={topRows} onRow={openDetail}/></Panel>
          <Panel title="Low rated employees" action={<button className="pms-text-button" onClick={()=>{setRating('');goTab('performance-by-rating')}}>View all</button>}><Table columns={employeeColumns} rows={lowRows} onRow={openDetail}/></Panel>
        </div>
        <div className="pms-content-grid pms-grid-three case-bottom-grid">
          <Panel title="Average rating trend"><div className="pms-chart-area"><ResponsiveContainer width="100%" height="100%"><LineChart data={report?.trend||[]} margin={{left:-15,right:8}}><CartesianGrid vertical={false} stroke="#edf0f3"/><XAxis dataKey="label" tick={{fontSize:8}}/><YAxis domain={[1,5]} tick={{fontSize:8}}/><Tooltip content={<ChartTooltip/>}/><Line type="monotone" dataKey="averageRating" name="Average rating" stroke="#347bd0" strokeWidth={2} dot={{r:3}}/></LineChart></ResponsiveContainer></div></Panel>
          <Panel title="Rating category summary"><div className="pms-donut-layout"><div className="pms-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={categoryData} dataKey="count" nameKey="name" innerRadius="58%" outerRadius="86%" paddingAngle={1} stroke="none">{categoryData.map((row,index)=><Cell key={row.name} fill={colors[4-index]}/>)}</Pie><Tooltip content={<ChartTooltip/>}/></PieChart></ResponsiveContainer><div className="pms-donut-center"><strong>{number(summary.completed)}</strong><span>Rated</span></div></div><div className="pms-legend">{categoryData.map(row=><div className="pms-legend-row" key={row.name}><span>{row.name}</span><strong>{number(row.count)} <small>({Number(row.percent||0).toFixed(1)}%)</small></strong></div>)}</div></div></Panel>
          <Panel title="Key insights"><div className="case-insights">{(report?.insights||[]).map((item,index)=><p key={`${item.label}-${index}`}><Lightbulb size={14}/><span>{item.label}</span><strong>{item.value}</strong></p>)}</div></Panel>
        </div>
      </>}
      {activeTab==='department-summary'&&<Panel title="Department appraisal summary"><Table columns={[{key:'department',label:'Department'},{key:'totalEmployees',label:'Total employees',render:r=>number(r.totalEmployees)},{key:'completed',label:'Completed',render:r=>number(r.completed)},{key:'inProgress',label:'In progress',render:r=>number(r.inProgress)},{key:'yetToStart',label:'Yet to start',render:r=>number(r.yetToStart)},{key:'completionRate',label:'Completion rate',render:r=>`${Number(r.completionRate||0).toFixed(1)}%`},{key:'averageRating',label:'Average rating',render:r=>score(r.averageRating)}]} rows={avgDepartments}/></Panel>}
      {activeTab==='rating-distribution'&&<div className="pms-content-grid pms-grid-two"><Panel title="Final rating distribution"><div className="pms-chart-area case-tall-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={ratingsData}><CartesianGrid vertical={false} stroke="#edf0f3"/><XAxis dataKey="rating"/><YAxis allowDecimals={false}/><Tooltip content={<ChartTooltip/>}/><Bar dataKey="count" name="Employees">{ratingsData.map((row,index)=><Cell key={row.rating} fill={colors[index]}/>)}</Bar></BarChart></ResponsiveContainer></div></Panel><Panel title="Rating categories"><div className="pms-donut-layout"><div className="pms-donut-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={categoryData} dataKey="count" nameKey="name" innerRadius="58%" outerRadius="86%">{categoryData.map((row,index)=><Cell key={row.name} fill={colors[4-index]}/>)}</Pie><Tooltip content={<ChartTooltip/>}/></PieChart></ResponsiveContainer></div><div className="pms-legend">{categoryData.map(row=><div className="pms-legend-row" key={row.name}><span>{row.name} ({row.minimum}-{row.maximum})</span><strong>{number(row.count)} · {Number(row.percent||0).toFixed(1)}%</strong></div>)}</div></div></Panel></div>}
      {activeTab==='performance-by-rating'&&<Panel title="Completed appraisals by final rating"><Table columns={employeeColumns} rows={performanceRows} onRow={openDetail}/></Panel>}
      {activeTab==='reviewer-summary'&&<Panel title="HR reviewer summary"><Table columns={[{key:'reviewer',label:'Reviewer'},{key:'assigned',label:'Assigned',render:r=>number(r.assigned)},{key:'completed',label:'Completed',render:r=>number(r.completed)},{key:'pending',label:'Pending',render:r=>number(r.pending)}]} rows={report?.reviewers||[]}/></Panel>}
      {activeTab==='appraisal-timeline'&&<Panel title={`${selectedCycle?.cycleName||'Appraisal cycle'} · Timeline`}><div className="case-timeline">{(report?.timeline||[]).map((stage,index)=><div key={stage.stageCode}><i className={index===0?'active':''}/><strong>{stage.stageCode.replaceAll('_',' ')}</strong><span>{displayDate(stage.startDate)} – {displayDate(stage.endDate)}</span></div>)}{!report?.timeline?.length&&<EmptyState title="No appraisal timeline configured"/>}</div></Panel>}
      <footer className="pms-footer">Performance Management <span>·</span> Appraisal Summary</footer>
    </>}
    {detail&&<EmployeeSummary detail={detail} onClose={()=>setDetail(null)} onInitiatePip={initiatePip}/>}
  </PageLayout>
}