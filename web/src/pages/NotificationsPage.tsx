import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getNotifications } from '../api/notifications';
import { PageHeader, LoadingCenter, EmptyState, Pagination, StatusBadge, formatDate } from '../components/ui';

export default function NotificationsPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-notifications', page],
    queryFn: () => getNotifications(page, 20),
  });

  const notifs = data?.data?.data ?? [];
  const meta = data?.data?.meta;

  return (
    <div className="page-content">
      <PageHeader title="System Notifications" subtitle="Audit log of all notifications dispatched by the platform" />

      <div className="card">
        {isLoading ? <LoadingCenter /> : notifs.length === 0 ? (
          <EmptyState title="No notifications logged" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Receiver</th>
                  <th>Type</th>
                  <th>Event</th>
                  <th>Title & Message</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {notifs.map((n) => (
                  <tr key={n.id}>
                    <td className="text-muted text-sm" style={{ whiteSpace: 'nowrap' }}>{formatDate(n.createdAt)}</td>
                    <td className="font-mono text-sm">{n.receiverUserId}</td>
                    <td><span className="badge badge-muted">{n.receiverType}</span></td>
                    <td className="text-sm font-mono text-accent" style={{ color: 'var(--accent)' }}>{n.eventType}</td>
                    <td style={{ maxWidth: 300 }}>
                      <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 4 }}>{n.title}</div>
                      <div className="text-muted text-sm truncate">{n.message}</div>
                    </td>
                    <td><StatusBadge status={n.status} /></td>
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
