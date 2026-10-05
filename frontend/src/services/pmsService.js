import apiClient from '../api/client'

function blobDownload(response, filename) {
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

export const pmsService = {
  async dashboard(params, signal) {
    return (await apiClient.get('/pms/dashboard/overview', { params, signal })).data
  },
  async trend(params, signal) {
    return (await apiClient.get('/pms/dashboard/goal-trend', { params, signal })).data
  },
  async goalOverview(params, signal) {
    return (await apiClient.get('/pms/goals/overview', { params, signal })).data
  },
  async listGoals(params, signal) {
    return (await apiClient.get('/pms/goals', { params, signal })).data
  },
  async getGoal(id, signal) {
    return (await apiClient.get(`/pms/goals/${id}`, { signal })).data
  },
  async history(id, signal) {
    return (await apiClient.get(`/pms/goals/${id}/history`, { signal })).data
  },
  async createGoal(payload) {
    return (await apiClient.post('/pms/goals', payload)).data
  },
  async updateGoal(id, payload) {
    return (await apiClient.put(`/pms/goals/${id}`, payload)).data
  },
  async updateProgress(id, payload) {
    return (await apiClient.patch(`/pms/goals/${id}/progress`, payload)).data
  },
  async archiveGoal(id, reason = 'Archived from Goal Management') {
    return (await apiClient.post(`/pms/goals/${id}/archive`, { reason })).data
  },
  async restoreGoal(id) {
    return (await apiClient.post(`/pms/goals/${id}/restore`)).data
  },
  async removeGoal(id) {
    return apiClient.delete(`/pms/goals/${id}`)
  },
  async alignment(signal) {
    return (await apiClient.get('/pms/goals/alignment', { signal })).data
  },
  async calendar(params, signal) {
    return (await apiClient.get('/pms/goals/calendar', { params, signal })).data
  },
  async lookups(signal) {
    const [categories, kras, kpis] = await Promise.all([
      apiClient.get('/pms/lookups/categories', { signal }),
      apiClient.get('/pms/lookups/kras', { signal }),
      apiClient.get('/pms/lookups/kpis', { signal }),
    ])
    return { categories: categories.data, kras: kras.data, kpis: kpis.data }
  },
  async setupOverview(params, signal) {
    return (await apiClient.get('/pms/setup/overview', { params, signal })).data
  },
  async listKras(params, signal) {
    return (await apiClient.get('/pms/setup/kras', { params, signal })).data
  },
  async createKra(payload) {
    return (await apiClient.post('/pms/setup/kras', payload)).data
  },
  async updateKra(id, payload) {
    return (await apiClient.put(`/pms/setup/kras/${id}`, payload)).data
  },
  async deleteKra(id) {
    return apiClient.delete(`/pms/setup/kras/${id}`)
  },
  async duplicateKra(id) {
    return (await apiClient.post(`/pms/setup/kras/${id}/duplicate`)).data
  },
  async listKpis(params, signal) {
    return (await apiClient.get('/pms/setup/kpis', { params, signal })).data
  },
  async createKpi(payload) {
    return (await apiClient.post('/pms/setup/kpis', payload)).data
  },
  async updateKpi(id, payload) {
    return (await apiClient.put(`/pms/setup/kpis/${id}`, payload)).data
  },
  async deleteKpi(id) {
    return apiClient.delete(`/pms/setup/kpis/${id}`)
  },
  async duplicateKpi(id) {
    return (await apiClient.post(`/pms/setup/kpis/${id}/duplicate`)).data
  },
  async departmentMappings(departmentId, signal) {
    return (await apiClient.get(`/pms/setup/departments/${departmentId}/mappings`, { signal })).data
  },
  async saveDepartmentMappings(departmentId, payload) {
    return (await apiClient.put(`/pms/setup/departments/${departmentId}/mappings`, payload)).data
  },
  async employeeMappings(employeeId, signal) {
    return (await apiClient.get(`/pms/setup/employees/${employeeId}/mappings`, { signal })).data
  },
  async saveEmployeeMappings(employeeId, payload) {
    return (await apiClient.put(`/pms/setup/employees/${employeeId}/mappings`, payload)).data
  },
  async setupAlignment(signal) {
    return (await apiClient.get('/pms/setup/alignment', { signal })).data
  },
  async setupHistory(params, signal) {
    return (await apiClient.get('/pms/setup/history', { params, signal })).data
  },
  async exportSetup(params) {
    const response = await apiClient.get('/pms/setup/export', { params, responseType: 'blob' })
    blobDownload(response, 'kra-kpi-report.csv')
  },
  async appraisalOverview(params, signal) {
    return (await apiClient.get('/pms/appraisals/overview', { params, signal })).data
  },
  async myAppraisal(signal) {
    return (await apiClient.get('/pms/appraisals/mine', { signal })).data
  },
  async saveMyAppraisal(payload) {
    return (await apiClient.put('/pms/appraisals/mine', payload)).data
  },
  async submitMyAppraisal() {
    return (await apiClient.post('/pms/appraisals/mine/submit')).data
  },
  async withdrawMyAppraisal() {
    return (await apiClient.post('/pms/appraisals/mine/withdraw')).data
  },
  async appraisalComments(appraisalId, signal) {
    return (await apiClient.get(`/pms/appraisals/${appraisalId}/comments`, { signal })).data
  },
  async addAppraisalComment(appraisalId, payload) {
    return (await apiClient.post(`/pms/appraisals/${appraisalId}/comments`, payload)).data
  },
  async updateAppraisalTimeline(cycleId, payload) {
    return (await apiClient.put(`/pms/appraisals/cycles/${cycleId}/timeline`, payload)).data
  },
  async exportAppraisals(params) {
    const response = await apiClient.get('/pms/appraisals/export', { params, responseType: 'blob' })
    blobDownload(response, 'self-appraisal-report.csv')
  },
  async departments(signal) {
    return (await apiClient.get('/departments', { signal })).data
  },
  async employees(search, signal) {
    return (await apiClient.get('/employees', { params: { search, page: 0, size: 8, sortBy: 'displayName', sortDirection: 'asc' }, signal })).data
  },
  async exportGoals(params) {
    const response = await apiClient.get('/pms/goals/export', { params, responseType: 'blob' })
    blobDownload(response, 'pms-goals.csv')
  },
  async exportDashboard(params) {
    const response = await apiClient.get('/pms/dashboard/export', { params, responseType: 'blob' })
    blobDownload(response, 'pms-dashboard.csv')
  },
}
