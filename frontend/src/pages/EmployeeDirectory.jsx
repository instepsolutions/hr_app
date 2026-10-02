import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import {
  ArrowDown, ArrowUp, ArrowUpDown, BriefcaseBusiness, CalendarDays, Check, ChevronDown,
  CircleUserRound, Ellipsis, Eye, FilterX, Mail, MapPin, Pencil, Plus, Search, ShieldAlert,
  Trash2, UserRoundCheck, UsersRound, X,
} from 'lucide-react'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip as ChartTooltip } from 'recharts'
import { getApiErrorMessage } from '../api/client'
import { departmentService } from '../services/departmentService'
import { designationService } from '../services/designationService'
import { employeeService } from '../services/employeeService'
import { employmentTypeService } from '../services/employmentTypeService'
import { locationService } from '../services/locationService'
import {
  DataTable, DateFilter, EmptyState, ErrorState, LoadingState, Modal, PageLayout, Pagination,
  SelectFilter, StatCard,
} from '../components/ui'

const emptyForm = {
  employeeCode: '', firstName: '', middleName: '', lastName: '', gender: '', dateOfBirth: '',
  personalEmail: '', officialEmail: '', mobileNumber: '', alternateMobile: '', departmentId: '',
  designationId: '', locationId: '', employmentTypeId: '', reportingManagerId: '',
  dateOfJoining: '', confirmationDate: '', dateOfExit: '', employeeStatus: 'ACTIVE', profilePhotoUrl: '',
}

const statusColors = {
  ACTIVE: '#16846b', ON_LEAVE: '#c48220', PROBATION: '#3973c6', NOTICE_PERIOD: '#a05d99',
  RESIGNED: '#796e62', EXITED: '#a45150', INACTIVE: '#7b8798',
}

function dateLabel(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(`${value}T00:00:00`))
}

function StatusBadge({ status }) {
  const label = (status || 'UNKNOWN').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
  return <span className={`status-badge status-${(status || 'unknown').toLowerCase()}`}><i />{label}</span>
}

