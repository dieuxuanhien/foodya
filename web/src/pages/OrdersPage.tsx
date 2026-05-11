import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Edit2, Trash2 } from 'lucide-react';
import { getOrders, updateOrderStatus, deleteOrder, type Order } from '../api/orders';
import { PageHeader, LoadingCenter, EmptyState, StatusBadge, Pagination, ConfirmModal, formatCurrency } from '../components/ui';

const STATUS_OPTIONS = ['', 'PENDING', 'ACCEPTED', 'PREPARING', 'DELIVERING', 'DELIVERED', 'CANCELLED'];

export default function OrdersPage() {
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [editStatus, setEditStatus] = useState('');
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-orders', statusFilter, page],
    queryFn: () => getOrders(statusFilter || undefined, page, 20),
  });

  const mutStatus = useMutation({ mutationFn: (params: { id: string, status: string }) => updateOrderStatus(params.id, params.status), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-orders'] }); setSelectedOrder(null); } });
  const mutDelete = useMutation({ mutationFn: (id: string) => deleteOrder(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-orders'] }); setDeleteId(null); } });

  const orders = data?.data?.data ?? [];
  const meta = data?.data?.meta;

  const handleUpdateStatus = () => {
    if (selectedOrder && editStatus) {
      mutStatus.mutate({ id: selectedOrder.id, status: editStatus });
    }
  };

  return (
    <div className="page-content">
      <PageHeader title="Order Governance" subtitle="Monitor and manage customer orders across the platform" />

      <div className="card">
        <div className="toolbar">
          <select
            id="orders-status-filter"
            className="select"
            style={{ maxWidth: 200 }}
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          >
            <option value="">All Statuses</option>
            {STATUS_OPTIONS.slice(1).map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>

        {isLoading ? <LoadingCenter /> : orders.length === 0 ? (
          <EmptyState title="No orders found" desc="Try changing the status filter" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Order Code</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Payment</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td><span className="font-mono">{o.orderCode}</span></td>
                    <td style={{ fontWeight: 600 }}>{formatCurrency(o.totalAmount)}</td>
                    <td><StatusBadge status={o.status} /></td>
                    <td><StatusBadge status={o.paymentStatus} /></td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button
                          id={`edit-status-${o.id}`}
                          className="btn btn-ghost btn-sm btn-icon"
                          title="Update Status"
                          onClick={() => { setSelectedOrder(o); setEditStatus(o.status); }}
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          id={`delete-order-${o.id}`}
                          className="btn btn-danger btn-sm btn-icon"
                          title="Delete Order"
                          onClick={() => setDeleteId(o.id)}
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {meta && <Pagination page={page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />}
      </div>

      {selectedOrder && (
        <div className="modal-backdrop" onClick={() => setSelectedOrder(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title">Update Order Status</h2>
            <div className="form-group">
              <label className="form-label">Order Code</label>
              <div className="font-mono text-muted">{selectedOrder.orderCode}</div>
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="new-status-select">New Status</label>
              <select id="new-status-select" className="select" value={editStatus} onChange={(e) => setEditStatus(e.target.value)}>
                {STATUS_OPTIONS.slice(1).map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setSelectedOrder(null)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleUpdateStatus} disabled={mutStatus.isPending}>
                {mutStatus.isPending ? 'Updating…' : 'Save Changes'}
              </button>
            </div>
          </div>
        </div>
      )}

      {deleteId && (
        <ConfirmModal
          title="Delete Order"
          message="Are you sure you want to delete this order? This action cannot be undone."
          onConfirm={() => mutDelete.mutate(deleteId)}
          onCancel={() => setDeleteId(null)}
          loading={mutDelete.isPending}
          danger
        />
      )}
    </div>
  );
}
