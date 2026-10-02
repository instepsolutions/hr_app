import apiClient from '../api/client'

export const employeeProfileService = {
  async get(employeeId, signal) {
    const response = await apiClient.get(`/employees/${employeeId}/profile`, { signal })
    return response.data
  },
  async listForEmployees(employeeIds, signal) {
    if (!employeeIds.length) return []
    const response = await apiClient.get('/employee-profiles', { params: { employeeIds }, signal })
    return response.data
  },
  async update(employeeId, payload) {
    const response = await apiClient.put(`/employees/${employeeId}/profile`, payload)
    return response.data
  },
}
