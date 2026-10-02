import { useEffect, useMemo, useState } from 'react'
import { ChevronDown, ChevronRight, Network, Search, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ErrorState, LoadingState, PageLayout } from '../components/ui'
import { getApiErrorMessage } from '../api/client'
import { departmentService } from '../services/departmentService'
import { employeeService } from '../services/employeeService'

function EmployeeNode({ employee, children, depth = 0 }) {
  const [expanded, setExpanded] = useState(depth < 2)
  return <div className="org-node" style={{ '--node-depth': depth }}>
    <div className="org-employee">
      {children.length > 0 ? <button className="org-expand" onClick={() => setExpanded((value) => !value)} aria-label={expanded ? 'Collapse reports' : 'Expand reports'}>{expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}</button> : <span className="org-expand-spacer" />}
      <span className="employee-avatar">{employee.firstName?.[0]}{employee.lastName?.[0]}</span>
      <span className="org-employee-copy"><Link to={`/employee-management/employees/${employee.employeeId}`}><strong>{employee.displayName || `${employee.firstName} ${employee.lastName}`}</strong></Link><small>{employee.employeeCode} · {employee.employeeStatus?.replaceAll('_', ' ')}</small></span>
      <span className="org-role">{employee.designationId ? `Designation ${employee.designationId}` : 'Employee'}</span>
      <span className="org-employee-count">{children.length || ''}</span>
    </div>
    {expanded && children.length > 0 && <div className="org-children">{children.map((child) => <EmployeeNode key={child.employeeId} employee={child.employee} children={child.children} depth={depth + 1} />)}</div>}
  </div>
}

function buildDepartmentTree(members) {
  const byId = new Map(members.map((employee) => [String(employee.employeeId), employee]))
  const childrenByManager = new Map()
  const roots = []

  members.forEach((employee) => {
    const managerId = employee.reportingManagerId == null ? null : String(employee.reportingManagerId)
    if (managerId && managerId !== String(employee.employeeId) && byId.has(managerId)) {
      const reports = childrenByManager.get(managerId) || []
      reports.push(employee)
      childrenByManager.set(managerId, reports)
    } else {
      roots.push(employee)
    }
  })

  const makeNode = (employee, ancestry = new Set()) => {
    const key = String(employee.employeeId)
    if (ancestry.has(key)) return { employee, children: [] }
    const nextAncestry = new Set(ancestry)
    nextAncestry.add(key)
    return {
      employee,
      children: (childrenByManager.get(key) || []).map((child) => makeNode(child, nextAncestry)),
    }
  }

  const tree = roots.map((employee) => makeNode(employee))
  const seen = new Set()
  const visit = (node) => { seen.add(String(node.employee.employeeId)); node.children.forEach(visit) }
  tree.forEach(visit)
  members.filter((employee) => !seen.has(String(employee.employeeId))).forEach((employee) => tree.push(makeNode(employee)))
  return tree
}

export default function OrganizationalStructure() {
  const [employees, setEmployees] = useState([])
  const [departments, setDepartments] = useState([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    Promise.all([employeeService.organizationTree(controller.signal), departmentService.list(controller.signal)])
      .then(([people, units]) => {
        setEmployees(people)
        setDepartments(units)
        setError('')
        setLoading(false)
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(getApiErrorMessage(requestError, 'Organization data could not be loaded.'))
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [refresh])

  const departmentTrees = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    const people = normalizedQuery ? employees.filter((employee) => `${employee.displayName} ${employee.employeeCode} ${employee.officialEmail}`.toLowerCase().includes(normalizedQuery)) : employees
    const departmentsWithPeople = new Map(departments.map((department) => [String(department.departmentId), department]))
    const groups = new Map()
    people.forEach((employee) => {
      const key = employee.departmentId == null ? 'unassigned' : String(employee.departmentId)
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key).push(employee)
    })
    return Array.from(groups, ([key, members]) => ({
      id: key,
      name: departmentsWithPeople.get(key)?.departmentName || (key === 'unassigned' ? 'Unassigned department' : `Department ${key}`),
      members,
      tree: buildDepartmentTree(members),
    })).sort((first, second) => first.name.localeCompare(second.name))
  }, [departments, employees, query])

  return <PageLayout eyebrow="PEOPLE OPERATIONS / STRUCTURE" title="Organizational structure" description="Explore reporting lines grouped by each employee’s current department." action={<span className="org-total"><UsersRound size={16} />{employees.length.toLocaleString()} people</span>}>
    <section className="directory-panel org-panel">
      <div className="org-toolbar"><label className="directory-search"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Find a person in the structure" /></label><span className="org-source"><Network size={14} />Department and reporting assignments</span></div>
      {error ? <ErrorState message={error} onRetry={() => setRefresh((value) => value + 1)} /> : loading ? <LoadingState label="Building organization structure from employee records" /> : departmentTrees.length === 0 ? <div className="empty-state"><span className="empty-mark">—</span><strong>No matching employees</strong><p>Adjust the search to see reporting lines.</p></div> : <div className="org-departments">
        {departmentTrees.map((department) => <section className="org-department" key={department.id}>
          <header className="org-department-header"><div><span className="org-department-icon"><Network size={16} /></span><h2>{department.name}</h2></div><span>{department.members.length} {department.members.length === 1 ? 'employee' : 'employees'}</span></header>
          <div className="org-tree">{department.tree.map((node) => <EmployeeNode key={node.employee.employeeId} employee={node.employee} children={node.children} />)}</div>
        </section>)}
      </div>}
    </section>
  </PageLayout>
}
