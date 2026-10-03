import apiClient from '../api/client'

export const dashboardService = {
  async overview(date, months, signal) {
    const response = await apiClient.get('/dashboard/overview', {
      params: { date, months },
      signal,
    })
    return response.data
  },
}
