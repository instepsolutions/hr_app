import apiClient from '../api/client'

export const employeeService = {
  async list(params, signal) {
    const response = await apiClient.get('/employees', { params, signal })
    return response.data
  },
  async statistics(signal) {
    const response = await apiClient.get('/employees/statistics', { signal })
    return response.data
  },
  async get(id, signal) {
    const response = await apiClient.get(`/employees/${id}`, { signal })
    return response.data
  },
  async create(payload) {
    const response = await apiClient.post('/employees', payload)
    return response.data
  },
  async update(id, payload) {
    const response = await apiClient.put(`/employees/${id}`, payload)
    return response.data
  },
  async remove(id) {
    return apiClient.delete(`/employees/${id}`)
  },
  async updateStatus(id, payload) {
    const response = await apiClient.patch(`/employees/${id}/status`, payload)
    return response.data
  },
  async organizationTree(signal) {
    const response = await apiClient.get('/organization/tree', { signal })
    return response.data
  },
}
