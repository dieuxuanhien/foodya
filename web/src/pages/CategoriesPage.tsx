import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit2, Trash2 } from 'lucide-react';
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  type CategoryTaxonomy,
} from '../api/categories';
import { PageHeader, LoadingCenter, EmptyState, ConfirmModal } from '../components/ui';

type Action = { type: 'delete'; cat: CategoryTaxonomy };

export default function CategoriesPage() {
  const qc = useQueryClient();
  const [showFormModal, setShowFormModal] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<CategoryTaxonomy | undefined>(undefined);
  const [action, setAction] = useState<Action | null>(null);
  const [formData, setFormData] = useState<Partial<CategoryTaxonomy>>({});

  const { data, isLoading } = useQuery({
    queryKey: ['admin-categories'],
    queryFn: getCategories,
  });

  const mutCreate = useMutation({
    mutationFn: createCategory,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-categories'] });
      setShowFormModal(false);
      setFormData({});
    },
  });

  const mutUpdate = useMutation({
    mutationFn: (vars: { code: string; data: { displayName: string; description?: string; sortOrder: number; isActive: boolean } }) =>
      updateCategory(vars.code, vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-categories'] });
      setShowFormModal(false);
      setFormData({});
    },
  });

  const mutDelete = useMutation({
    mutationFn: (code: string) => deleteCategory(code),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-categories'] });
      setAction(null);
    },
  });

  const categories = data?.data?.data ?? [];
  const isMutating = mutCreate.isPending || mutUpdate.isPending || mutDelete.isPending;

  const handleSubmit = () => {
    if (!formData.code || !formData.displayName) {
      alert('Code and Name are required');
      return;
    }
    if (selectedCategory) {
      mutUpdate.mutate({ 
        code: selectedCategory.code, 
        data: {
          displayName: formData.displayName,
          description: formData.description,
          sortOrder: formData.sortOrder ?? 0,
          isActive: formData.active ?? true,
        }
      });
    } else {
      mutCreate.mutate({
        code: formData.code,
        displayName: formData.displayName,
        description: formData.description,
        sortOrder: formData.sortOrder ?? 0,
        isActive: true,
      });
    }
  };

  const openForm = (cat?: CategoryTaxonomy) => {
    setSelectedCategory(cat);
    setFormData(cat ? { ...cat } : {});
    setShowFormModal(true);
  };

  return (
    <div className="page-content">
      <PageHeader
        title="Category Taxonomy"
        subtitle="Manage menu categories for restaurants"
        actions={
          <button className="btn btn-primary" onClick={() => openForm()}>
            <Plus size={16} /> New Category
          </button>
        }
      />

      <div className="card">
        {isLoading ? (
          <LoadingCenter />
        ) : categories.length === 0 ? (
          <EmptyState title="No categories found" desc="Create your first category" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Description</th>
                  <th>Display Order</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((c) => (
                  <tr key={c.code}>
                    <td>
                      <span className="font-mono text-sm">{c.code}</span>
                    </td>
                    <td style={{ fontWeight: 500 }}>{c.displayName}</td>
                    <td className="text-muted text-sm">{c.description || '—'}</td>
                    <td className="text-center">{c.sortOrder ?? '—'}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button
                          className="btn btn-ghost btn-sm btn-icon"
                          title="Edit"
                          onClick={() => openForm(c)}
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          className="btn btn-danger btn-sm btn-icon"
                          title="Delete"
                          onClick={() => setAction({ type: 'delete', cat: c })}
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
      </div>

      {showFormModal && (
        <div className="modal-backdrop" onClick={() => setShowFormModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title">{selectedCategory ? 'Edit Category' : 'New Category'}</h2>

            <div className="form-group">
              <label className="form-label" htmlFor="cat-code">
                Code *
              </label>
              <input
                id="cat-code"
                className="input"
                type="text"
                placeholder="e.g., MAIN_COURSE"
                value={formData.code ?? ''}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                disabled={!!selectedCategory}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="cat-name">
                Name *
              </label>
              <input
                id="cat-name"
                className="input"
                type="text"
                placeholder="Main Courses"
                value={formData.displayName ?? ''}
                onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="cat-desc">
                Description
              </label>
              <input
                id="cat-desc"
                className="input"
                type="text"
                placeholder="Optional description"
                value={formData.description ?? ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="cat-order">
                Display Order
              </label>
              <input
                id="cat-order"
                className="input"
                type="number"
                min="0"
                value={formData.sortOrder ?? ''}
                onChange={(e) => setFormData({ ...formData, sortOrder: parseInt(e.target.value) })}
              />
            </div>

            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setShowFormModal(false)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSubmit} disabled={isMutating}>
                {isMutating ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {action && (
        <ConfirmModal
          title="Delete Category"
          message={`Permanently delete category "${action.cat.displayName}"? This cannot be undone.`}
          onConfirm={() => mutDelete.mutate(action.cat.code)}
          onCancel={() => setAction(null)}
          loading={mutDelete.isPending}
          danger
        />
      )}
    </div>
  );
}
