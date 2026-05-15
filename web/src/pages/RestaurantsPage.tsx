import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, CheckCircle, XCircle, Trash2, Edit2, Plus, UtensilsCrossed } from 'lucide-react';
import {
  getRestaurants, approveRestaurant, rejectRestaurant, deleteRestaurant, createRestaurant, updateRestaurant,
  type Restaurant,
} from '../api/restaurants';
import { PageHeader, LoadingCenter, EmptyState, StatusBadge, Pagination, ConfirmModal } from '../components/ui';
import { RestaurantFormModal } from '../components/RestaurantFormModal';

const STATUS_OPTIONS = ['', 'PENDING', 'APPROVED', 'REJECTED', 'ACTIVE', 'INACTIVE'];
type Action = { type: 'approve' | 'reject' | 'delete'; rest: Restaurant };

export default function RestaurantsPage() {
  const qc = useQueryClient();
  const nav = useNavigate();
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<Action | null>(null);
  const [showFormModal, setShowFormModal] = useState(false);
  const [selectedRestaurant, setSelectedRestaurant] = useState<Restaurant | undefined>(undefined);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-restaurants', search, statusFilter, page],
    queryFn: () => getRestaurants(search || undefined, statusFilter || undefined, page, 20),
  });

  const mutApprove = useMutation({ mutationFn: (id: string) => approveRestaurant(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-restaurants'] }); setAction(null); } });
  const mutReject = useMutation({ mutationFn: (id: string) => rejectRestaurant(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-restaurants'] }); setAction(null); } });
  const mutDelete = useMutation({ mutationFn: (id: string) => deleteRestaurant(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-restaurants'] }); setAction(null); } });
  const mutCreate = useMutation({ mutationFn: createRestaurant, onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-restaurants'] }); setShowFormModal(false); } });
  const mutUpdate = useMutation({ mutationFn: (vars: { id: string, data: Partial<Restaurant> }) => updateRestaurant(vars.id, vars.data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-restaurants'] }); setShowFormModal(false); } });

  const restaurants = data?.data?.data ?? [];
  const meta = data?.data?.meta;
  const isMutating = mutApprove.isPending || mutReject.isPending || mutDelete.isPending;

  const handleConfirm = () => {
    if (!action) return;
    if (action.type === 'approve') mutApprove.mutate(action.rest.id);
    else if (action.type === 'reject') mutReject.mutate(action.rest.id);
    else mutDelete.mutate(action.rest.id);
  };

  return (
    <div className="page-content">
      <PageHeader 
        title="Restaurant Governance" 
        subtitle="Create, edit, approve, reject, and manage restaurant registrations" 
        actions={
          <button className="btn btn-primary" onClick={() => { setSelectedRestaurant(undefined); setShowFormModal(true); }}>
            <Plus size={16} /> New Restaurant
          </button>
        }
      />

      <div className="card">
        <div className="toolbar">
          <div style={{ position: 'relative', flex: 1, maxWidth: 280 }}>
            <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              id="restaurants-search"
              className="input"
              style={{ paddingLeft: 32 }}
              placeholder="Search restaurant name…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') { setSearch(q); setPage(0); } }}
            />
          </div>
          <select
            id="restaurants-status-filter"
            className="select"
            style={{ maxWidth: 160 }}
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          >
            {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s || 'All Statuses'}</option>)}
          </select>
          <button className="btn btn-ghost" onClick={() => { setSearch(q); setPage(0); }}>Search</button>
        </div>

        {isLoading ? <LoadingCenter /> : restaurants.length === 0 ? (
          <EmptyState title="No restaurants found" desc="Try changing filters" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Cuisine</th>
                  <th>Status</th>
                  <th>Open</th>
                  <th>Rating</th>
                  <th>Address</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {restaurants.map((r) => (
                  <tr key={r.id}>
                    <td style={{ fontWeight: 500 }}>{r.name}</td>
                    <td className="text-muted">{r.cuisineType}</td>
                    <td><StatusBadge status={r.status} /></td>
                    <td>{r.open ? <span className="badge badge-success">Open</span> : <span className="badge badge-muted">Closed</span>}</td>
                    <td>{r.avgRating ? `★ ${r.avgRating.toFixed(1)}` : '—'}</td>
                    <td className="truncate text-muted text-sm">{r.addressLine}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button className="btn btn-ghost btn-sm btn-icon" title="Manage Menu" onClick={() => nav(`/menu-items?restaurantId=${r.id}`)}>
                          <UtensilsCrossed size={13} />
                        </button>
                        <button className="btn btn-ghost btn-sm btn-icon" title="Edit" onClick={() => { setSelectedRestaurant(r); setShowFormModal(true); }}>
                          <Edit2 size={13} />
                        </button>
                        {(r.status === 'PENDING' || r.status === 'PENDING_APPROVAL') && (
                          <>
                            <button id={`approve-${r.id}`} className="btn btn-success btn-sm btn-icon" title="Approve" onClick={() => setAction({ type: 'approve', rest: r })}>
                              <CheckCircle size={13} />
                            </button>
                            <button id={`reject-${r.id}`} className="btn btn-danger btn-sm btn-icon" title="Reject" onClick={() => setAction({ type: 'reject', rest: r })}>
                              <XCircle size={13} />
                            </button>
                          </>
                        )}
                        <button id={`delete-rest-${r.id}`} className="btn btn-danger btn-sm btn-icon" title="Delete" onClick={() => setAction({ type: 'delete', rest: r })}>
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

      {action && (
        <ConfirmModal
          title={action.type === 'delete' ? 'Delete Restaurant' : action.type === 'approve' ? 'Approve Restaurant' : 'Reject Restaurant'}
          message={
            action.type === 'delete'
              ? `Permanently delete "${action.rest.name}"? This cannot be undone.`
              : action.type === 'approve'
              ? `Approve restaurant "${action.rest.name}"? It will become visible to customers.`
              : `Reject restaurant "${action.rest.name}"?`
          }
          onConfirm={handleConfirm}
          onCancel={() => setAction(null)}
          loading={isMutating}
          danger={action.type === 'delete' || action.type === 'reject'}
        />
      )}

      {showFormModal && (
        <RestaurantFormModal 
          restaurant={selectedRestaurant} 
          onClose={() => setShowFormModal(false)}
          onSubmit={(data) => selectedRestaurant ? mutUpdate.mutate({ id: selectedRestaurant.id, data }) : mutCreate.mutate(data)}
          loading={mutCreate.isPending || mutUpdate.isPending}
        />
      )}
    </div>
  );
}
