import apiClient from '../api/client'

export const bulkActionService = {
  async create(payload) {
    const response = await apiClient.post('/bulk-actions', payload)
    return response.data
  },
  async get(id, signal) {
    const response = await apiClient.get(`/bulk-actions/${id}`, { signal })
    return response.data
  },
  async history(signal) {
    const response = await apiClient.get('/bulk-actions/history', { signal, params: { sort: 'requestedAt,desc' } })
    return response.data
  },
}
