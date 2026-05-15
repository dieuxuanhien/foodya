import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Edit2, Trash2, Eye } from 'lucide-react';
import { getOrders, updateOrderStatus, deleteOrder, getOrderDetails, type Order } from '../api/orders';
import { PageHeader, LoadingCenter, EmptyState, StatusBadge, Pagination, ConfirmModal, formatCurrency, Spinner } from '../components/ui';

const STATUS_OPTIONS = ['', 'PENDING', 'ACCEPTED', 'PREPARING', 'DELIVERING', 'DELIVERED', 'CANCELLED'];

export default function OrdersPage() {
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [viewOrderId, setViewOrderId] = useState<string | null>(null);
  const [editStatus, setEditStatus] = useState('');
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-orders', statusFilter, page],
    queryFn: () => getOrders(statusFilter || undefined, page, 20),
  });

  const { data: detailData, isLoading: isDetailLoading } = useQuery({
    queryKey: ['admin-order-detail', viewOrderId],
    queryFn: () => getOrderDetails(viewOrderId!),
    enabled: !!viewOrderId
  });

  const mutStatus = useMutation({ 
    mutationFn: (params: { id: string, status: string }) => updateOrderStatus(params.id, params.status), 
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-orders'] }); setSelectedOrder(null); } 
  });
  
  const mutDelete = useMutation({ 
    mutationFn: (id: string) => deleteOrder(id), 
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-orders'] }); setDeleteId(null); } 
  });

  const orders = data?.data?.data ?? [];
  const meta = data?.data?.meta;
  const orderDetail = detailData?.data?.data;

  const handleUpdateStatus = () => {
    if (selectedOrder && editStatus) {
      mutStatus.mutate({ id: selectedOrder.orderId, status: editStatus });
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
                  <th>Customer</th>
                  <th>Restaurant</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Payment</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.orderId}>
                    <td><span className="font-mono">{o.orderCode}</span></td>
                    <td className="text-sm">{o.customerName || <span className="text-muted">{o.customerUserId?.substring(0, 8)}</span>}</td>
                    <td className="text-sm">{o.restaurantName || <span className="text-muted">{o.restaurantId?.substring(0, 8)}</span>}</td>
                    <td style={{ fontWeight: 600 }}>{formatCurrency(o.totalAmount)}</td>
                    <td><StatusBadge status={o.status} /></td>
                    <td><StatusBadge status={o.paymentStatus} /></td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button
                          className="btn btn-ghost btn-sm btn-icon"
                          title="View Details"
                          onClick={() => setViewOrderId(o.orderId)}
                        >
                          <Eye size={13} />
                        </button>
                        <button
                          id={`edit-status-${o.orderId}`}
                          className="btn btn-ghost btn-sm btn-icon"
                          title="Update Status"
                          onClick={() => { setSelectedOrder(o); setEditStatus(o.status); }}
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          id={`delete-order-${o.orderId}`}
                          className="btn btn-danger btn-sm btn-icon"
                          title="Delete Order"
                          onClick={() => setDeleteId(o.orderId)}
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

      {viewOrderId && (
        <div className="modal-backdrop" onClick={() => setViewOrderId(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ minWidth: 600 }}>
             <h2 className="modal-title">Order Details</h2>
             {isDetailLoading ? <LoadingCenter /> : orderDetail ? (
               <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Order Code</label>
                      <p className="font-mono">{orderDetail.orderCode}</p>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Status</label>
                      <div><StatusBadge status={orderDetail.status} /></div>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Customer</label>
                      <p>{orderDetail.customerName || orderDetail.customerUserId}</p>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Restaurant</label>
                      <p>{orderDetail.restaurantName || orderDetail.restaurantId}</p>
                    </div>
                  </div>

                  <hr style={{ border: 'none', borderTop: '1px solid var(--border-color)' }} />

                  <div>
                    <label className="text-xs font-medium text-muted uppercase">Delivery Address</label>
                    <p className="text-sm">{orderDetail.deliveryAddress}</p>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16 }}>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Subtotal</label>
                      <p>{formatCurrency(orderDetail.subtotalAmount || 0)}</p>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Delivery Fee</label>
                      <p>{formatCurrency(orderDetail.deliveryFee || 0)}</p>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Total</label>
                      <p style={{ fontWeight: 700, color: 'var(--primary-600)' }}>{formatCurrency(orderDetail.totalAmount)}</p>
                    </div>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Payment Method</label>
                      <p className="text-sm">{orderDetail.paymentMethod}</p>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-muted uppercase">Payment Status</label>
                      <div><StatusBadge status={orderDetail.paymentStatus} /></div>
                    </div>
                  </div>
               </div>
             ) : <p>Order not found</p>}
             <div className="modal-footer" style={{ marginTop: 24 }}>
                <button className="btn btn-primary" onClick={() => setViewOrderId(null)}>Close</button>
             </div>
          </div>
        </div>
      )}

      {selectedOrder && (
        <div className="modal-backdrop" onClick={() => setSelectedOrder(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title">Update Order Status</h2>
            <div className="form-group" style={{ marginTop: 16 }}>
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
