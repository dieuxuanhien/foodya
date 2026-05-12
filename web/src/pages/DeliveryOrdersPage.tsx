import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Truck } from 'lucide-react';
import {
  getDeliveryOrders,
  updateDeliveryStatus,
  type DeliveryOrder,
} from '../api/orders-delivery';
import { PageHeader, LoadingCenter, EmptyState, StatusBadge, formatCurrency } from '../components/ui';

const STATUS_OPTIONS = ['ASSIGNED', 'PREPARING', 'DELIVERING', 'SUCCESS', 'FAILED'];

export default function DeliveryOrdersPage() {
  const qc = useQueryClient();
  const [selectedOrder, setSelectedOrder] = useState<DeliveryOrder | null>(null);
  const [editStatus, setEditStatus] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-delivery-orders'],
    queryFn: getDeliveryOrders,
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

  const handleUpdateStatus = () => {
    if (selectedOrder && editStatus) {
      mutStatus.mutate({ orderId: selectedOrder.id, status: editStatus });
    }
  };

  return (
    <div className="page-content">
      <PageHeader
        title="Delivery Order Tracking"
        subtitle="Monitor delivery assignments and update lifecycle status"
      />

      <div className="card">
        {isLoading ? (
          <LoadingCenter />
        ) : orders.length === 0 ? (
          <EmptyState title="No delivery assignments found" desc="Assignments appear when orders move into delivery flow" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Order Code</th>
                  <th>Status</th>
                  <th>Payment</th>
                  <th>Total</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td>
                      <span className="font-mono">{o.orderCode}</span>
                    </td>
                    <td>
                      <StatusBadge status={o.status} />
                    </td>
                    <td>
                      <StatusBadge status={o.paymentStatus} />
                    </td>
                    <td style={{ fontWeight: 600 }}>
                      {formatCurrency(o.totalAmount)}
                    </td>
                    <td>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => { setSelectedOrder(o); setEditStatus(o.status); }}
                      >
                        <Truck size={13} /> Update
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedOrder && (
        <div className="modal-backdrop" onClick={() => setSelectedOrder(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title">Update Delivery Status</h2>
           
            <div className="form-group">
              <label className="form-label">Order</label>
              <div className="text-muted">{selectedOrder.orderCode}</div>
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
                {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
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
