import { api } from './client';

export interface User {
  id: string;
  username: string;
  email: string;
  phoneNumber: string;
  fullName: string;
  role: string;
  status: string;
}

export interface PageMeta { page: number; size: number; totalElements: number; totalPages: number; }
export interface PagedResponse<T> { data: T[]; meta: PageMeta; }

export const getUsers = (q?: string, page = 0, size = 20) =>
  api.get<{ data: User[]; meta: PageMeta }>('/admin/users', { params: { q, page, size } });

export const lockUser = (id: string) =>
  api.post<{ data: User }>(`/admin/users/${id}/lock`);

export const unlockUser = (id: string) =>
  api.post<{ data: User }>(`/admin/users/${id}/unlock`);

export const approveUser = (id: string) =>
  api.post<{ data: User }>(`/admin/users/${id}/approve`);

export const deleteUser = (id: string) =>
  api.delete(`/admin/users/${id}`);

export const getUser = (id: string) =>
  api.get<{ data: User }>(`/admin/users/${id}`);

export const createUser = (data: Partial<User> & { password?: string }) =>
  api.post<{ data: User }>('/admin/users', data);

export const updateUser = (id: string, data: Partial<User>) =>
  api.put<{ data: User }>(`/admin/users/${id}`, data);

export const resetUserPassword = (id: string, newPassword: string) =>
  api.post(`/admin/users/${id}/reset-password`, { newPassword });
