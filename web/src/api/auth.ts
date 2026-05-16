import { api } from './client';

export interface LoginResponse {
  data: { accessToken: string; refreshToken: string; username?: string; role?: string };
}

export const login = (usernameOrEmail: string, password: string) =>
  api.post<LoginResponse>('/auth/login', { usernameOrEmail, password });

export const logout = (refreshToken: string) =>
  api.post('/auth/logout', { refreshToken });