function EmployeeForm({ employee, lookups, onClose, onSubmit, saving }) {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({ defaultValues: emptyForm })

  useEffect(() => {
    reset(employee ? {
      ...emptyForm,
      ...employee,
      dateOfBirth: employee.dateOfBirth || '',
      dateOfJoining: employee.dateOfJoining || '',
      confirmationDate: employee.confirmationDate || '',
      dateOfExit: employee.dateOfExit || '',
      departmentId: employee.departmentId?.toString() || '',
      designationId: employee.designationId?.toString() || '',
      locationId: employee.locationId?.toString() || '',
      employmentTypeId: employee.employmentTypeId?.toString() || '',
      reportingManagerId: employee.reportingManagerId?.toString() || '',
    } : emptyForm)
  }, [employee, reset])

  return (
    <form className="employee-form" onSubmit={handleSubmit(onSubmit)}>
      <div className="form-section-label">IDENTITY</div>
      <div className="form-grid">
        <label className="form-field"><span>Employee ID <b>*</b></span><input placeholder="e.g. NS-1042" {...register('employeeCode', { required: 'Employee ID is required' })} />{errors.employeeCode && <small>{errors.employeeCode.message}</small>}</label>
        <label className="form-field"><span>First name <b>*</b></span><input placeholder="First name" {...register('firstName', { required: 'First name is required' })} />{errors.firstName && <small>{errors.firstName.message}</small>}</label>
        <label className="form-field"><span>Middle name</span><input placeholder="Middle name" {...register('middleName')} /></label>
        <label className="form-field"><span>Last name <b>*</b></span><input placeholder="Last name" {...register('lastName', { required: 'Last name is required' })} />{errors.lastName && <small>{errors.lastName.message}</small>}</label>
        <label className="form-field"><span>Gender</span><select {...register('gender')}><option value="">Select gender</option><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other</option><option value="PREFER_NOT_TO_SAY">Prefer not to say</option></select></label>
        <label className="form-field"><span>Date of birth</span><input type="date" {...register('dateOfBirth')} /></label>
        <label className="form-field"><span>Official email <b>*</b></span><input type="email" placeholder="name@company.com" {...register('officialEmail', { required: 'Official email is required', pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email address' } })} />{errors.officialEmail && <small>{errors.officialEmail.message}</small>}</label>
        <label className="form-field"><span>Personal email</span><input type="email" placeholder="name@email.com" {...register('personalEmail')} /></label>
        <label className="form-field"><span>Mobile number</span><input type="tel" placeholder="+91 98765 43210" {...register('mobileNumber')} /></label>
        <label className="form-field"><span>Alternate mobile</span><input type="tel" placeholder="Optional" {...register('alternateMobile')} /></label>
      </div>
      <div className="form-section-label form-section-spaced">WORK DETAILS</div>
      <div className="form-grid">
        <label className="form-field"><span>Department <b>*</b></span><select {...register('departmentId', { required: 'Choose a department' })}><option value="">Select department</option>{lookups.departments.map((item) => <option key={item.departmentId} value={item.departmentId}>{item.departmentName}</option>)}</select>{errors.departmentId && <small>{errors.departmentId.message}</small>}</label>
        <label className="form-field"><span>Designation <b>*</b></span><select {...register('designationId', { required: 'Choose a designation' })}><option value="">Select designation</option>{lookups.designations.map((item) => <option key={item.designationId} value={item.designationId}>{item.designationName}</option>)}</select>{errors.designationId && <small>{errors.designationId.message}</small>}</label>
        <label className="form-field"><span>Location <b>*</b></span><select {...register('locationId', { required: 'Choose a location' })}><option value="">Select location</option>{lookups.locations.map((item) => <option key={item.locationId} value={item.locationId}>{item.locationName}</option>)}</select>{errors.locationId && <small>{errors.locationId.message}</small>}</label>
        <label className="form-field"><span>Employment type <b>*</b></span><select {...register('employmentTypeId', { required: 'Choose an employment type' })}><option value="">Select employment type</option>{lookups.employmentTypes.map((item) => <option key={item.employmentTypeId} value={item.employmentTypeId}>{item.employmentTypeName}</option>)}</select>{errors.employmentTypeId && <small>{errors.employmentTypeId.message}</small>}</label>
        <label className="form-field"><span>Reporting manager ID</span><input type="number" min="1" placeholder="Employee ID" {...register('reportingManagerId')} /></label>
        <label className="form-field"><span>Status</span><select {...register('employeeStatus')}><option value="ACTIVE">Active</option><option value="ON_LEAVE">On leave</option><option value="PROBATION">Probation</option><option value="NOTICE_PERIOD">Notice period</option><option value="RESIGNED">Resigned</option><option value="EXITED">Exited</option><option value="INACTIVE">Inactive</option></select></label>
        <label className="form-field"><span>Date of joining</span><input type="date" {...register('dateOfJoining')} /></label>
        <label className="form-field"><span>Confirmation date</span><input type="date" {...register('confirmationDate')} /></label>
      </div>
      <footer className="modal-actions"><button type="button" className="button button-secondary" onClick={onClose}>Cancel</button><button type="submit" className="button button-primary" disabled={saving}>{saving ? 'Saving…' : employee ? 'Save changes' : 'Add employee'}</button></footer>
    </form>
  )
}

function RowActions({ employee, onView, onEdit, onDeactivate, onReactivate, onDelete }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="row-actions">
      <button className="icon-button row-action-trigger" aria-label={`Actions for ${employee.displayName}`} aria-expanded={open} onClick={() => setOpen((value) => !value)}><Ellipsis size={19} /></button>
      {open && <><button className="menu-dismiss" aria-label="Close actions" onClick={() => setOpen(false)} /><div className="row-action-menu">
        <button onClick={() => { setOpen(false); onView() }}><Eye size={15} />View profile</button>
        <button onClick={() => { setOpen(false); onEdit() }}><Pencil size={15} />Edit employee</button>
        {employee.employeeStatus === 'INACTIVE' ? <button onClick={() => { setOpen(false); onReactivate() }}><Check size={15} />Reactivate</button> : <button onClick={() => { setOpen(false); onDeactivate() }}><ShieldAlert size={15} />Deactivate</button>}
        <span className="action-menu-divider" />
        <button className="menu-danger" onClick={() => { setOpen(false); onDelete() }}><Trash2 size={15} />Archive employee</button>
      </div></>}
    </div>
  )
}

export default function EmployeeDirectory({ notify }) {
  const navigate = useNavigate()
  const [employees, setEmployees] = useState([])
  const [stats, setStats] = useState(null)
  const [lookups, setLookups] = useState({ departments: [], locations: [], designations: [], employmentTypes: [] })
  const [lookupError, setLookupError] = useState('')
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [statsLoading, setStatsLoading] = useState(true)
  const [filters, setFilters] = useState({ search: '', departmentId: '', locationId: '', employmentTypeId: '', status: '', dateOfJoiningFrom: '', dateOfJoiningTo: '' })
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [sort, setSort] = useState({ field: 'employeeCode', direction: 'asc' })
  const [pageData, setPageData] = useState({ totalElements: 0, totalPages: 0 })
  const [modal, setModal] = useState(null)
  const [saving, setSaving] = useState(false)
  const [refresh, setRefresh] = useState(0)
  const [modalError, setModalError] = useState('')

  useEffect(() => {
    const timer = window.setTimeout(() => setFilters((current) => current.search === searchInput.trim() ? current : { ...current, search: searchInput.trim() }), 320)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([
      departmentService.list(controller.signal),
      locationService.list(controller.signal),
      designationService.list(controller.signal),
      employmentTypeService.list(controller.signal),
    ]).then(([departments, locations, designations, employmentTypes]) => {
      setLookups({ departments, locations, designations, employmentTypes })
      setLookupError('')
    }).catch((error) => {
      if (!controller.signal.aborted) setLookupError(getApiErrorMessage(error, 'Lookup options could not be loaded.'))
    })
    return () => controller.abort()
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    setStatsLoading(true)
    employeeService.statistics(controller.signal)
      .then((data) => { setStats(data); setStatsLoading(false) })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setStatsLoading(false)
          notify({ type: 'error', message: getApiErrorMessage(error, 'Employee statistics are unavailable.') })
        }
      })
    return () => controller.abort()
  }, [refresh, notify])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setLoadError('')
    const params = Object.fromEntries(Object.entries({
      ...filters,
      page,
      size,
      sortBy: sort.field,
      sortDirection: sort.direction,
    }).filter(([, value]) => value !== '' && value !== null && value !== undefined))
    employeeService.list(params, controller.signal)
      .then((data) => {
        setEmployees(data.content || [])
        setPageData({ totalElements: data.totalElements || 0, totalPages: data.totalPages || 0 })
        setLoading(false)
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setLoadError(getApiErrorMessage(error, 'Employee directory is unavailable.'))
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [filters, page, size, sort, refresh])

  function updateFilter(field, value) {
    setPage(0)
    setFilters((current) => ({ ...current, [field]: value }))
  }

  function clearFilters() {
    setSearchInput('')
    setFilters({ search: '', departmentId: '', locationId: '', employmentTypeId: '', status: '', dateOfJoiningFrom: '', dateOfJoiningTo: '' })
    setPage(0)
  }

  function lookupName(list, id, idKey, nameKey) {
    return list.find((item) => String(item[idKey]) === String(id))?.[nameKey] || '—'
  }

  function toggleSort(field) {
    setSort((current) => ({ field, direction: current.field === field && current.direction === 'asc' ? 'desc' : 'asc' }))
  }

  async function submitEmployee(values) {
    setSaving(true)
    setModalError('')
    const payload = {
      ...values,
      departmentId: Number(values.departmentId),
      designationId: Number(values.designationId),
      locationId: Number(values.locationId),
      employmentTypeId: Number(values.employmentTypeId),
      reportingManagerId: values.reportingManagerId ? Number(values.reportingManagerId) : null,
      dateOfBirth: values.dateOfBirth || null,
      dateOfJoining: values.dateOfJoining || null,
      confirmationDate: values.confirmationDate || null,
      dateOfExit: values.dateOfExit || null,
      mobileNumber: values.mobileNumber || null,
      alternateMobile: values.alternateMobile || null,
      personalEmail: values.personalEmail || null,
      profilePhotoUrl: values.profilePhotoUrl || null,
    }
    try {
      if (modal.employee) await employeeService.update(modal.employee.employeeId, payload)
      else await employeeService.create(payload)
      setModal(null)
      setRefresh((value) => value + 1)
      notify({ type: 'success', message: modal.employee ? 'Employee details updated.' : 'Employee added to the directory.' })
    } catch (error) {
      setModalError(getApiErrorMessage(error, 'The employee could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  async function changeStatus(employee, status) {
    setSaving(true)
    try {
      await employeeService.updateStatus(employee.employeeId, { status, reason: status === 'INACTIVE' ? 'DEACTIVATED' : 'REACTIVATED', remarks: `Status changed from directory` })
      setModal(null)
      setRefresh((value) => value + 1)
      notify({ type: 'success', message: status === 'INACTIVE' ? 'Employee deactivated.' : 'Employee reactivated.' })
    } catch (error) {
      setModalError(getApiErrorMessage(error, 'Employee status could not be changed.'))
    } finally {
      setSaving(false)
    }
  }

  async function deleteEmployee(employee) {
    setSaving(true)
    try {
      await employeeService.remove(employee.employeeId)
      setModal(null)
      setRefresh((value) => value + 1)
      notify({ type: 'success', message: 'Employee archived and marked inactive.' })
    } catch (error) {
      setModalError(getApiErrorMessage(error, 'Employee could not be deleted.'))
    } finally {
      setSaving(false)
    }
  }

  const activeFilters = Object.entries(filters).some(([key, value]) => key !== 'search' && value !== '') || Boolean(filters.search)
  const chartData = stats ? [
    { name: 'Active', value: stats.activeEmployees || 0, color: statusColors.ACTIVE },
    { name: 'On leave', value: stats.onLeaveToday || 0, color: statusColors.ON_LEAVE },
    { name: 'Probation', value: stats.probationEmployees || 0, color: statusColors.PROBATION },
    { name: 'Exited', value: stats.exitedEmployees || 0, color: statusColors.EXITED },
  ].filter((item) => item.value > 0) : []

  return (
    <PageLayout
      eyebrow="PEOPLE OPERATIONS / DIRECTORY"
      title="Employee directory"
      description="A clear view of the people who make Northstar work."
      action={<button className="button button-primary" onClick={() => { setModalError(''); setModal({ type: 'form', employee: null }) }}><Plus size={17} />Add employee</button>}
    >
      <section className="directory-overview" aria-label="Employee overview">
        <div className="stat-grid">
          <StatCard label="Total employees" value={statsLoading ? '…' : stats?.totalEmployees} note="Across all locations" icon={UsersRound} accent="blue" />
          <StatCard label="Active" value={statsLoading ? '…' : stats?.activeEmployees} note="Currently employed" icon={UserRoundCheck} accent="green" />
          <StatCard label="New this month" value={statsLoading ? '…' : stats?.newJoiners} note="Joined since month start" icon={CalendarDays} accent="amber" />
          <StatCard label="On leave today" value={statsLoading ? '…' : stats?.onLeaveToday} note="Away from work" icon={BriefcaseBusiness} accent="rose" />
        </div>
        <aside className="workforce-card" aria-label="Employee status distribution">
          <div className="workforce-heading"><span>WORKFORCE</span><strong>Status mix</strong></div>
          {statsLoading ? <div className="chart-loading">Loading</div> : chartData.length ? <div className="workforce-chart"><ResponsiveContainer width="100%" height={110}><PieChart><Pie data={chartData} dataKey="value" nameKey="name" innerRadius={30} outerRadius={44} paddingAngle={3} stroke="none">{chartData.map((entry) => <Cell key={entry.name} fill={entry.color} />)}</Pie><ChartTooltip formatter={(value, name) => [`${value} employees`, name]} /></PieChart></ResponsiveContainer><div className="workforce-legend">{chartData.slice(0, 3).map((item) => <span key={item.name}><i style={{ backgroundColor: item.color }} />{item.name}</span>)}</div></div> : <div className="chart-empty">No workforce data</div>}
        </aside>
      </section>

      <section className="directory-panel" aria-label="Employee directory list">
        <div className="directory-panel-heading">
          <div><h2>All employees</h2><span>{pageData.totalElements.toLocaleString()} records</span></div>
          <span className="directory-live"><i />LIVE DATA</span>
        </div>
        <div className="filter-toolbar">
          <label className="directory-search"><Search size={17} /><input id="employee-search" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Search name, email or employee ID" /><kbd>/</kbd></label>
          <div className="filter-grid">
            <SelectFilter label="Department" value={filters.departmentId} onChange={(value) => updateFilter('departmentId', value)} options={lookups.departments.map((item) => ({ value: item.departmentId, label: item.departmentName }))} />
            <SelectFilter label="Location" value={filters.locationId} onChange={(value) => updateFilter('locationId', value)} options={lookups.locations.map((item) => ({ value: item.locationId, label: item.locationName }))} />
            <SelectFilter label="Employment type" value={filters.employmentTypeId} onChange={(value) => updateFilter('employmentTypeId', value)} options={lookups.employmentTypes.map((item) => ({ value: item.employmentTypeId, label: item.employmentTypeName }))} />
            <SelectFilter label="Status" value={filters.status} onChange={(value) => updateFilter('status', value)} options={Object.keys(statusColors).map((value) => ({ value, label: value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) }))} />
            <DateFilter label="Joined from" value={filters.dateOfJoiningFrom} onChange={(value) => updateFilter('dateOfJoiningFrom', value)} />
            <DateFilter label="Joined to" value={filters.dateOfJoiningTo} onChange={(value) => updateFilter('dateOfJoiningTo', value)} />
          </div>
          {activeFilters && <button className="clear-filter-button" onClick={clearFilters}><FilterX size={14} />Clear filters</button>}
        </div>
        {lookupError && <div className="lookup-warning" role="alert">{lookupError}<button onClick={() => window.location.reload()}>Reload</button></div>}
        {loadError ? <ErrorState message={loadError} onRetry={() => setRefresh((value) => value + 1)} /> : loading ? <LoadingState label="Loading employee directory" /> : employees.length === 0 ? <EmptyState /> : (
          <>
            <DataTable className="employee-table" minWidth="880px" columns={[
              { key: 'employee', label: <button className="sort-button" onClick={() => toggleSort('employeeCode')}>Employee <SortIcon field="employeeCode" sort={sort} /></button> },
              { key: 'department', label: 'Department' },
              { key: 'location', label: 'Location' },
              { key: 'employment', label: 'Employment' },
              { key: 'joined', label: <button className="sort-button" onClick={() => toggleSort('dateOfJoining')}>Date joined <SortIcon field="dateOfJoining" sort={sort} /></button> },
              { key: 'status', label: 'Status' },
              { key: 'actions', label: '', className: 'actions-heading' },
            ]}>
                {employees.map((employee) => (
                  <tr key={employee.employeeId}>
                    <td><button className="employee-identity" onClick={() => navigate(`/employee-management/employees/${employee.employeeId}`)}><span className="employee-avatar">{(employee.firstName?.[0] || '?')}{employee.lastName?.[0] || ''}</span><span className="employee-name-block"><strong>{employee.displayName || `${employee.firstName} ${employee.lastName}`}</strong><small>{employee.employeeCode} <i>·</i> {employee.officialEmail}</small></span></button></td>
                    <td><span className="table-primary">{lookupName(lookups.departments, employee.departmentId, 'departmentId', 'departmentName')}</span></td>
                    <td><span className="table-location"><MapPin size={13} />{lookupName(lookups.locations, employee.locationId, 'locationId', 'locationName')}</span></td>
                    <td>{lookupName(lookups.employmentTypes, employee.employmentTypeId, 'employmentTypeId', 'employmentTypeName')}</td>
                    <td className="table-date">{dateLabel(employee.dateOfJoining)}</td>
                    <td><StatusBadge status={employee.employeeStatus} /></td>
                    <td><RowActions employee={employee}
                      onView={() => navigate(`/employee-management/employees/${employee.employeeId}`)}
                      onEdit={() => { setModalError(''); setModal({ type: 'form', employee }) }}
                      onDeactivate={() => { setModalError(''); setModal({ type: 'status', employee, status: 'INACTIVE' }) }}
                      onReactivate={() => { setModalError(''); setModal({ type: 'status', employee, status: 'ACTIVE' }) }}
                      onDelete={() => { setModalError(''); setModal({ type: 'delete', employee }) }}
                    /></td>
                  </tr>
                ))}
            </DataTable>
            <Pagination page={page} totalPages={pageData.totalPages} totalElements={pageData.totalElements} size={size} onPageChange={setPage} onSizeChange={(value) => { setSize(value); setPage(0) }} />
          </>
        )}
      </section>
      {modal?.type === 'form' && <Modal title={modal.employee ? 'Edit employee' : 'Add employee'} description={modal.employee ? `Update ${modal.employee.displayName || modal.employee.employeeCode}` : 'Enter the employee’s core information.'} onClose={() => { setModal(null); setModalError('') }} size="large">
        {modalError && <div className="modal-error" role="alert">{modalError}</div>}
        <EmployeeForm employee={modal.employee} lookups={lookups} onClose={() => setModal(null)} onSubmit={submitEmployee} saving={saving} />
      </Modal>}
      {modal?.type === 'status' && <Modal title={modal.status === 'INACTIVE' ? 'Deactivate employee?' : 'Reactivate employee?'} description={modal.employee.displayName || modal.employee.employeeCode} onClose={() => { setModal(null); setModalError('') }}>
        <div className="confirm-content"><span className="confirm-symbol"><ShieldAlert size={20} /></span><p>{modal.status === 'INACTIVE' ? 'This employee will be marked inactive. Their record and history will remain available.' : 'This employee will be marked active again.'}</p>{modalError && <div className="modal-error" role="alert">{modalError}</div>}</div>
        <footer className="modal-actions"><button className="button button-secondary" onClick={() => setModal(null)}>Cancel</button><button className="button button-primary" disabled={saving} onClick={() => changeStatus(modal.employee, modal.status)}>{saving ? 'Updating…' : modal.status === 'INACTIVE' ? 'Deactivate employee' : 'Reactivate employee'}</button></footer>
      </Modal>}
      {modal?.type === 'delete' && <Modal title="Archive employee?" description={modal.employee.displayName || modal.employee.employeeCode} onClose={() => { setModal(null); setModalError('') }}>
        <div className="confirm-content"><span className="confirm-symbol confirm-danger"><Trash2 size={19} /></span><p>The employee will be marked inactive and retained for reporting, lifecycle, and audit history. You can reactivate the employee later.</p>{modalError && <div className="modal-error" role="alert">{modalError}</div>}</div>
        <footer className="modal-actions"><button className="button button-secondary" onClick={() => setModal(null)}>Cancel</button><button className="button button-primary" disabled={saving} onClick={() => deleteEmployee(modal.employee)}>{saving ? 'Archiving…' : 'Archive employee'}</button></footer>
      </Modal>}
    </PageLayout>
  )
}

function SortIcon({ field, sort }) {
  if (sort.field !== field) return <ArrowUpDown size={13} />
  return sort.direction === 'asc' ? <ArrowUp size={13} /> : <ArrowDown size={13} />
}
