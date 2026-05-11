import { useState } from 'react';
import { Spinner } from './ui';
import type { User } from '../api/users';

interface Props {
  user: User;
  onClose: () => void;
  onSubmit: (password: string) => void;
  loading?: boolean;
}

export function ResetPasswordModal({ user, onClose, onSubmit, loading }: Props) {
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (password) {
      onSubmit(password);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2 className="modal-title">Reset Password</h2>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 16 }}>
          Force reset password for user <strong>{user.username}</strong>.
        </p>
        <form onSubmit={handleSubmit}>
          <input
            required
            type="password"
            className="input"
            placeholder="New password"
            value={password}
            onChange={e => setPassword(e.target.value)}
          />
          <div className="modal-footer" style={{ marginTop: 20 }}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-warning" disabled={loading}>
              {loading ? <Spinner /> : 'Reset Password'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
