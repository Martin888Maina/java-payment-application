import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // Sends API calls to the Spring Boot app during development
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
