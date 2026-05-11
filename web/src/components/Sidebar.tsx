import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Users, Store, ShoppingBag,
  BarChart2, Settings, Bell, LogOut,
  Tag,
} from 'lucide-react';
import { useAuthStore } from '../store/auth';
import { logout } from '../api/auth';

const NAV = [
  { label: 'Overview', items: [
    { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  ]},
  { label: 'Governance', items: [
    { to: '/users', icon: Users, label: 'Users' },
    { to: '/restaurants', icon: Store, label: 'Restaurants' },
    { to: '/orders', icon: ShoppingBag, label: 'Orders' },
  ]},
  { label: 'Catalog Management', items: [
    { to: '/categories', icon: Tag, label: 'Categories' },
  ]},
  { label: 'Analytics', items: [
    { to: '/reports', icon: BarChart2, label: 'Revenue Reports' },
    { to: '/notifications', icon: Bell, label: 'Notifications' },
  ]},
  { label: 'Configuration', items: [
    { to: '/system-params', icon: Settings, label: 'System Parameters' },
  ]},
];

export default function Sidebar() {
  const { username, refreshToken, clear } = useAuthStore();
  const nav = useNavigate();

  const handleLogout = async () => {
    try { if (refreshToken) await logout(refreshToken); } catch (_) { /* ignore */ }
    clear();
    nav('/login');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="logo-icon">F</div>
        <span className="logo-text">Foodya</span>
        <span className="logo-badge">Admin</span>
      </div>

      <nav className="sidebar-nav">
        {NAV.map((group) => (
          <div key={group.label}>
            <div className="nav-group-label">{group.label}</div>
            {group.items.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
              >
                <Icon size={16} />
                {label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-card">
          <div className="user-avatar">{username?.[0]?.toUpperCase() ?? 'A'}</div>
          <div className="user-info">
            <div className="user-name">{username ?? 'Admin'}</div>
            <div className="user-role">Administrator</div>
          </div>
          <button className="logout-btn" onClick={handleLogout} title="Logout">
            <LogOut size={15} />
          </button>
        </div>
      </div>
    </aside>
  );
}
