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
  isActive?: boolean;
  createdAt?: string;
}

interface RawMenuItem {
  id: string;
  restaurantId: string;
  categoryId?: string;
  taxonomyCodes?: string[];
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  active?: boolean;
  available?: boolean;
}

const normalizeMenuItem = (raw: RawMenuItem): MenuItem => ({
  id: raw.id,
  restaurantId: raw.restaurantId,
  categoryCode: raw.categoryId ?? raw.taxonomyCodes?.[0] ?? '—',
  name: raw.name,
  description: raw.description,
  price: raw.price,
  imageUrl: raw.imageUrl,
  isAvailable: Boolean(raw.available),
  isActive: raw.active,
});

export const getMenuItems = (restaurantId?: string, categoryCode?: string, q?: string, page = 0, size = 50) =>
  api
    .get<{ data: RawMenuItem[]; meta: PageMeta }>(`/admin/menu-items`, {
      params: {
        restaurantId: restaurantId || undefined,
        categoryCode: categoryCode || undefined,
        q: q || undefined,
        page,
        size,
      },
    })
    .then((res) => ({
      ...res,
      data: {
        ...res.data,
        data: (res.data?.data ?? []).map(normalizeMenuItem),
      },
    }));

export const deleteMenuItem = (id: string) =>
  api.delete(`/admin/menu-items/${id}`);
