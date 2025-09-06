import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      // Fast Refresh 명시적 활성화
      fastRefresh: true,
      // DevTools 호환성 개선
      jsxRuntime: 'automatic'
    })
  ],
  server: {
    port: 5177,
    host: true,
    // 도메인 허용 설정
    allowedHosts: [
      'localhost',
      'holo-oshi.com',
      'www.holo-oshi.com'
    ]
  },
  // DevTools 충돌 방지
  define: {
    __REACT_DEVTOOLS_GLOBAL_HOOK__: '({ isDisabled: true })'
  }
})
