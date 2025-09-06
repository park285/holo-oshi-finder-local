const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || (window.location.protocol === 'https:' ? 'https://holo-oshi.com' : 'http://localhost'),
  ENDPOINTS: {
    ANALYZE: '/api/analyze/final',
    SEARCH: '/api/search/compound',
    HEALTH: '/api/health'
  },
  TIMEOUT: 300000 // 300초
} as const

export default API_CONFIG

export const apiClient = {
  async post<T = any>(endpoint: string, data: any): Promise<T> {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), API_CONFIG.TIMEOUT)
    
    try {
      const response = await fetch(`${API_CONFIG.BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data),
        signal: controller.signal
      })
      
      clearTimeout(timeoutId)
      
      if (!response.ok) {
        throw new Error(`API Error: ${response.status} ${response.statusText}`)
      }
      
      return response.json()
    } catch (error: any) {
      clearTimeout(timeoutId)
      if (error.name === 'AbortError') {
        throw new Error(`API 타임아웃 (${API_CONFIG.TIMEOUT/1000}초)`)
      }
      throw error
    }
  },
  
  async get<T = any>(endpoint: string): Promise<T> {
    const response = await fetch(`${API_CONFIG.BASE_URL}${endpoint}`)
    
    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`)
    }
    
    return response.json()
  }
}