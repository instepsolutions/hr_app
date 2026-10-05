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
