import { api } from './client';
import type { PageMeta } from './users';

export interface Notification {
  id: string;
  receiverUserId: string;
  receiverType: string;
  eventType: string;
  title: string;
  message: string;
  status: string;
  orderId?: string;
  sentAt?: string;
  readAt?: string;
  createdAt: string;
}

export const getNotifications = (page = 0, size = 20) =>
  api.get<{ data: Notification[]; meta: PageMeta }>('/admin/notifications', {
    params: { page, size },
  });
