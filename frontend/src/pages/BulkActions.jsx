import { useEffect, useMemo, useState } from 'react'
import { Check, ChevronLeft, ChevronRight, CircleCheck, CircleX, ClipboardList, Clock3, Search, UsersRound } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { DataTable, EmptyState, ErrorState, LoadingState, Modal, PageLayout, Pagination } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { bulkActionService } from '../services/bulkActionService'
import { departmentService } from '../services/departmentService'
import { designationService } from '../services/designationService'
import { employeeService } from '../services/employeeService'
import { locationService } from '../services/locationService'

const actions = [
  { type: 'UPDATE_DEPARTMENT', label: 'Update department', target: 'department' },
  { type: 'UPDATE_DESIGNATION', label: 'Update designation', target: 'designation' },
  { type: 'UPDATE_LOCATION', label: 'Update work location', target: 'location' },
  { type: 'UPDATE_REPORTING_MANAGER', label: 'Update reporting manager', target: 'manager' },
  { type: 'CHANGE_STATUS', label: 'Change employee status', target: 'status' },
  { type: 'ACTIVATE_EMPLOYEES', label: 'Activate employees', target: null },
  { type: 'DEACTIVATE_EMPLOYEES', label: 'Deactivate employees', target: null },
]
const statusOptions = ['ACTIVE', 'ON_LEAVE', 'PROBATION', 'NOTICE_PERIOD', 'RESIGNED', 'EXITED', 'INACTIVE']
const stepTitles = ['Select employees', 'Choose action', 'Review & confirm']

function dateLabel(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value))
}

function resultActionName(type) {
  return actions.find((action) => action.type === type)?.label || type?.replaceAll('_', ' ')
}

