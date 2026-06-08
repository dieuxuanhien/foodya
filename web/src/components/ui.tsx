import { formatVndCurrency } from '../utils/currency';

export function Spinner() {
  return <span className="spinner" />;
}

export function LoadingCenter({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loading-center">
      <Spinner />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ title, desc }: { title: string; desc?: string }) {
  return (
    <div className="empty-state">
      <h3>{title}</h3>
      {desc && <p>{desc}</p>}
    </div>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const cls = statusClass(status);
  return <span className={`badge ${cls}`}>{status}</span>;
}

function statusClass(s: string) {
  const up = s.toUpperCase();
  if (['ACTIVE', 'SUCCESS', 'APPROVED', 'PAID', 'SENT'].includes(up)) return 'badge-success';
  if (['PENDING', 'UNPAID', 'PENDING_APPROVAL'].includes(up)) return 'badge-warning';
  if (['LOCKED', 'REJECTED', 'CANCELLED', 'FAILED', 'DELETED'].includes(up)) return 'badge-danger';
  if (['DELIVERING', 'ASSIGNED', 'ACCEPTED', 'PREPARING'].includes(up)) return 'badge-info';
  if (['INACTIVE', 'OPEN'].includes(up)) return 'badge-accent';
  return 'badge-muted';
}

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (p: number) => void;
}

export function Pagination({ page, totalPages, totalElements, size, onPage }: PaginationProps) {
  const start = page * size + 1;
  const end = Math.min((page + 1) * size, totalElements);
  return (
    <div className="pagination">
      <span>{totalElements > 0 ? `${start}–${end} of ${totalElements}` : 'No results'}</span>
      <div className="pagination-buttons">
        <button className="btn btn-ghost btn-sm" onClick={() => onPage(0)} disabled={page === 0}>«</button>
        <button className="btn btn-ghost btn-sm" onClick={() => onPage(page - 1)} disabled={page === 0}>‹</button>
        <span className="btn btn-ghost btn-sm" style={{ cursor: 'default' }}>{page + 1} / {Math.max(1, totalPages)}</span>
        <button className="btn btn-ghost btn-sm" onClick={() => onPage(page + 1)} disabled={page >= totalPages - 1}>›</button>
        <button className="btn btn-ghost btn-sm" onClick={() => onPage(totalPages - 1)} disabled={page >= totalPages - 1}>»</button>
      </div>
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 20 }}>
      <div>
        <h1 className="section-title">{title}</h1>
        {subtitle && <p className="section-sub">{subtitle}</p>}
      </div>
      {actions && <div style={{ display: 'flex', gap: 8 }}>{actions}</div>}
    </div>
  );
}

export function ConfirmModal({
  title, message, onConfirm, onCancel, loading, danger,
}: {
  title: string; message: string;
  onConfirm: () => void; onCancel: () => void;
  loading?: boolean; danger?: boolean;
}) {
  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2 className="modal-title">{title}</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{message}</p>
        <div className="modal-footer">
          <button className="btn btn-ghost" onClick={onCancel}>Cancel</button>
          <button
            className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
            onClick={onConfirm} disabled={loading}
          >
            {loading ? <Spinner /> : 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  );
}

export function formatCurrency(amount: number, _code = 'VND') {
  return formatVndCurrency(amount);
}

export function formatDate(dt?: string) {
  if (!dt) return '—';
  return new Date(dt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}
