import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // 网页开发服务 8081；接口与 WS 走代理到 Spring Boot 8082（/api 含 context-path）
  server: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
        ws: true
      }
    }
  },
  preview: {
    port: 4173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8082', changeOrigin: true, ws: true }
    }
  }
})
