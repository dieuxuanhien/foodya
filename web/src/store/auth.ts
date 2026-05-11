import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  username: string | null;
  role: string | null;
  setTokens: (access: string, refresh: string, username: string, role: string) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      username: null,
      role: null,
      setTokens: (accessToken, refreshToken, username, role) =>
        set({ accessToken, refreshToken, username, role }),
      clear: () => set({ accessToken: null, refreshToken: null, username: null, role: null }),
    }),
    { name: 'foodya-admin-auth' }
  )
);
