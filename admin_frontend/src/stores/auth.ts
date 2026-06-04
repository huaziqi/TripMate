import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('admin_token') ?? '')
  const username = ref<string>(localStorage.getItem('admin_username') ?? '')
  const role = ref<string>(localStorage.getItem('admin_role') ?? '')

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  function setAuth(t: string, u: string, r: string) {
    token.value = t
    username.value = u
    role.value = r
    localStorage.setItem('admin_token', t)
    localStorage.setItem('admin_username', u)
    localStorage.setItem('admin_role', r)
  }

  function clearAuth() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_username')
    localStorage.removeItem('admin_role')
  }

  return { token, username, role, isLoggedIn, isSuperAdmin, setAuth, clearAuth }
})
