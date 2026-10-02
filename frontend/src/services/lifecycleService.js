import apiClient from '../api/client'

export const lifecycleService = {
  async statistics(signal) {
    const response = await apiClient.get('/lifecycle/statistics', { signal })
    return response.data
  },
  async events(signal) {
    const response = await apiClient.get('/lifecycle/events', { signal })
    return response.data
  },
  async employeeEvents(employeeId, signal) {
    const response = await apiClient.get(`/employees/${employeeId}/lifecycle`, { signal })
    return response.data
  },
  async statusHistory(employeeId, signal) {
    const response = await apiClient.get(`/employees/${employeeId}/status-history`, { signal })
    return response.data
  },
}
