import apiClient from '../api/client'

export const locationService = {
  async list(signal) {
    const response = await apiClient.get('/locations', { signal })
    return response.data
  },
}
