import { api } from './client';
import type { PageMeta } from './users';

export interface MenuCategory {
  id: string;
  restaurantId: string;
  name: string;
  sortOrder: number;
  active: boolean;
}

export const getRestaurantMenuCategories = (restaurantId: string, page = 0, size = 100) =>
  api.get<{ data: MenuCategory[]; meta: PageMeta }>(`/admin/restaurants/${restaurantId}/menu-categories`, {
    params: { page, size },
  });
