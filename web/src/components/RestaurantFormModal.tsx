import { useState, useEffect } from 'react';
import { Spinner } from './ui';
import type { Restaurant } from '../api/restaurants';
import { getUsers } from '../api/users';
import { useQuery } from '@tanstack/react-query';

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
    open: true,
    status: 'PENDING_APPROVAL',
    latitude: 0,
    longitude: 0,
    maxDeliveryKm: 10
  });

  const [userSearch, setUserSearch] = useState('');
  
  const { data: userData } = useQuery({
    queryKey: ['admin-users-lookup', userSearch],
    queryFn: () => getUsers(userSearch, 0, 10),
    enabled: userSearch.length > 2 || !!restaurant
  });

  const users = userData?.data?.data ?? [];

  useEffect(() => {
    if (restaurant) {
      setFormData({
        ownerUserId: restaurant.ownerUserId || '',
        name: restaurant.name,
        description: restaurant.description || '',
        addressLine: restaurant.addressLine,
        cuisineType: restaurant.cuisineType,
        open: restaurant.open,
        status: restaurant.status,
        latitude: 0,
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
              <input required className="input" value={formData.name || ''} onChange={e => setFormData({ ...formData, name: e.target.value })} />
            </div>
          </div>
          
          <div>
            <label className="text-xs font-medium">Owner (Merchant)</label>
            <div style={{ display: 'flex', gap: 8 }}>
               <select 
                required 
                className="input" 
                style={{ flex: 2 }}
                value={formData.ownerUserId || ''} 
                onChange={e => setFormData({ ...formData, ownerUserId: e.target.value })}
              >
                <option value="">Select an owner</option>
                {restaurant && !users.find(u => u.id === restaurant.ownerUserId) && (
                  <option value={restaurant.ownerUserId}>Current Owner ({restaurant.ownerUserId.substring(0,8)})</option>
                )}
                {users.map(u => (
                  <option key={u.id} value={u.id}>{u.fullName} ({u.role})</option>
                ))}
              </select>
              <input 
                className="input" 
                style={{ flex: 1 }}
                placeholder="Search users..." 
                value={userSearch} 
                onChange={e => setUserSearch(e.target.value)} 
              />
            </div>
            <p className="text-xs text-muted mt-1">Search by name or email to find a merchant user.</p>
          </div>

          <div>
            <label className="text-xs font-medium">Description</label>
            <input className="input" value={formData.description || ''} onChange={e => setFormData({ ...formData, description: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-medium">Address</label>
            <input required className="input" value={formData.addressLine || ''} onChange={e => setFormData({ ...formData, addressLine: e.target.value })} />
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Cuisine Type</label>
              <input required className="input" value={formData.cuisineType || ''} onChange={e => setFormData({ ...formData, cuisineType: e.target.value })} />
            </div>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Status</label>
              <select className="input" value={formData.status || 'PENDING_APPROVAL'} onChange={e => setFormData({ ...formData, status: e.target.value })}>
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
              <input type="number" step="0.000001" className="input" value={formData.latitude} onChange={e => setFormData({ ...formData, latitude: parseFloat(e.target.value) || 0 })} />
            </div>
             <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Longitude</label>
              <input type="number" step="0.000001" className="input" value={formData.longitude} onChange={e => setFormData({ ...formData, longitude: parseFloat(e.target.value) || 0 })} />
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8 }}>
            <input type="checkbox" checked={formData.open} onChange={e => setFormData({ ...formData, open: e.target.checked })} />
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
