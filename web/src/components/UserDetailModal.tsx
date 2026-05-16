import { formatDate, Spinner } from './ui';
import type { AdminUserDetail } from '../api/users';

interface Props {
  user?: AdminUserDetail | null;
  loading?: boolean;
  onClose: () => void;
}

export function UserDetailModal({ user, loading, onClose }: Props) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ minWidth: 420, maxWidth: 560 }}>
        <h2 className="modal-title">User Detail</h2>
        {loading ? (
          <div style={{ padding: '24px 0' }}>
            <div className="loading-center">
              <Spinner />
              <span>Loading user detail…</span>
            </div>
          </div>
        ) : user ? (
          <div style={{ display: 'grid', gap: 12, marginTop: 16 }}>
            <div>
              <div className="text-xs font-medium">Username</div>
              <div className="text-muted">{user.username}</div>
            </div>
            <div>
              <div className="text-xs font-medium">Full Name</div>
              <div className="text-muted">{user.fullName}</div>
            </div>
            <div>
              <div className="text-xs font-medium">Email</div>
              <div className="text-muted">{user.email}</div>
            </div>
            <div>
              <div className="text-xs font-medium">Phone</div>
              <div className="text-muted">{user.phoneNumber}</div>
            </div>
            <div style={{ display: 'flex', gap: 12 }}>
              <div style={{ flex: 1 }}>
                <div className="text-xs font-medium">Role</div>
                <div className="text-muted">{user.role}</div>
              </div>
              <div style={{ flex: 1 }}>
                <div className="text-xs font-medium">Status</div>
                <div className="text-muted">{user.status}</div>
              </div>
            </div>
            <div>
              <div className="text-xs font-medium">Avatar URL</div>
              <div className="text-muted break-all">{user.avatarUrl || '—'}</div>
            </div>
            <div style={{ display: 'flex', gap: 12 }}>
              <div style={{ flex: 1 }}>
                <div className="text-xs font-medium">Created</div>
                <div className="text-muted">{formatDate(user.createdAt)}</div>
              </div>
              <div style={{ flex: 1 }}>
                <div className="text-xs font-medium">Updated</div>
                <div className="text-muted">{formatDate(user.updatedAt)}</div>
              </div>
            </div>
          </div>
        ) : (
          <div style={{ padding: '12px 0', color: 'var(--text-secondary)' }}>User not found.</div>
        )}
        <div className="modal-footer" style={{ marginTop: 20 }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
