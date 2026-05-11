import { useState, useEffect } from 'react';
import { Spinner } from './ui';
import type { Restaurant } from '../api/restaurants';

interface Props {
  restaurant?: Restaurant;
  onClose: () => void;
  onSubmit: (data: Partial<Restaurant>) => void;
  loading?: boolean;
}

export function RestaurantFormModal({ restaurant, onClose, onSubmit, loading }: Props) {
  const [formData, setFormData] = useState({
    ownerUserId: '',
    name: '',
    description: '',
    addressLine: '',
    cuisineType: '',
    isOpen: true,
    status: 'PENDING_APPROVAL',
    latitude: 0,
    longitude: 0,
    maxDeliveryKm: 10
  });

  useEffect(() => {
    if (restaurant) {
      setFormData({
        ownerUserId: restaurant.ownerUserId || '',
        name: restaurant.name,
        description: restaurant.description || '',
        addressLine: restaurant.addressLine,
        cuisineType: restaurant.cuisineType,
        isOpen: restaurant.isOpen,
        status: restaurant.status,
        latitude: 0, // In a real app we'd load this from detail API
        longitude: 0,
        maxDeliveryKm: 10
      });
    }
  }, [restaurant]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ minWidth: 500 }}>
        <h2 className="modal-title">{restaurant ? 'Edit Restaurant' : 'Create Restaurant'}</h2>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Name</label>
              <input required className="input" value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} />
            </div>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Owner User ID</label>
              <input required className="input" value={formData.ownerUserId} onChange={e => setFormData({ ...formData, ownerUserId: e.target.value })} />
            </div>
          </div>
          <div>
            <label className="text-xs font-medium">Description</label>
            <input className="input" value={formData.description} onChange={e => setFormData({ ...formData, description: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-medium">Address</label>
            <input required className="input" value={formData.addressLine} onChange={e => setFormData({ ...formData, addressLine: e.target.value })} />
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Cuisine Type</label>
              <input required className="input" value={formData.cuisineType} onChange={e => setFormData({ ...formData, cuisineType: e.target.value })} />
            </div>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Status</label>
              <select className="input" value={formData.status} onChange={e => setFormData({ ...formData, status: e.target.value })}>
                <option value="PENDING_APPROVAL">PENDING_APPROVAL</option>
                <option value="APPROVED">APPROVED</option>
                <option value="REJECTED">REJECTED</option>
                <option value="SUSPENDED">SUSPENDED</option>
              </select>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
             <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Latitude</label>
              <input type="number" step="0.000001" className="input" value={formData.latitude} onChange={e => setFormData({ ...formData, latitude: parseFloat(e.target.value) })} />
            </div>
             <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Longitude</label>
              <input type="number" step="0.000001" className="input" value={formData.longitude} onChange={e => setFormData({ ...formData, longitude: parseFloat(e.target.value) })} />
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8 }}>
            <input type="checkbox" checked={formData.isOpen} onChange={e => setFormData({ ...formData, isOpen: e.target.checked })} />
            <label className="text-xs font-medium">Currently Open</label>
          </div>
          <div className="modal-footer" style={{ marginTop: 20 }}>
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
