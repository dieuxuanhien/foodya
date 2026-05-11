import { api } from './client';
import type { PageMeta } from './users';

export interface Order {
  id: string;
  orderCode: string;
  status: string;
  paymentStatus: string;
  totalAmount: number;
  restaurantId?: string;
  customerUserId?: string;
  deliveryAddress?: string;
  subtotalAmount?: number;
  deliveryFee?: number;
  paymentMethod?: string;
}

export const getOrders = (status?: string, page = 0, size = 20) =>
  api.get<{ data: Order[]; meta: PageMeta }>('/admin/orders', {
    params: { status: status || undefined, page, size },
  });

export const updateOrderStatus = (id: string, status: string) =>
  api.patch<{ data: Order }>(`/admin/orders/${id}/status`, { status });

export const deleteOrder = (id: string) =>
  api.delete(`/admin/orders/${id}`);
