import apiClient from '../api/client'

export const employmentTypeService = {
  async list(signal) {
    const response = await apiClient.get('/employment-types', { signal })
    return response.data
  },
}
