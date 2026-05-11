import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Filter } from 'lucide-react';
import { getAuditLogs } from '../api/audit-logs';
import { PageHeader, LoadingCenter, EmptyState, Pagination, formatDate } from '../components/ui';

const EVENT_TYPES = [
  '', 'ADMIN_USER_LOCK', 'ADMIN_USER_UNLOCK', 'ADMIN_USER_DELETE',
  'ADMIN_RESTAURANT_APPROVE', 'ADMIN_RESTAURANT_REJECT', 'ADMIN_RESTAURANT_DELETE',
  'ADMIN_MENU_ITEM_DELETE', 'ADMIN_ORDER_STATUS_UPDATE',
];

const ENTITY_TYPES = ['', 'USER', 'RESTAURANT', 'ORDER', 'MENU_ITEM', 'CATEGORY'];

export default function AuditLogsPage() {
  const [eventTypeFilter, setEventTypeFilter] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-audit-logs', eventTypeFilter, entityTypeFilter, page],
    queryFn: () => getAuditLogs(eventTypeFilter || undefined, entityTypeFilter || undefined, page, 50),
  });

  const logs = data?.data?.data ?? [];
  const meta = data?.data?.meta;

  return (
    <div className="page-content">
      <PageHeader
        title="Audit Logs"
        subtitle="Comprehensive security and governance audit trail"
      />

      <div className="card">
        <div className="toolbar" style={{ gap: 12, flexWrap: 'wrap' }}>
          <Filter size={16} style={{ color: 'var(--text-muted)' }} />

          <select
            className="select"
            style={{ flex: 1, minWidth: 200 }}
            value={eventTypeFilter}
            onChange={(e) => { setEventTypeFilter(e.target.value); setPage(0); }}
          >
            <option value="">All Event Types</option>
            {EVENT_TYPES.slice(1).map((et) => <option key={et} value={et}>{et}</option>)}
          </select>

          <select
            className="select"
            style={{ flex: 1, minWidth: 180 }}
            value={entityTypeFilter}
            onChange={(e) => { setEntityTypeFilter(e.target.value); setPage(0); }}
          >
            <option value="">All Entity Types</option>
            {ENTITY_TYPES.slice(1).map((entType) => <option key={entType} value={entType}>{entType}</option>)}
          </select>

          {(eventTypeFilter || entityTypeFilter) && (
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => { setEventTypeFilter(''); setEntityTypeFilter(''); setPage(0); }}
            >
              Clear
            </button>
          )}
        </div>

        {isLoading ? (
          <LoadingCenter />
        ) : logs.length === 0 ? (
          <EmptyState title="No audit logs found" desc="Try different filters" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Actor ID</th>
                  <th>Event Type</th>
                  <th>Entity Type</th>
                  <th>Entity ID</th>
                  <th>Changes</th>
                  <th>Context</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td className="text-muted text-sm" style={{ whiteSpace: 'nowrap' }}>
                      {formatDate(log.createdAt)}
                    </td>
                    <td className="font-mono text-sm">{log.actorId.substring(0, 8)}</td>
                    <td>
                      <span className="badge badge-warning">{log.eventType}</span>
                    </td>
                    <td>
                      <span className="badge badge-info">{log.entityType}</span>
                    </td>
                    <td className="font-mono text-sm">{log.entityId.substring(0, 12)}</td>
                    <td className="text-sm">
                      {log.oldValue || log.newValue ? (
                        <div style={{ maxWidth: 200, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                          {log.oldValue && <div style={{ color: 'var(--danger)' }}>- {log.oldValue}</div>}
                          {log.newValue && <div style={{ color: 'var(--success)' }}>+ {log.newValue}</div>}
                        </div>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="text-muted text-sm">{log.context || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {meta && <Pagination page={page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />}
      </div>
    </div>
  );
}
