import { api } from './client';
import type { PageMeta } from './users';

export interface Restaurant {
  id: string;
  name: string;
  description?: string;
  cuisineType: string;
  status: string;
  open: boolean;
  avgRating: number;
  reviewCount: number;
  addressLine: string;
  ownerUserId: string;
  createdAt?: string;
}

export const getRestaurants = (q?: string, status?: string, page = 0, size = 20) =>
  api.get<{ data: Restaurant[]; meta: PageMeta }>('/admin/restaurants', {
    params: { q, status: status || undefined, page, size },
  });

export const approveRestaurant = (id: string) =>
  api.post<{ data: Restaurant }>(`/admin/restaurants/${id}/approve`);

export const rejectRestaurant = (id: string) =>
  api.post<{ data: Restaurant }>(`/admin/restaurants/${id}/reject`);

export const deleteRestaurant = (id: string) =>
  api.delete(`/admin/restaurants/${id}`);

export const hardDeleteMenuItem = (id: string) =>
  api.delete(`/admin/menu-items/${id}`);

export const getRestaurant = (id: string) =>
  api.get<{ data: Restaurant }>(`/admin/restaurants/${id}`);

export const createRestaurant = (data: Partial<Restaurant>) =>
  api.post<{ data: Restaurant }>('/admin/restaurants', data);

export const updateRestaurant = (id: string, data: Partial<Restaurant>) =>
  api.put<{ data: Restaurant }>(`/admin/restaurants/${id}`, data);