export default function BulkActions({ notify }) {
  const [step, setStep] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [employees, setEmployees] = useState([])
  const [pageCount, setPageCount] = useState(0)
  const [totalEmployees, setTotalEmployees] = useState(0)
  const [selected, setSelected] = useState({})
  const [lookups, setLookups] = useState({ departments: [], designations: [], locations: [] })
  const [managerSearch, setManagerSearch] = useState('')
  const [managerOptions, setManagerOptions] = useState([])
  const [managerLoading, setManagerLoading] = useState(false)
  const [action, setAction] = useState('')
  const [result, setResult] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [history, setHistory] = useState([])
  const [historyError, setHistoryError] = useState('')
  const [refresh, setRefresh] = useState(0)
  const { register, watch, setValue, reset } = useForm({ defaultValues: { newValue: '', remarks: '' } })
  const newValue = watch('newValue')
  const selectedEmployees = Object.values(selected)
  const selectedAction = actions.find((item) => item.type === action)
  const requiresTarget = Boolean(selectedAction?.target)

  useEffect(() => {
    const timer = window.setTimeout(() => { setSearch(searchInput.trim()); setPage(0) }, 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([departmentService.list(controller.signal), designationService.list(controller.signal), locationService.list(controller.signal)])
      .then(([departments, designations, locations]) => setLookups({ departments, designations, locations }))
      .catch((error) => { if (!controller.signal.aborted) setLoadError(getApiErrorMessage(error, 'Action options could not be loaded.')) })
    return () => controller.abort()
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    setLoadError('')
    employeeService.list({ page, size: 10, search, sortBy: 'displayName', sortDirection: 'asc' }, controller.signal)
      .then((data) => {
        setEmployees(data.content || [])
        setPageCount(data.totalPages || 0)
        setTotalEmployees(data.totalElements || 0)
      })
      .catch((error) => { if (!controller.signal.aborted) setLoadError(getApiErrorMessage(error, 'Employees could not be loaded.')) })
    return () => controller.abort()
  }, [page, search, refresh])

  useEffect(() => {
    if (action !== 'UPDATE_REPORTING_MANAGER') return undefined
    const controller = new AbortController()
    const timer = window.setTimeout(() => {
      setManagerLoading(true)
      employeeService.list({ page: 0, size: 20, search: managerSearch.trim(), sortBy: 'displayName', sortDirection: 'asc' }, controller.signal)
        .then((result) => { setManagerOptions(result.content || []); setManagerLoading(false) })
        .catch((error) => {
          if (!controller.signal.aborted) {
            setManagerOptions([])
            setManagerLoading(false)
            notify({ type: 'error', message: getApiErrorMessage(error, 'Reporting managers could not be loaded.') })
          }
        })
    }, 220)
    return () => { window.clearTimeout(timer); controller.abort() }
  }, [action, managerSearch, notify])

  useEffect(() => {
    const controller = new AbortController()
    bulkActionService.history(controller.signal)
      .then((items) => { setHistory([...(items || [])].sort((first, second) => (second.requestedAt || '').localeCompare(first.requestedAt || ''))); setHistoryError('') })
      .catch((error) => { if (!controller.signal.aborted) setHistoryError(getApiErrorMessage(error, 'Bulk action history could not be loaded.')) })
    return () => controller.abort()
  }, [refresh])

  const targetOptions = useMemo(() => {
    if (selectedAction?.target === 'department') return lookups.departments.map((item) => ({ id: item.departmentId, name: item.departmentName }))
    if (selectedAction?.target === 'designation') return lookups.designations.map((item) => ({ id: item.designationId, name: item.designationName }))
    if (selectedAction?.target === 'location') return lookups.locations.map((item) => ({ id: item.locationId, name: item.locationName }))
    if (selectedAction?.target === 'manager') return managerOptions.map((item) => ({ id: item.employeeId, name: `${item.displayName} · ${item.employeeCode}` }))
    if (selectedAction?.target === 'status') return statusOptions.map((value) => ({ id: value, name: value.replaceAll('_', ' ') }))
    return []
  }, [lookups, managerOptions, selectedAction])

  function toggleEmployee(employee) {
    setSelected((current) => {
      const next = { ...current }
      if (next[employee.employeeId]) delete next[employee.employeeId]
      else next[employee.employeeId] = employee
      return next
    })
    setResult(null)
  }

  function toggleVisibleEmployees() {
    const allSelected = employees.length > 0 && employees.every((employee) => selected[employee.employeeId])
    setSelected((current) => {
      const next = { ...current }
      employees.forEach((employee) => {
        if (allSelected) delete next[employee.employeeId]
        else next[employee.employeeId] = employee
      })
      return next
    })
  }

  function chooseAction(type) {
    setAction(type)
    if (type === 'UPDATE_REPORTING_MANAGER') setManagerSearch('')
    reset({ newValue: '', remarks: '' })
    setResult(null)
  }

  async function submitAction() {
    setProcessing(true)
    try {
      const response = await bulkActionService.create({
        actionType: action,
        employeeIds: selectedEmployees.map((employee) => employee.employeeId),
        newValue: requiresTarget ? newValue : null,
        remarks: watch('remarks') || null,
      })
      setResult(response)
      setConfirmOpen(false)
      setRefresh((value) => value + 1)
      notify({ type: response.failedCount > 0 ? 'error' : 'success', message: `${response.successfulCount} employees updated; ${response.failedCount} failed.` })
    } catch (error) {
      setConfirmOpen(false)
      notify({ type: 'error', message: getApiErrorMessage(error, 'Bulk action could not be processed.') })
    } finally {
      setProcessing(false)
    }
  }

  function resetWorkflow() {
    setSelected({})
    setAction('')
    setResult(null)
    setStep(0)
    reset({ newValue: '', remarks: '' })
  }

  return <PageLayout eyebrow="PEOPLE OPERATIONS / DIRECTORY" title="Bulk actions" description="Apply a controlled employee update and keep the outcome in an auditable history." action={<span className="bulk-selected-summary"><UsersRound size={16} />{selectedEmployees.length} selected</span>}>
    <section className="bulk-workflow-panel">
      <div className="bulk-steps">{stepTitles.map((title, index) => <button className={`bulk-step ${step === index ? 'is-current' : ''} ${step > index ? 'is-complete' : ''}`} key={title} onClick={() => { if (index < step && !processing) setStep(index) }} disabled={index > step || processing}><span className="bulk-step-number">{step > index ? <Check size={15} /> : index + 1}</span><span>{title}</span></button>)}</div>
      {step === 0 && <div className="bulk-step-content">
        <div className="bulk-content-heading"><div><h2>Choose employees</h2><p>Select the people this action should affect.</p></div><label className="directory-search bulk-search"><Search size={16} /><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Search employees" /></label></div>
        {loadError ? <ErrorState message={loadError} onRetry={() => setRefresh((value) => value + 1)} /> : employees.length === 0 ? <EmptyState title="No employees available" /> : <>
          <DataTable className="employee-table bulk-employee-table" minWidth="720px" columns={[
            { key: 'select', label: <label className="checkbox-label"><input type="checkbox" checked={employees.length > 0 && employees.every((employee) => Boolean(selected[employee.employeeId]))} onChange={toggleVisibleEmployees} aria-label="Select all employees on this page" /><span>Select page</span></label> },
            { key: 'employee', label: 'Employee' }, { key: 'department', label: 'Department ID' }, { key: 'status', label: 'Status' },
          ]}>{employees.map((employee) => <tr key={employee.employeeId}>
            <td><input className="employee-checkbox" type="checkbox" checked={Boolean(selected[employee.employeeId])} onChange={() => toggleEmployee(employee)} aria-label={`Select ${employee.displayName}`} /></td>
            <td><span className="employee-name-block"><strong>{employee.displayName}</strong><small>{employee.employeeCode} · {employee.officialEmail}</small></span></td>
            <td>{employee.departmentId}</td><td><span className={`status-badge status-${employee.employeeStatus?.toLowerCase()}`}><i />{employee.employeeStatus?.replaceAll('_', ' ')}</span></td>
          </tr>)}</DataTable>
          <Pagination page={page} totalPages={pageCount} totalElements={totalEmployees} size={10} onPageChange={setPage} onSizeChange={() => {}} />
        </>}
        <div className="bulk-step-footer"><span>{selectedEmployees.length} employee{selectedEmployees.length === 1 ? '' : 's'} selected across pages</span><button className="button button-primary" disabled={!selectedEmployees.length} onClick={() => setStep(1)}>Choose action <ChevronRight size={16} /></button></div>
      </div>}
      {step === 1 && <div className="bulk-step-content">
        <div className="bulk-content-heading"><div><h2>Choose an action</h2><p>The update will apply to {selectedEmployees.length} selected employees.</p></div><button className="button button-secondary button-small" onClick={() => setStep(0)}><ChevronLeft size={14} />Change selection</button></div>
        <div className="bulk-action-options">{actions.map((item) => <label className={`bulk-action-option ${action === item.type ? 'is-selected' : ''}`} key={item.type}><input type="radio" name="bulk-action" value={item.type} checked={action === item.type} onChange={() => chooseAction(item.type)} /><span className="radio-visual" /><span><strong>{item.label}</strong><small>{item.target ? `Choose a ${item.target === 'manager' ? 'reporting manager' : item.target} for the selected employees.` : item.type === 'ACTIVATE_EMPLOYEES' ? 'Set employee status to Active.' : item.type === 'DEACTIVATE_EMPLOYEES' ? 'Set employee status to Inactive.' : 'Apply the selected employee status.'}</small></span></label>)}</div>
        {selectedAction?.target && <label className="form-field bulk-target-field"><span>{selectedAction.target === 'status' ? 'New status' : selectedAction.target === 'manager' ? 'Manager employee' : `New ${selectedAction.target}`} <b>*</b></span>
          {selectedAction.target === 'manager' ? <><input value={managerSearch} onChange={(event) => setManagerSearch(event.target.value)} placeholder="Search the employee directory" /><select {...register('newValue', { required: true })}><option value="">{managerLoading ? 'Loading managers…' : 'Choose a manager'}</option>{targetOptions.map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}</select><small>Results are searched from the live employee directory.</small></> : <select {...register('newValue', { required: true })}><option value="">Choose a value</option>{targetOptions.map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}</select>}
        </label>}
        <label className="form-field bulk-remarks-field"><span>Reason / remarks</span><textarea rows="2" placeholder="Optional note for the action history" {...register('remarks')} /></label>
        <div className="bulk-step-footer"><span>Action: <strong>{selectedAction?.label || 'Not selected'}</strong></span><button className="button button-primary" disabled={!action || (requiresTarget && !newValue)} onClick={() => setStep(2)}>Review action <ChevronRight size={16} /></button></div>
      </div>}
      {step === 2 && <div className="bulk-step-content">
        <div className="bulk-content-heading"><div><h2>Review & confirm</h2><p>Check the action and employee selection before processing.</p></div><button className="button button-secondary button-small" onClick={() => setStep(1)}><ChevronLeft size={14} />Edit action</button></div>
        <div className="bulk-review-grid"><div><span>SELECTED EMPLOYEES</span><strong>{selectedEmployees.length}</strong></div><div><span>ACTION</span><strong>{selectedAction?.label}</strong></div><div><span>NEW VALUE</span><strong>{targetOptions.find((option) => String(option.id) === String(newValue))?.name || (selectedAction?.target === 'manager' ? `Employee #${newValue}` : '—')}</strong></div></div>
        <div className="bulk-review-list">{selectedEmployees.slice(0, 8).map((employee) => <span key={employee.employeeId}>{employee.displayName} <small>{employee.employeeCode}</small></span>)}{selectedEmployees.length > 8 && <span className="bulk-review-more">+{selectedEmployees.length - 8} more employees</span>}</div>
        <label className="form-field bulk-remarks-field"><span>Reason / remarks</span><textarea rows="2" readOnly value={watch('remarks') || 'No remarks provided'} /></label>
        {result && <div className={`bulk-result ${result.failedCount > 0 ? 'has-failures' : ''}`}><div><CircleCheck size={20} /><span><small>SUCCESSFUL</small><strong>{result.successfulCount}</strong></span></div><div><CircleX size={20} /><span><small>FAILED</small><strong>{result.failedCount}</strong></span></div><div><ClipboardList size={20} /><span><small>BULK ACTION ID</small><strong>#{result.bulkActionId}</strong></span></div><button className="button button-secondary button-small" onClick={resetWorkflow}>New action</button></div>}
        {!result && <div className="bulk-step-footer"><span>Processing will write a history record for this action.</span><button className="button button-primary" disabled={processing} onClick={() => setConfirmOpen(true)}>{processing ? 'Processing…' : 'Confirm and process'}<Check size={15} /></button></div>}
      </div>}
    </section>

    <section className="directory-panel bulk-history-panel"><div className="directory-panel-heading"><div><h2>Recent bulk actions</h2><span>Persisted processing history</span></div><span className="directory-live"><i />LIVE HISTORY</span></div>
      {historyError ? <div className="lookup-warning" role="alert">{historyError}</div> : history.length === 0 ? <div className="bulk-history-empty"><Clock3 size={18} />No bulk actions have been recorded.</div> : <DataTable className="employee-table bulk-history-table" minWidth="700px" columns={[{ key: 'id', label: 'Action ID' }, { key: 'action', label: 'Action' }, { key: 'requested', label: 'Requested' }, { key: 'count', label: 'Employees' }, { key: 'status', label: 'Status' }, { key: 'result', label: 'Result' }]}>{history.slice(0, 12).map((item) => <tr key={item.bulkActionId}><td>#{item.bulkActionId}</td><td>{resultActionName(item.actionType)}</td><td>{dateLabel(item.requestedAt)}</td><td>{item.totalEmployees}</td><td><span className={`status-badge status-${item.status?.toLowerCase()}`}><i />{item.status}</span></td><td>{item.successfulCount || 0} succeeded · {item.failedCount || 0} failed</td></tr>)}</DataTable>}
    </section>
    {confirmOpen && <Modal title="Confirm bulk action" description={`${selectedAction?.label} · ${selectedEmployees.length} employees`} onClose={() => { if (!processing) setConfirmOpen(false) }}>
      <div className="confirm-content"><span className="confirm-symbol"><UsersRound size={19} /></span><p>This action will update {selectedEmployees.length} employee records. Review the selection and chosen value before continuing.</p></div>
      <footer className="modal-actions"><button className="button button-secondary" disabled={processing} onClick={() => setConfirmOpen(false)}>Go back</button><button className="button button-primary" disabled={processing} onClick={submitAction}>{processing ? 'Processing…' : 'Confirm and process'}</button></footer>
    </Modal>}
  </PageLayout>
}
