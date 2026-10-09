/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        navy: {
          950: '#060a12', // deepest background
          900: '#0b1120', // main body background
          850: '#0f172a', // panel background
          800: '#15203b', // card / surface background
          750: '#1b2a4e', // surface hover / secondary
          700: '#243765', // borders
          600: '#354e8c', // subtle accents
          500: '#4d6eb5',
        },
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6', // primary brand blue
          600: '#2563eb',
          700: '#1d4ed8',
        },
        risk: {
          low: '#10b981',    // emerald green
          medium: '#f59e0b', // amber yellow
          high: '#ef4444',   // red
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
