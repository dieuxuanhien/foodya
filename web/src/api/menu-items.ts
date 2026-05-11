import { api } from './client';
import type { PageMeta } from './users';

export interface MenuItem {
  id: string;
  restaurantId: string;
  categoryCode: string;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  isAvailable: boolean;
  createdAt?: string;
}

export const getMenuItems = (restaurantId?: string, categoryCode?: string, page = 0, size = 50) =>
  api.get<{ data: MenuItem[]; meta: PageMeta }>('/admin/menu-items', {
    params: { restaurantId, categoryCode, page, size },
  });

export const deleteMenuItem = (id: string) =>
  api.delete(`/admin/menu-items/${id}`);

export const createMenuItem = (data: {
  restaurantId: string;
  categoryCode: string;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
}) =>
  api.post<{ data: MenuItem }>('/admin/menu-items', data);

export const updateMenuItem = (id: string, data: Partial<MenuItem>) =>
  api.put<{ data: MenuItem }>(`/admin/menu-items/${id}`, data);
