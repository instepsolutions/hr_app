import { useEffect, useState } from 'react'
import { ArrowLeft, BriefcaseBusiness, CalendarDays, ContactRound, Mail, MapPin, Phone, UserRound } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { DataTable, ErrorState, LoadingState, PageLayout } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { departmentService } from '../services/departmentService'
import { designationService } from '../services/designationService'
import { employeeProfileService } from '../services/employeeProfileService'
import { employeeService } from '../services/employeeService'
import { employmentTypeService } from '../services/employmentTypeService'
import { lifecycleService } from '../services/lifecycleService'
import { locationService } from '../services/locationService'

function EmployeeField({ icon: Icon, label, value }) {
  return <div className="details-field"><span className="details-field-icon"><Icon size={16} /></span><div><small>{label}</small><strong>{value || '—'}</strong></div></div>
}

function dateLabel(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(`${value.slice(0, 10)}T00:00:00`))
}

function DetailSection({ id, title, children }) {
  return <section className="employee-detail-section" id={id}><div className="employee-detail-section-title"><h2>{title}</h2></div><div className="details-grid">{children}</div></section>
}

export default function EmployeeDetails() {
  const { id } = useParams()
  const [record, setRecord] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    Promise.all([
      employeeService.get(id, controller.signal),
      employeeProfileService.get(id, controller.signal),
      lifecycleService.employeeEvents(id, controller.signal),
      lifecycleService.statusHistory(id, controller.signal),
      departmentService.list(controller.signal),
      locationService.list(controller.signal),
      designationService.list(controller.signal),
      employmentTypeService.list(controller.signal),
    ]).then(async ([employee, profile, lifecycle, history, departments, locations, designations, employmentTypes]) => {
      const manager = employee.reportingManagerId ? await employeeService.get(employee.reportingManagerId, controller.signal) : null
      if (controller.signal.aborted) return
      setRecord({
        employee,
        profile: profile || {},
        lifecycle,
        history,
        manager,
        department: departments.find((item) => item.departmentId === employee.departmentId)?.departmentName || '—',
        location: locations.find((item) => item.locationId === employee.locationId)?.locationName || '—',
        designation: designations.find((item) => item.designationId === employee.designationId)?.designationName || '—',
        employmentType: employmentTypes.find((item) => item.employmentTypeId === employee.employmentTypeId)?.employmentTypeName || '—',
      })
      setError('')
      setLoading(false)
    }).catch((requestError) => {
      if (!controller.signal.aborted) {
        setError(getApiErrorMessage(requestError, 'Employee details could not be loaded.'))
        setLoading(false)
      }
    })
    return () => controller.abort()
  }, [id, retry])

  const employee = record?.employee
  const profile = record?.profile || {}

  return <PageLayout eyebrow="PEOPLE OPERATIONS / DIRECTORY" title={employee?.displayName || 'Employee profile'} description={employee ? `${employee.employeeCode} · ${record.designation}` : 'Complete employee record'} action={<Link className="button button-secondary" to="/employee-management/directory"><ArrowLeft size={16} />Back to directory</Link>}>
    {loading ? <LoadingState label="Loading employee overview, profile and history" /> : error ? <ErrorState message={error} onRetry={() => setRetry((value) => value + 1)} /> : record && <>
      <section className="employee-profile-hero"><span className="details-avatar">{employee.firstName?.[0]}{employee.lastName?.[0]}</span><div className="profile-hero-copy"><h2>{employee.displayName}</h2><p>{record.designation} <span>·</span> {record.department}</p><span className={`status-badge status-${employee.employeeStatus?.toLowerCase()}`}><i />{employee.employeeStatus?.replaceAll('_', ' ')}</span></div><div className="profile-hero-actions"><Link className="button button-secondary" to="/employee-management/manage-profiles"><ContactRound size={15} />Manage profile</Link></div></section>
      <div className="profile-section-nav">{[['overview', 'Overview'], ['personal', 'Personal information'], ['employment', 'Employment'], ['contact', 'Contact'], ['reporting', 'Reporting'], ['lifecycle', 'Lifecycle'], ['history', 'Status history']].map(([anchor, label]) => <a key={anchor} href={`#${anchor}`}>{label}</a>)}</div>
      <div className="employee-profile-sections">
        <DetailSection id="overview" title="Overview">
          <EmployeeField icon={UserRound} label="Employee ID" value={employee.employeeCode} />
          <EmployeeField icon={BriefcaseBusiness} label="Current status" value={employee.employeeStatus?.replaceAll('_', ' ')} />
          <EmployeeField icon={CalendarDays} label="Date joined" value={dateLabel(employee.dateOfJoining)} />
          <EmployeeField icon={ContactRound} label="Profile last updated" value={dateLabel(profile.updatedAt || employee.updatedAt)} />
        </DetailSection>
        <DetailSection id="personal" title="Personal information">
          <EmployeeField icon={UserRound} label="First name" value={employee.firstName} />
          <EmployeeField icon={UserRound} label="Middle name" value={employee.middleName} />
          <EmployeeField icon={UserRound} label="Last name" value={employee.lastName} />
          <EmployeeField icon={CalendarDays} label="Date of birth" value={dateLabel(employee.dateOfBirth)} />
          <EmployeeField icon={UserRound} label="Gender" value={employee.gender?.replaceAll('_', ' ')} />
          <EmployeeField icon={ContactRound} label="Nationality" value={profile.nationality} />
          <EmployeeField icon={ContactRound} label="Blood group" value={profile.bloodGroup} />
          <EmployeeField icon={ContactRound} label="Marital status" value={profile.maritalStatus?.replaceAll('_', ' ')} />
        </DetailSection>
        <DetailSection id="employment" title="Employment information">
          <EmployeeField icon={BriefcaseBusiness} label="Department" value={record.department} />
          <EmployeeField icon={BriefcaseBusiness} label="Designation" value={record.designation} />
          <EmployeeField icon={BriefcaseBusiness} label="Employment type" value={record.employmentType} />
          <EmployeeField icon={MapPin} label="Work location" value={record.location} />
          <EmployeeField icon={CalendarDays} label="Date of joining" value={dateLabel(employee.dateOfJoining)} />
          <EmployeeField icon={CalendarDays} label="Confirmation date" value={dateLabel(employee.confirmationDate)} />
          <EmployeeField icon={CalendarDays} label="Date of exit" value={dateLabel(employee.dateOfExit)} />
        </DetailSection>
        <DetailSection id="contact" title="Contact information">
          <EmployeeField icon={Mail} label="Official email" value={employee.officialEmail} />
          <EmployeeField icon={Mail} label="Personal email" value={employee.personalEmail} />
          <EmployeeField icon={Phone} label="Mobile" value={employee.mobileNumber} />
          <EmployeeField icon={Phone} label="Alternate mobile" value={employee.alternateMobile} />
          <EmployeeField icon={MapPin} label="Current address" value={profile.currentAddress} />
          <EmployeeField icon={MapPin} label="Permanent address" value={profile.permanentAddress} />
          <EmployeeField icon={MapPin} label="City / State / Postal code" value={[profile.city, profile.state, profile.postalCode].filter(Boolean).join(', ')} />
          <EmployeeField icon={Phone} label="Emergency contact" value={[profile.emergencyContactName, profile.emergencyContactRelation].filter(Boolean).join(' · ')} />
          <EmployeeField icon={Phone} label="Emergency number" value={profile.emergencyContactNumber} />
        </DetailSection>
        <DetailSection id="reporting" title="Reporting structure">
          <EmployeeField icon={UserRound} label="Reporting manager" value={record.manager?.displayName || (employee.reportingManagerId ? `Employee #${employee.reportingManagerId}` : 'No manager assigned')} />
          <EmployeeField icon={BriefcaseBusiness} label="Manager employee ID" value={employee.reportingManagerId || '—'} />
          <EmployeeField icon={BriefcaseBusiness} label="Department ID" value={employee.departmentId} />
        </DetailSection>
        <section className="employee-detail-section" id="lifecycle"><div className="employee-detail-section-title"><h2>Lifecycle</h2><span>{record.lifecycle.length} events</span></div>{record.lifecycle.length ? <DataTable className="employee-history-table" columns={[{ key: 'date', label: 'Date' }, { key: 'event', label: 'Event' }, { key: 'description', label: 'Description' }]}>{record.lifecycle.map((event) => <tr key={event.eventId}><td>{dateLabel(event.eventDate)}</td><td>{event.eventType?.replaceAll('_', ' ')}</td><td>{event.description || '—'}</td></tr>)}</DataTable> : <p className="profile-empty-note">No lifecycle events recorded.</p>}</section>
        <section className="employee-detail-section" id="history"><div className="employee-detail-section-title"><h2>Status history</h2><span>{record.history.length} changes</span></div>{record.history.length ? <DataTable className="employee-history-table" columns={[{ key: 'date', label: 'Effective date' }, { key: 'from', label: 'Previous status' }, { key: 'to', label: 'New status' }, { key: 'reason', label: 'Reason' }, { key: 'by', label: 'Changed by' }]}>{record.history.map((entry) => <tr key={entry.historyId}><td>{dateLabel(entry.effectiveDate)}</td><td>{entry.oldStatus || '—'}</td><td>{entry.newStatus}</td><td>{entry.reason || '—'}</td><td>{entry.changedBy || '—'}</td></tr>)}</DataTable> : <p className="profile-empty-note">No status changes recorded.</p>}</section>
      </div>
    </>}
  </PageLayout>
}
