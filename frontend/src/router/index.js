import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import Profile from '../views/Profile.vue'
import Settings from '../views/Settings.vue'
import Results from '../views/Results.vue'

// Router is responsible for creating the web history and defining the application's route structure.
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'Home', component: Home },
    { path: '/login', name: 'Login', component: Login },
    { path: '/dashboard', name: 'Dashboard', component: Dashboard, meta: { requiresAuth: true } }, // requires authentication
    { path: '/profile', name: 'Profile', component: Profile, meta: { requiresAuth: true } }, // requires authentication
    { path: '/settings', name: 'Settings', component: Settings, meta: { requiresAuth: true } }, // requires authentication
    { path: '/results', name: 'Results', component: Results, meta: { requiresAuth: true } } // requires authentication
  ],
})

/**
 * Global navigation guard.
 * Before entering any route marked requiresAuth, call /api/auth/me.
 * The browser sends the JSESSIONID cookie automatically.
 * If the backend returns 401 (no session), we redirect to /login.
 */
router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true

  try {
    const res = await fetch('/api/auth/me', { credentials: 'include' })
    if (res.ok) return true
  } catch {
    // network error - redirect to login
  }

  return { name: 'Login' }
})

export default router
