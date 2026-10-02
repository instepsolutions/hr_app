import apiClient, { AUTH_STORAGE_KEY } from '../api/client'

export const authService = {
  async login(credentials) {
    const response = await apiClient.post('/auth/login', credentials)
    return response.data.data
  },
  save(session) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
  },
  read() {
    try {
      return JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || 'null')
    } catch {
      return null
    }
  },
  clear() {
    localStorage.removeItem(AUTH_STORAGE_KEY)
  },
}
