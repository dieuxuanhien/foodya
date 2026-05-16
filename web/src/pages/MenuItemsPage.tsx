import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Edit2, Plus, Search, Trash2 } from 'lucide-react';
import {
  createMenuItem,
  deleteMenuItem,
  getMenuItems,
  updateMenuItem,
  type CreateMenuItemPayload,
  type MenuItem,
} from '../api/menu-items';
import { getCategories } from '../api/categories';
import { getRestaurants } from '../api/restaurants';
import { getRestaurantMenuCategories } from '../api/menu-categories';
import { MenuItemFormModal } from '../components/MenuItemFormModal';
import { PageHeader, LoadingCenter, EmptyState, Pagination, ConfirmModal, formatCurrency } from '../components/ui';

type Action = { type: 'delete'; item: MenuItem };

export default function MenuItemsPage() {
  const qc = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [restaurantFilter, setRestaurantFilter] = useState('');
  const [taxonomyFilter, setTaxonomyFilter] = useState('');
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<Action | null>(null);
  const [editingItem, setEditingItem] = useState<MenuItem | null>(null);
  const [showFormModal, setShowFormModal] = useState(false);

  useEffect(() => {
    const restaurantId = searchParams.get('restaurantId') ?? '';
    setRestaurantFilter(restaurantId);
  }, [searchParams]);

  const { data: restaurants } = useQuery({ queryKey: ['admin-restaurants'], queryFn: () => getRestaurants(undefined, undefined, 0, 100) });
  const { data: taxonomies } = useQuery({ queryKey: ['admin-taxonomies'], queryFn: () => getCategories() });
  const { data: menuCategories } = useQuery({
    queryKey: ['admin-restaurant-menu-categories', restaurantFilter],
    queryFn: () => getRestaurantMenuCategories(restaurantFilter),
    enabled: Boolean(restaurantFilter),
  });

  const { data, isLoading } = useQuery({
    queryKey: ['admin-menu-items', restaurantFilter, taxonomyFilter, search, page],
    queryFn: () => getMenuItems(restaurantFilter || undefined, taxonomyFilter || undefined, search || undefined, page, 50),
  });

  const mutDelete = useMutation({
    mutationFn: (id: string) => deleteMenuItem(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-menu-items'] });
      setAction(null);
    },
  });

  const mutCreate = useMutation({
    mutationFn: (vars: { restaurantId: string; data: CreateMenuItemPayload }) => createMenuItem(vars.restaurantId, vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-menu-items'] });
      setShowFormModal(false);
      setEditingItem(null);
    },
  });

  const mutUpdate = useMutation({
    mutationFn: (vars: { id: string; data: CreateMenuItemPayload }) => updateMenuItem(vars.id, vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-menu-items'] });
      setShowFormModal(false);
      setEditingItem(null);
    },
  });

  const items = data?.data?.data ?? [];
  const meta = data?.data?.meta;
  const restList = restaurants?.data?.data ?? [];
  const taxonomyList = taxonomies?.data?.data ?? [];
  const categoryList = menuCategories?.data?.data ?? [];
  const selectedRestaurant = useMemo(() => restList.find((r) => r.id === restaurantFilter), [restList, restaurantFilter]);
  const restaurantNameById = useMemo(() => new Map(restList.map((r) => [r.id, r.name] as const)), [restList]);
  const categoryNameById = useMemo(() => new Map(categoryList.map((c) => [c.id, c.name] as const)), [categoryList]);
  const taxonomyNameByCode = useMemo(() => new Map(taxonomyList.map((c) => [c.code, c.name] as const)), [taxonomyList]);

  const handleRestaurantChange = (nextRestaurantId: string) => {
    setRestaurantFilter(nextRestaurantId);
    setPage(0);
    const next = new URLSearchParams(searchParams);
    if (nextRestaurantId) next.set('restaurantId', nextRestaurantId);
    else next.delete('restaurantId');
    setSearchParams(next, { replace: true });
  };

  const handleSearch = () => {
    setSearch(q);
    setPage(0);
  };

  const openCreate = () => {
    if (!restaurantFilter) return;
    setEditingItem(null);
    setShowFormModal(true);
  };

  const openEdit = (item: MenuItem) => {
    setEditingItem(item);
    handleRestaurantChange(item.restaurantId);
    setShowFormModal(true);
  };

  const submitMenuItem = (data: CreateMenuItemPayload) => {
    if (editingItem) {
      mutUpdate.mutate({ id: editingItem.id, data });
      return;
    }
    if (!restaurantFilter) return;
    mutCreate.mutate({ restaurantId: restaurantFilter, data });
  };

  return (
    <div className="page-content">
      <PageHeader
        title={selectedRestaurant ? `${selectedRestaurant.name} Menu` : 'Menu Items'}
        subtitle={selectedRestaurant ? 'Create, update, and remove menu items for the selected restaurant' : 'Inspect and manage menu items across restaurants'}
        actions={
          <button className="btn btn-primary" onClick={openCreate} disabled={!restaurantFilter}>
            <Plus size={16} /> New Menu Item
          </button>
        }
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
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleSearch();
                }
              }}
            />
          </div>
          <select
            className="select"
            style={{ maxWidth: 240 }}
            value={restaurantFilter}
            onChange={(e) => handleRestaurantChange(e.target.value)}
          >
            <option value="">All Restaurants</option>
            {restList.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
          <select
            className="select"
            style={{ maxWidth: 240 }}
            value={taxonomyFilter}
            onChange={(e) => {
              setTaxonomyFilter(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Taxonomies</option>
            {taxonomyList.map((c) => (
              <option key={c.code} value={c.code}>
                {c.name}
              </option>
            ))}
          </select>
          <button className="btn btn-ghost" onClick={handleSearch}>
            Search
          </button>
        </div>

        {isLoading ? (
          <LoadingCenter />
        ) : items.length === 0 ? (
          <EmptyState title="No menu items found" desc="Try changing filters" />
        ) : (
          <div className="table-wrap">
            {selectedRestaurant && (
              <p className="text-muted text-sm" style={{ marginBottom: 10 }}>
                Showing menu for <strong>{selectedRestaurant.name}</strong>
              </p>
            )}
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Restaurant</th>
                  <th>Category</th>
                  <th>Taxonomies</th>
                  <th>Price</th>
                  <th>Available</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td style={{ fontWeight: 500 }}>{item.name}</td>
                    <td className="text-muted text-sm">{item.restaurantName || item.restaurantId}</td>
                    <td className="text-sm">{item.categoryName || item.categoryId || '—'}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                        {item.taxonomyCodes.map((code) => (
                          <span key={code} className="badge badge-info">
                            {taxonomyNameByCode.get(code) ?? code}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td style={{ fontWeight: 600 }}>{formatCurrency(item.price)}</td>
                    <td>
                      <span className={`badge ${item.isAvailable ? 'badge-success' : 'badge-danger'}`}>
                        {item.isAvailable ? 'Yes' : 'No'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button className="btn btn-ghost btn-sm btn-icon" title="Edit" onClick={() => openEdit(item)}>
                          <Edit2 size={13} />
                        </button>
                        <button
                          className="btn btn-danger btn-sm btn-icon"
                          title="Delete"
                          onClick={() => setAction({ type: 'delete', item })}
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

      {showFormModal && (
        <MenuItemFormModal
          restaurantName={selectedRestaurant?.name}
          item={editingItem}
          categories={categoryList}
          taxonomies={taxonomyList}
          onClose={() => {
            setShowFormModal(false);
            setEditingItem(null);
          }}
          onSubmit={submitMenuItem}
          loading={mutCreate.isPending || mutUpdate.isPending}
          categoriesLoading={menuCategories?.isLoading}
        />
      )}
    </div>
  );
}
