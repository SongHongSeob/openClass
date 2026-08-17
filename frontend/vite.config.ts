import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// SPEC-FRONTEND-001 plan.md §C.5 / §M1: this config intentionally omits any dev
// proxy configuration (mechanically checked — see plan.md E11). The dev server
// port is pinned to 5173 (the backend's default CORS allowed origin, spec.md
// §A.5) with strictPort so a port collision fails loudly instead of silently
// shifting to a port the backend will reject (plan.md AP-6).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
