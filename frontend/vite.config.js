import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 代理配置：将 /api 请求转发到后端 Spring Boot（解决跨域）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      'normalize-wheel-es': fileURLToPath(new URL('./src/shims/normalize-wheel-es.js', import.meta.url))
    }
  }
})
