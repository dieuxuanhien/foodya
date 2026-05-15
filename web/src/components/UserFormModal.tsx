import { useState, useEffect } from 'react';
import { Spinner } from './ui';
import type { User } from '../api/users';

interface Props {
  user?: User;
  onClose: () => void;
  onSubmit: (data: Partial<User> & { password?: string }) => void;
  loading?: boolean;
}

export function UserFormModal({ user, onClose, onSubmit, loading }: Props) {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    phoneNumber: '',
    fullName: '',
    role: 'CUSTOMER',
    status: 'ACTIVE',
    password: ''
  });

  useEffect(() => {
    if (user) {
      setFormData({
        username: user.username,
        email: user.email,
        phoneNumber: user.phoneNumber,
        fullName: user.fullName,
        role: user.role,
        status: user.status,
        password: ''
      });
    }
  }, [user]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (user) {
      const { password, ...rest } = formData;
      onSubmit(rest);
    } else {
      onSubmit(formData);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ minWidth: 400 }}>
        <h2 className="modal-title">{user ? 'Edit User' : 'Create User'}</h2>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
          <div>
            <label className="text-xs font-medium">Username</label>
            <input required className="input" value={formData.username} onChange={e => setFormData({ ...formData, username: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-medium">Email</label>
            <input required type="email" className="input" value={formData.email} onChange={e => setFormData({ ...formData, email: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-medium">Phone</label>
            <input required className="input" value={formData.phoneNumber} onChange={e => setFormData({ ...formData, phoneNumber: e.target.value })} />
          </div>
          <div>
            <label className="text-xs font-medium">Full Name</label>
            <input required className="input" value={formData.fullName} onChange={e => setFormData({ ...formData, fullName: e.target.value })} />
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Role</label>
              <select className="input" value={formData.role} onChange={e => setFormData({ ...formData, role: e.target.value })}>
                <option value="CUSTOMER">CUSTOMER</option>
                <option value="MERCHANT">MERCHANT</option>
                <option value="ADMIN">ADMIN</option>
                <option value="DRIVER">DRIVER</option>
              </select>
            </div>
            <div style={{ flex: 1 }}>
              <label className="text-xs font-medium">Status</label>
              <select className="input" value={formData.status} onChange={e => setFormData({ ...formData, status: e.target.value })}>
                <option value="ACTIVE">ACTIVE</option>
                <option value="LOCKED">LOCKED</option>
                <option value="PENDING_APPROVAL">PENDING</option>
              </select>
            </div>
          </div>
          {!user && (
            <div>
              <label className="text-xs font-medium">Password</label>
              <input required type="password" className="input" value={formData.password} onChange={e => setFormData({ ...formData, password: e.target.value })} />
            </div>
          )}
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
