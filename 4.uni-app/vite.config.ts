import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
// Root source layout, as supported by UNI_INPUT_DIR (see scripts/run.mjs).
process.env.UNI_INPUT_DIR = __dirname
export default defineConfig({ plugins: [uni()], server: { host: '127.0.0.1', port: 5175, proxy: { '/api': { target: process.env.BACKEND_URL || 'http://localhost:8080', changeOrigin: true, configure(proxy) { proxy.on('proxyReq', req => req.removeHeader('origin')) } } } } })
