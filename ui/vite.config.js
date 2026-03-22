import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/shorten': 'http://localhost:80',
      '/analytics': 'http://localhost:80',
      '^/[a-zA-Z0-9]{7}$': {
        target: 'http://localhost:80',
        changeOrigin: true,
        rewrite: (path) => path,
      },
    },
  },
})
