import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';
import { login } from '../api/auth';
import { useAuthStore } from '../store/auth';
import { Spinner } from '../components/ui';

export default function LoginPage() {
  const nav = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await login(form.usernameOrEmail, form.password);
      const d = res.data.data;
      // Decode role from JWT payload
      let role = 'ADMIN';
      let username = form.usernameOrEmail;
      try {
        const payload = JSON.parse(atob(d.accessToken.split('.')[1]));
        role = payload.role ?? payload.authorities?.[0] ?? 'ADMIN';
        username = payload.sub ?? username;
      } catch (_) { /* ignore */ }
      setTokens(d.accessToken, d.refreshToken, username, role);
      nav('/');
    } catch (err: unknown) {
      console.error('Login error:', err);
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message ?? 'Login failed. Check credentials.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">F</div>
          <h1>Admin Console</h1>
          <p>Sign in to manage the Foodya platform</p>
        </div>

        {error && (
          <div className="login-error">
            <AlertCircle size={16} />
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="login-username">Username or Email</label>
            <input
              id="login-username"
              className="input"
              type="text"
              placeholder="api_admin"
              value={form.usernameOrEmail}
              onChange={(e) => setForm({ ...form, usernameOrEmail: e.target.value })}
              required
              autoFocus
            />
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="login-password">Password</label>
            <input
              id="login-password"
              className="input"
              type="password"
              placeholder="••••••••"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', justifyContent: 'center', marginTop: 8 }}
            disabled={loading}
          >
            {loading ? <Spinner /> : 'Sign In'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: 20, fontSize: 12, color: 'var(--text-muted)' }}>
          Foodya Admin · Restricted Access
        </p>
      </div>
    </div>
  );
}
