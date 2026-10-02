import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { ContactRound, HeartPulse, Pencil, Search, ShieldCheck, UserRound } from 'lucide-react'
import { DataTable, EmptyState, ErrorState, LoadingState, Modal, PageLayout, Pagination } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { departmentService } from '../services/departmentService'
import { employeeService } from '../services/employeeService'
import { employeeProfileService } from '../services/employeeProfileService'

const profileFields = [
  ['bloodGroup', 'Blood group'], ['maritalStatus', 'Marital status'], ['nationality', 'Nationality'],
  ['emergencyContactName', 'Emergency contact name'], ['emergencyContactNumber', 'Emergency contact number'],
  ['emergencyContactRelation', 'Emergency contact relation'], ['currentAddress', 'Current address'],
  ['permanentAddress', 'Permanent address'], ['city', 'City'], ['state', 'State'], ['postalCode', 'Postal code'],
]

const blankProfile = Object.fromEntries(profileFields.map(([key]) => [key, '']))

function completion(profile) {
  const complete = profileFields.filter(([key]) => Boolean(profile?.[key]?.toString().trim())).length
  return Math.round((complete / profileFields.length) * 100)
}

function ProfileEditor({ employee, profile, onClose, onSave, saving }) {
  const { register, handleSubmit } = useForm({ defaultValues: { ...blankProfile, ...profile } })
  return <form className="employee-form" onSubmit={handleSubmit((values) => onSave({ ...values, employeeId: employee.employeeId }))}>
    <div className="profile-editor-grid">
      <div className="form-section-label">PERSONAL DETAILS</div>
      <div className="form-grid">
        <label className="form-field"><span>Blood group</span><select {...register('bloodGroup')}><option value="">Not specified</option>{['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((value) => <option key={value}>{value}</option>)}</select></label>
        <label className="form-field"><span>Marital status</span><select {...register('maritalStatus')}><option value="">Not specified</option>{['SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED'].map((value) => <option key={value}>{value}</option>)}</select></label>
        <label className="form-field"><span>Nationality</span><input placeholder="Nationality" {...register('nationality')} /></label>
      </div>
      <div className="form-section-label form-section-spaced">EMERGENCY CONTACT</div>
      <div className="form-grid">
        <label className="form-field"><span>Contact name</span><input placeholder="Full name" {...register('emergencyContactName')} /></label>
        <label className="form-field"><span>Contact number</span><input type="tel" placeholder="Phone number" {...register('emergencyContactNumber')} /></label>
        <label className="form-field"><span>Relationship</span><input placeholder="Relationship" {...register('emergencyContactRelation')} /></label>
      </div>
      <div className="form-section-label form-section-spaced">ADDRESS</div>
      <div className="form-grid">
        <label className="form-field"><span>Current address</span><textarea rows="2" placeholder="Street address" {...register('currentAddress')} /></label>
        <label className="form-field"><span>Permanent address</span><textarea rows="2" placeholder="Street address" {...register('permanentAddress')} /></label>
        <label className="form-field"><span>City</span><input {...register('city')} /></label>
        <label className="form-field"><span>State</span><input {...register('state')} /></label>
        <label className="form-field"><span>Postal code</span><input {...register('postalCode')} /></label>
      </div>
    </div>
    <footer className="modal-actions"><button type="button" className="button button-secondary" onClick={onClose}>Cancel</button><button type="submit" className="button button-primary" disabled={saving}>{saving ? 'Saving…' : 'Save profile'}</button></footer>
  </form>
}

export default function ManageProfiles({ notify }) {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [employees, setEmployees] = useState([])
  const [departments, setDepartments] = useState([])
  const [profiles, setProfiles] = useState({})
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)
  const [saving, setSaving] = useState(false)
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setPage(0)
      setSearch(searchInput.trim())
    }, 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    Promise.all([
      employeeService.list({ page, size, search, sortBy: 'displayName', sortDirection: 'asc' }, controller.signal),
      departmentService.list(controller.signal),
    ]).then(async ([result, units]) => {
        const currentEmployees = result.content || []
        const profileRecords = await employeeProfileService.listForEmployees(
          currentEmployees.map((employee) => employee.employeeId),
          controller.signal,
        )
        const profileMap = Object.fromEntries(profileRecords.map((profile) => [profile.employeeId, profile]))
        if (!controller.signal.aborted) {
          setEmployees(currentEmployees)
          setDepartments(units)
          setProfiles(profileMap)
          setTotal(result.totalElements || 0)
          setTotalPages(result.totalPages || 0)
          setError('')
          setLoading(false)
        }
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(getApiErrorMessage(requestError, 'Employee profiles could not be loaded.'))
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [page, size, search, refresh])

  async function saveProfile(values) {
    setSaving(true)
    try {
      await employeeProfileService.update(modal.employee.employeeId, values)
      setModal(null)
      setRefresh((value) => value + 1)
      notify({ type: 'success', message: 'Employee profile saved.' })
    } catch (requestError) {
      notify({ type: 'error', message: getApiErrorMessage(requestError, 'Profile changes could not be saved.') })
    } finally {
      setSaving(false)
    }
  }

  return <PageLayout eyebrow="PEOPLE OPERATIONS / DIRECTORY" title="Manage profiles" description="Personal details, addresses and emergency contacts." action={<div className="profile-total"><ContactRound size={17} /><span>{total.toLocaleString()} profiles</span></div>}>
    <section className="directory-panel profile-directory-panel">
      <div className="directory-panel-heading"><div><h2>Employee profiles</h2><span>Completeness is based on saved profile fields</span></div><span className="directory-live"><i />LIVE DATA</span></div>
      <div className="profile-search-row"><label className="directory-search"><Search size={17} /><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Search employee name, email or ID" /></label><span className="profile-results"><ShieldCheck size={15} />Profile data is access-controlled</span></div>
      {error ? <ErrorState message={error} onRetry={() => setRefresh((value) => value + 1)} /> : loading ? <LoadingState label="Loading employee profiles" /> : employees.length === 0 ? <EmptyState title="No profiles found" /> : <>
        <DataTable className="employee-table profiles-table" columns={[
          { key: 'employee', label: 'Employee' }, { key: 'department', label: 'Department' },
          { key: 'completion', label: 'Completeness' }, { key: 'emergency', label: 'Emergency contact' },
          { key: 'actions', label: '', className: 'actions-heading' },
        ]}>
          {employees.map((employee) => {
            const profile = profiles[employee.employeeId] || {}
            const percent = completion(profile)
            return <tr key={employee.employeeId}>
              <td><button className="employee-identity" onClick={() => navigate(`/employee-management/employees/${employee.employeeId}`)}><span className="employee-avatar">{employee.firstName?.[0]}{employee.lastName?.[0]}</span><span className="employee-name-block"><strong>{employee.displayName}</strong><small>{employee.employeeCode} <i>·</i> {employee.officialEmail}</small></span></button></td>
              <td>{departments.find((department) => department.departmentId === employee.departmentId)?.departmentName || '—'}</td>
              <td><div className="profile-completion"><div className="completion-track"><i style={{ width: `${percent}%` }} /></div><strong>{percent}%</strong></div></td>
              <td><div className="emergency-cell"><strong>{profile.emergencyContactName || 'Not added'}</strong><small>{profile.emergencyContactNumber || 'Emergency contact missing'}</small></div></td>
              <td><button className="icon-button" aria-label={`Edit ${employee.displayName} profile`} title="Edit profile" onClick={() => setModal({ employee, profile })}><Pencil size={16} /></button></td>
            </tr>
          })}
        </DataTable>
        <Pagination page={page} totalPages={totalPages} totalElements={total} size={size} onPageChange={setPage} onSizeChange={(value) => { setSize(value); setPage(0) }} />
      </>}
    </section>
    {modal && <Modal title="Edit employee profile" description={`${modal.employee.displayName} · ${modal.employee.employeeCode}`} onClose={() => setModal(null)} size="large"><ProfileEditor employee={modal.employee} profile={modal.profile} onClose={() => setModal(null)} onSave={saveProfile} saving={saving} /></Modal>}
  </PageLayout>
}
