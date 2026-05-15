import { api } from './client';
import type { PageMeta } from './users';

export interface Order {
  orderId: string;
  orderCode: string;
  status: string;
  paymentStatus: string;
  totalAmount: number;
  restaurantId?: string;
  restaurantName?: string;
  customerUserId?: string;
  customerName?: string;
  deliveryAddress?: string;
  subtotalAmount?: number;
  deliveryFee?: number;
  paymentMethod?: string;
}

export const getOrders = (status?: string, page = 0, size = 20) =>
  api.get<{ data: Order[]; meta: PageMeta }>('/admin/orders', {
    params: { status: status || undefined, page, size },
  });

export const getOrderDetails = (id: string) =>
  api.get<{ data: Order }>(`/admin/orders/${id}`);

export const updateOrderStatus = (id: string, status: string) =>
  api.patch<{ data: Order }>(`/admin/orders/${id}/status`, { status });

export const deleteOrder = (id: string) =>
  api.delete(`/admin/orders/${id}`);
