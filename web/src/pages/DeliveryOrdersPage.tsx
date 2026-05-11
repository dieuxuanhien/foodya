import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MapPin, Clock } from 'lucide-react';
import {
  getDeliveryOrders,
  updateDeliveryStatus,
  type DeliveryOrder,
} from '../api/orders-delivery';
import { PageHeader, LoadingCenter, EmptyState, StatusBadge, Pagination, formatDate } from '../components/ui';

const STATUS_OPTIONS = ['', 'PENDING', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED'];

export default function DeliveryOrdersPage() {
  const qc = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState<DeliveryOrder | null>(null);
  const [editStatus, setEditStatus] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-delivery-orders', statusFilter, page],
    queryFn: () => getDeliveryOrders(statusFilter || undefined, page, 20),
  });

  const mutStatus = useMutation({
    mutationFn: (params: { orderId: string; status: string }) =>
      updateDeliveryStatus(params.orderId, params.status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-delivery-orders'] });
      setSelectedOrder(null);
    },
  });

  const orders = data?.data?.data ?? [];
  const meta = data?.data?.meta;

  const handleUpdateStatus = () => {
    if (selectedOrder && editStatus) {
      mutStatus.mutate({ orderId: selectedOrder.id, status: editStatus });
    }
  };

  return (
    <div className="page-content">
      <PageHeader
        title="Delivery Order Tracking"
        subtitle="Monitor and manage delivery status for all orders"
      />

      <div className="card">
        <div className="toolbar">
          <select
            className="select"
            style={{ maxWidth: 200 }}
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          >
            <option value="">All Statuses</option>
            {STATUS_OPTIONS.slice(1).map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>

        {isLoading ? (
          <LoadingCenter />
        ) : orders.length === 0 ? (
          <EmptyState title="No delivery orders found" desc="Try changing the status filter" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Order Code</th>
                  <th>Customer</th>
                  <th>Status</th>
                  <th>Delivery Address</th>
                  <th>Est. Delivery</th>
                  <th>Location</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr
                    key={o.id}
                    onClick={() => { setSelectedOrder(o); setEditStatus(o.status); }}
                    style={{ cursor: 'pointer' }}
                  >
                    <td>
                      <span className="font-mono">{o.orderCode}</span>
                    </td>
                    <td>{o.customerName}</td>
                    <td>
                      <StatusBadge status={o.status} />
                    </td>
                    <td className="text-muted text-sm truncate">{o.deliveryAddress}</td>
                    <td className="text-sm">
                      {o.estimatedDeliveryTime ? (
                        <>
                          <Clock size={12} style={{ display: 'inline', marginRight: 4 }} />
                          {formatDate(o.estimatedDeliveryTime)}
                        </>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="text-sm">
                      {o.latitude && o.longitude ? (
                        <>
                          <MapPin size={12} style={{ display: 'inline', marginRight: 4 }} />
                          {o.latitude.toFixed(4)}, {o.longitude.toFixed(4)}
                        </>
                      ) : (
                        '—'
                      )}
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
            <h2 className="modal-title">Update Delivery Status</h2>
           
            <div className="form-group">
              <label className="form-label">Order</label>
              <div className="text-muted">{selectedOrder.orderCode} - {selectedOrder.customerName}</div>
            </div>

            <div className="form-group">
              <label className="form-label">Delivery Address</label>
              <div className="text-muted text-sm">{selectedOrder.deliveryAddress}</div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="delivery-status">
                New Status
              </label>
              <select
                id="delivery-status"
                className="select"
                value={editStatus}
                onChange={(e) => setEditStatus(e.target.value)}
              >
                {STATUS_OPTIONS.slice(1).map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>

            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setSelectedOrder(null)}>
                Close
              </button>
              <button className="btn btn-primary" onClick={handleUpdateStatus} disabled={mutStatus.isPending}>
                {mutStatus.isPending ? 'Updating...' : 'Update Status'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
