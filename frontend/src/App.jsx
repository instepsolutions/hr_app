import { Suspense, lazy, useCallback, useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import AppShell from './components/AppShell'
import { LoadingState, Toast } from './components/ui'
import { authService } from './services/authService'

const EmployeeDetails = lazy(() => import('./pages/EmployeeDetails'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const EmployeeDirectory = lazy(() => import('./pages/EmployeeDirectory'))
const ManageProfiles = lazy(() => import('./pages/ManageProfiles'))
const OrganizationalStructure = lazy(() => import('./pages/OrganizationalStructure'))
const EmployeeLifecycle = lazy(() => import('./pages/EmployeeLifecycle'))
const BulkActions = lazy(() => import('./pages/BulkActions'))
const LoginPage = lazy(() => import('./pages/LoginPage'))
const PlaceholderPage = lazy(() => import('./pages/PlaceholderPage'))
const PMSDashboardPage = lazy(() => import('./pms/PMSDashboardPage'))
const GoalManagementPage = lazy(() => import('./pms/GoalManagementPage'))
const KraKpiSetupPage = lazy(() => import('./pms/KraKpiSetupPage'))
const SelfAppraisalPage = lazy(() => import('./pms/SelfAppraisalPage'))
const PMSPlaceholderPage = lazy(() => import('./pms/PMSPlaceholderPage'))

function ProtectedLayout({ session, onLogout, onNotify }) {
  return session ? <AppShell session={session} onLogout={onLogout} onNotify={onNotify}><Outlet /></AppShell> : <Navigate to="/login" replace />
}

function AppRoutes() {
  const [session, setSession] = useState(() => authService.read())
  const [toast, setToast] = useState(null)
  const notify = useCallback((nextToast) => setToast(nextToast), [])
  const dismissToast = useCallback(() => setToast(null), [])

  useEffect(() => {
    const onUnauthorized = () => setSession(null)
    const onToast = (event) => setToast(event.detail)
    window.addEventListener('hrms:unauthorized', onUnauthorized)
    window.addEventListener('hrms:toast', onToast)
    return () => {
      window.removeEventListener('hrms:unauthorized', onUnauthorized)
      window.removeEventListener('hrms:toast', onToast)
    }
  }, [])

  async function signIn(credentials) {
    const nextSession = await authService.login(credentials)
    authService.save(nextSession)
    setSession(nextSession)
  }

  function signOut() {
    authService.clear()
    setSession(null)
  }

  return <>
    <Suspense fallback={<LoadingState label="Opening workspace" />}><Routes>
      <Route path="/login" element={session ? <Navigate to="/dashboard" replace /> : <LoginPage onLogin={signIn} />} />
      <Route element={<ProtectedLayout session={session} onLogout={signOut} onNotify={notify} />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage session={session} notify={notify} />} />
        <Route path="/employee-management/directory" element={<EmployeeDirectory notify={notify} />} />
        <Route path="/employee-management/organizational-structure" element={<OrganizationalStructure />} />
        <Route path="/employee-management/manage-profiles" element={<ManageProfiles notify={notify} />} />
        <Route path="/employee-management/employee-lifecycle" element={<EmployeeLifecycle />} />
        <Route path="/employee-management/bulk-actions" element={<BulkActions notify={notify} />} />
        <Route path="/employee-management/employees/:id" element={<EmployeeDetails />} />
        <Route path="/performance" element={<Navigate to="/performance/pms-dashboard" replace />} />
        <Route path="/performance/pms-dashboard/:tab?" element={<PMSDashboardPage />} />
        <Route path="/performance/goal-management/:tab?" element={<GoalManagementPage session={session} notify={notify} />} />
        <Route path="/performance/kra-kpi/:tab?" element={<KraKpiSetupPage notify={notify} />} />
        <Route path="/performance/self-appraisal/:tab?" element={<SelfAppraisalPage notify={notify} />} />
        <Route path="/performance/:section" element={<PMSPlaceholderPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes></Suspense>
    <Toast toast={toast} onClose={dismissToast} />
  </>
}

export default function App() {
  return <BrowserRouter><AppRoutes /></BrowserRouter>
}
