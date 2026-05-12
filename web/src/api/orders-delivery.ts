import { api } from './client';

export interface DeliveryOrder {
  id: string;
  orderCode: string;
  status: string;
  paymentStatus: string;
  totalAmount: number;
}

interface RawDeliveryOrder {
  orderId: string;
  orderCode: string;
  status: string;
  paymentStatus: string;
  totalAmount: number;
}

const normalizeDeliveryOrder = (raw: RawDeliveryOrder): DeliveryOrder => ({
  id: raw.orderId,
  orderCode: raw.orderCode,
  status: raw.status,
  paymentStatus: raw.paymentStatus,
  totalAmount: raw.totalAmount,
});

export const getDeliveryOrders = () =>
  api.get<{ data: RawDeliveryOrder[] }>('/delivery/orders/assignments').then((res) => ({
    ...res,
    data: {
      ...res.data,
      data: (res.data?.data ?? []).map(normalizeDeliveryOrder),
    },
  }));

export const updateDeliveryStatus = (orderId: string, status: string) =>
  api.patch<{ data: DeliveryOrder }>(`/delivery/orders/${orderId}/status`, { status }).then((res) => ({
    ...res,
    data: {
      ...res.data,
      data: {
        ...res.data.data,
        id: (res.data.data as unknown as RawDeliveryOrder).orderId ?? orderId,
      },
    },
  }));

export const updateDeliveryLocation = (orderId: string, latitude: number, longitude: number, recordedAt?: string) =>
  api.post(`/delivery/orders/${orderId}/tracking-points`, {
    lat: latitude,
    lng: longitude,
    recordedAt,
  });
