import apiClient from '../api/client'

export const designationService = {
  async list(signal) {
    const response = await apiClient.get('/designations', { signal })
    return response.data
  },
}
