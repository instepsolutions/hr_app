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
  async managerReviewOverview(params, signal) {
    return (await apiClient.get('/pms/reviews/manager/overview', { params, signal })).data
  },
  async managerReviewQueue(params, signal) {
    return (await apiClient.get('/pms/reviews/manager', { params, signal })).data
  },
  async getManagerReview(appraisalId, signal) {
    return (await apiClient.get(`/pms/reviews/manager/${appraisalId}`, { signal })).data
  },
  async saveManagerReview(appraisalId, payload) {
    return (await apiClient.put(`/pms/reviews/manager/${appraisalId}/draft`, payload)).data
  },
  async submitManagerReview(appraisalId) {
    return (await apiClient.post(`/pms/reviews/manager/${appraisalId}/submit`)).data
  },
  async sendBackManagerReview(appraisalId, reason) {
    return (await apiClient.post(`/pms/reviews/manager/${appraisalId}/send-back`, { reason })).data
  },
  async requestManagerClarification(appraisalId, reason) {
    return (await apiClient.post(`/pms/reviews/manager/${appraisalId}/clarification`, { reason })).data
  },
  async remindManagerReview(appraisalId) {
    return (await apiClient.post(`/pms/reviews/manager/${appraisalId}/remind`)).data
  },
  async hrReviewOverview(params, signal) {
    return (await apiClient.get('/pms/reviews/hr/overview', { params, signal })).data
  },
  async hrReviewQueue(params, signal) {
    return (await apiClient.get('/pms/reviews/hr', { params, signal })).data
  },
  async hrReviewers(signal) {
    return (await apiClient.get('/pms/reviews/hr/reviewers', { signal })).data
  },
  async getHrReview(appraisalId, signal) {
    return (await apiClient.get(`/pms/reviews/hr/${appraisalId}`, { signal })).data
  },
  async saveHrReview(appraisalId, payload) {
    return (await apiClient.put(`/pms/reviews/hr/${appraisalId}/draft`, payload)).data
  },
  async submitHrReview(appraisalId) {
    return (await apiClient.post(`/pms/reviews/hr/${appraisalId}/submit`)).data
  },
  async reassignHrReview(appraisalId, reviewerId) {
    return (await apiClient.post(`/pms/reviews/hr/${appraisalId}/reassign`, { reviewerId })).data
  },
  async remindHrReview(appraisalId) {
    return (await apiClient.post(`/pms/reviews/hr/${appraisalId}/remind`)).data
  },
  async reviewHistory(appraisalId, signal) {
    return (await apiClient.get(`/pms/reviews/${appraisalId}/history`, { signal })).data
  },
  async exportReviews(type, params) {
    const response = await apiClient.get(`/pms/reviews/${type}/export`, { params, responseType: 'blob' })
    blobDownload(response, `${type.toLowerCase()}-review-report.csv`)
  },
  async appraisalSummaryOverview(params, signal) {
    return (await apiClient.get('/pms/appraisals/summary/overview', { params, signal })).data
  },
  async appraisalSummaryList(params, signal) {
    return (await apiClient.get('/pms/appraisals/summary', { params, signal })).data
  },
  async appraisalSummaryDetail(appraisalId, signal) {
    return (await apiClient.get(`/pms/appraisals/summary/${appraisalId}`, { signal })).data
  },
  async exportAppraisalSummary(params) {
    const response = await apiClient.get('/pms/appraisals/summary/export', { params, responseType: 'blob' })
    blobDownload(response, 'appraisal-summary-report.csv')
  },
  async pipOverview(params, signal) {
    return (await apiClient.get('/pms/pips/overview', { params, signal })).data
  },
  async pipList(params, signal) {
    return (await apiClient.get('/pms/pips', { params, signal })).data
  },
  async pipDetail(pipId, signal) {
    return (await apiClient.get(`/pms/pips/${pipId}`, { signal })).data
  },
  async pipTemplates(signal) {
    return (await apiClient.get('/pms/pips/templates', { signal })).data
  },
  async createPip(payload) {
    return (await apiClient.post('/pms/pips', payload)).data
  },
  async updatePip(pipId, payload) {
    return (await apiClient.put(`/pms/pips/${pipId}`, payload)).data
  },
  async startPip(pipId) {
    return (await apiClient.post(`/pms/pips/${pipId}/start`)).data
  },
  async addPipReview(pipId, payload) {
    return (await apiClient.post(`/pms/pips/${pipId}/reviews`, payload)).data
  },
  async extendPip(pipId, payload) {
    return (await apiClient.post(`/pms/pips/${pipId}/extend`, payload)).data
  },
  async closePip(pipId, payload) {
    return (await apiClient.post(`/pms/pips/${pipId}/close`, payload)).data
  },
  async cancelPip(pipId, reason) {
    return (await apiClient.post(`/pms/pips/${pipId}/cancel`, { reason })).data
  },
  async remindPip(pipId) {
    return (await apiClient.post(`/pms/pips/${pipId}/remind`)).data
  },
  async exportPips(params) {
    const response = await apiClient.get('/pms/pips/export', { params, responseType: 'blob' })
    blobDownload(response, 'pip-report.csv')
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
