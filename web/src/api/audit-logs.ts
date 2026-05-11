import { api } from './client';
import type { PageMeta } from './users';

export interface AuditLog {
  id: string;
  actorId: string;
  eventType: string;
  entityType: string;
  entityId: string;
  oldValue?: string;
  newValue?: string;
  context?: string;
  createdAt: string;
}

export const getAuditLogs = (eventType?: string, entityType?: string, page = 0, size = 50) =>
  api.get<{ data: AuditLog[]; meta: PageMeta }>('/admin/audit-logs', {
    params: { eventType, entityType, page, size },
  });
