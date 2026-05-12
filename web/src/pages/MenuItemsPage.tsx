import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Trash2 } from 'lucide-react';
import {
  getMenuItems,
  deleteMenuItem,
  type MenuItem,
} from '../api/menu-items';
import { getCategories } from '../api/categories';
import { getRestaurants } from '../api/restaurants';
import { PageHeader, LoadingCenter, EmptyState, Pagination, ConfirmModal, formatCurrency } from '../components/ui';

type Action = { type: 'delete'; item: MenuItem };

export default function MenuItemsPage() {
  const qc = useQueryClient();
  const [q, setQ] = useState('');
  const [restaurantFilter, setRestaurantFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<Action | null>(null);

  const { data: restaurants } = useQuery({ queryKey: ['admin-restaurants'], queryFn: () => getRestaurants(undefined, undefined, 0, 500) });
  const { data: categories } = useQuery({ queryKey: ['admin-categories'], queryFn: () => getCategories() });

  const { data, isLoading } = useQuery({
    queryKey: ['admin-menu-items', restaurantFilter, categoryFilter, q, page],
    queryFn: () => getMenuItems(restaurantFilter || undefined, categoryFilter || undefined, q || undefined, page, 50),
  });

  const mutDelete = useMutation({
    mutationFn: (id: string) => deleteMenuItem(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-menu-items'] });
      setAction(null);
    },
  });

  const items = data?.data?.data ?? [];
  const meta = data?.data?.meta;
  const restList = restaurants?.data?.data ?? [];
  const catList = categories?.data?.data ?? [];
  const selectedRestaurantName = restList.find((r) => r.id === restaurantFilter)?.name;
  const restaurantNameById = new Map(restList.map((r) => [r.id, r.name]));

  return (
    <div className="page-content">
      <PageHeader
        title="Menu Items (Hard Delete)"
        subtitle="Inspect menu items across restaurants and permanently remove invalid items"
      />

      <div className="card">
        <div className="toolbar">
          <div style={{ position: 'relative', flex: 1, maxWidth: 250 }}>
            <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              className="input"
              style={{ paddingLeft: 32 }}
              placeholder="Search items…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
          </div>
          <select
            className="select"
            style={{ maxWidth: 180 }}
            value={restaurantFilter}
            onChange={(e) => { setRestaurantFilter(e.target.value); setPage(0); }}
          >
            <option value="">All Restaurants</option>
            {restList.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
          <select
            className="select"
            style={{ maxWidth: 180 }}
            value={categoryFilter}
            onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
          >
            <option value="">All Categories</option>
            {catList.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
          </select>
        </div>

        {isLoading ? (
          <LoadingCenter />
        ) : items.length === 0 ? (
          <EmptyState title="No menu items found" desc="Try changing filters" />
        ) : (
          <div className="table-wrap">
            {selectedRestaurantName && (
              <p className="text-muted text-sm" style={{ marginBottom: 10 }}>
                Showing menu for <strong>{selectedRestaurantName}</strong>
              </p>
            )}
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Restaurant</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Available</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td style={{ fontWeight: 500 }}>{item.name}</td>
                    <td className="text-muted text-sm">{restaurantNameById.get(item.restaurantId) ?? item.restaurantId}</td>
                    <td>
                      <span className="badge badge-info">{item.categoryCode}</span>
                    </td>
                    <td style={{ fontWeight: 600 }}>{formatCurrency(item.price)}</td>
                    <td>
                      <span className={`badge ${item.isAvailable ? 'badge-success' : 'badge-danger'}`}>
                        {item.isAvailable ? 'Yes' : 'No'}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn btn-danger btn-sm btn-icon"
                        title="Delete"
                        onClick={() => setAction({ type: 'delete', item })}
                      >
                        <Trash2 size={13} />
                      </button>
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
          title="Delete Menu Item"
          message={`Permanently delete "${action.item.name}"? This cannot be undone.`}
          onConfirm={() => mutDelete.mutate(action.item.id)}
          onCancel={() => setAction(null)}
          loading={mutDelete.isPending}
          danger
        />
      )}
    </div>
  );
}
