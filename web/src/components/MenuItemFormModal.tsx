import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Spinner } from './ui';
import type { MenuItem, CreateMenuItemPayload } from '../api/menu-items';
import type { MenuCategory } from '../api/menu-categories';
import type { CategoryTaxonomy } from '../api/categories';

interface Props {
  restaurantName?: string;
  item?: MenuItem | null;
  categories: MenuCategory[];
  taxonomies: CategoryTaxonomy[];
  onClose: () => void;
  onSubmit: (data: CreateMenuItemPayload) => void;
  loading?: boolean;
  categoriesLoading?: boolean;
}

export function MenuItemFormModal({ restaurantName, item, categories, taxonomies, onClose, onSubmit, loading, categoriesLoading }: Props) {
  const [categoryId, setCategoryId] = useState('');
  const [taxonomyCodes, setTaxonomyCodes] = useState<string[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [isAvailable, setIsAvailable] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (item) {
      setCategoryId(item.categoryId || '');
      setTaxonomyCodes(item.taxonomyCodes || []);
      setName(item.name || '');
      setDescription(item.description || '');
      setPrice(String(item.price ?? ''));
      setIsActive(Boolean(item.isActive));
      setIsAvailable(Boolean(item.isAvailable));
      setError('');
    } else {
       setName('');
       setDescription('');
       setPrice('');
       setTaxonomyCodes([]);
       setError('');
       if (categories.length > 0) {
         setCategoryId(categories[0].id);
       } else {
         setCategoryId('');
       }
    }
  }, [item, categories]);

  const taxonomyOptions = useMemo(() => taxonomies.map((t) => ({ value: t.code, label: t.name })), [taxonomies]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const parsedPrice = Number(price);
    if (!categoryId) {
      setError('Select a menu category');
      return;
    }
    if (taxonomyCodes.length === 0) {
      setError('Select at least one taxonomy tag');
      return;
    }
    if (!name.trim() || !description.trim()) {
      setError('Name and description are required');
      return;
    }
    if (!Number.isFinite(parsedPrice) || parsedPrice <= 0) {
      setError('Enter a valid price');
      return;
    }

    setError('');
    onSubmit({
      categoryId,
      taxonomyCodes,
      name: name.trim(),
      description: description.trim(),
      price: parsedPrice,
      isActive,
      isAvailable,
    });
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ minWidth: 640 }}>
        <h2 className="modal-title">{item ? 'Edit Menu Item' : 'Create Menu Item'}</h2>
        <p className="text-muted text-sm" style={{ marginTop: 4 }}>
          {restaurantName ? `Restaurant: ${restaurantName}` : 'Select a restaurant before creating items.'}
        </p>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div>
              <label className="text-xs font-medium">Menu Category</label>
              {categoriesLoading ? (
                <div className="input" style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'var(--bg-subtle)' }}>
                  <Spinner size={14} />
                  <span className="text-sm text-muted">Loading categories...</span>
                </div>
              ) : categories.length > 0 ? (
                <select className="input" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                  <option value="">Select category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              ) : (
                <>
                  <input
                    className="input"
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                    placeholder="Category UUID"
                    required
                  />
                  <p className="text-muted text-xs">No active menu categories were found for this restaurant.</p>
                </>
              )}
            </div>
            <div>
              <label className="text-xs font-medium">Price</label>
              <input
                className="input"
                type="number"
                min="0"
                step="1"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                required
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-medium">Taxonomy Tags</label>
            <select
              className="input"
              multiple
              size={Math.min(6, Math.max(3, taxonomyOptions.length || 3))}
              value={taxonomyCodes}
              onChange={(e) => setTaxonomyCodes(Array.from(e.target.selectedOptions, (option) => option.value))}
            >
              {taxonomyOptions.map((taxonomy) => (
                <option key={taxonomy.value} value={taxonomy.value}>
                  {taxonomy.label} ({taxonomy.value})
                </option>
              ))}
            </select>
            <p className="text-muted text-xs">Hold Ctrl/Cmd to select multiple tags.</p>
          </div>

          <div>
            <label className="text-xs font-medium">Name</label>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>

          <div>
            <label className="text-xs font-medium">Description</label>
            <textarea
              className="input"
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              style={{ resize: 'vertical' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <label className="card" style={{ padding: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
              <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
              <span className="text-sm">Active</span>
            </label>
            <label className="card" style={{ padding: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
              <input type="checkbox" checked={isAvailable} onChange={(e) => setIsAvailable(e.target.checked)} />
              <span className="text-sm">Available</span>
            </label>
          </div>

          {error && <p className="text-sm" style={{ color: 'var(--danger-600)' }}>{error}</p>}

          <div className="modal-footer" style={{ marginTop: 8 }}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <Spinner /> : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
