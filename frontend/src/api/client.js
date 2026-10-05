import axios from 'axios'

export const AUTH_STORAGE_KEY = 'hrms.auth'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
})

apiClient.interceptors.request.use((config) => {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY)
  if (stored) {
    try {
      const { token } = JSON.parse(stored)
      if (token) config.headers.Authorization = `Bearer ${token}`
    } catch {
      localStorage.removeItem(AUTH_STORAGE_KEY)
    }
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    // A 403 that carries our JSON error body is a business rule (for example a role limit), not an expired session.
    const isBusinessForbidden = status === 403 && typeof error.response?.data?.message === 'string'
    if ([401, 403].includes(status) && !isBusinessForbidden && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      window.dispatchEvent(new CustomEvent('hrms:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export function getApiErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data
  if (typeof data?.message === 'string') return data.message
  if (data?.message && typeof data.message === 'object') {
    return Object.values(data.message).join(' ')
  }
  if (error?.code === 'ERR_NETWORK') return 'Unable to reach the HRMS API. Check that the backend is running at localhost:8080.'
  return fallback
}

export default apiClient
