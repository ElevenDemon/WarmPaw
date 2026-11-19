import { token, loginPage } from '../utils/auth'
import { useUserStore } from '../store/modules/user'
export function useAuth() { const user = useUserStore(); const requireAuth = async () => { if(!token()) { loginPage(); return false } await user.restore(); if (!user.user && !token()) loginPage(); return !!user.user }; return {user,requireAuth} }
