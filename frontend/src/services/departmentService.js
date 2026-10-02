import apiClient from '../api/client'

export const departmentService = {
  async list(signal) {
    const response = await apiClient.get('/departments', { signal })
    return response.data
  },
}
