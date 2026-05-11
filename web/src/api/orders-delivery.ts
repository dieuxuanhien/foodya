import { api } from './client';
import type { PageMeta } from './users';

export interface DeliveryOrder {
  id: string;
  orderId: string;
  orderCode: string;
  customerName: string;
  deliveryAddress: string;
  status: string;
  estimatedDeliveryTime?: string;
  actualDeliveryTime?: string;
  latitude?: number;
  longitude?: number;
  createdAt?: string;
}

export const getDeliveryOrders = (status?: string, page = 0, size = 20) =>
  api.get<{ data: DeliveryOrder[]; meta: PageMeta }>('/delivery/orders', {
    params: { status, page, size },
  });

export const updateDeliveryStatus = (orderId: string, status: string) =>
  api.patch<{ data: DeliveryOrder }>(`/delivery/orders/${orderId}`, { status });

export const updateDeliveryLocation = (orderId: string, latitude: number, longitude: number) =>
  api.patch<{ data: DeliveryOrder }>(`/delivery/orders/${orderId}/location`, { latitude, longitude });
