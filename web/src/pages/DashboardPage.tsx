import { useQuery } from '@tanstack/react-query';
import { Users, Store, ShoppingBag, TrendingUp } from 'lucide-react';
import { getUsers } from '../api/users';
import { getRestaurants } from '../api/restaurants';
import { getOrders } from '../api/orders';
import { getRevenueReport } from '../api/reports';
import { PageHeader, LoadingCenter, formatCurrency } from '../components/ui';

export default function DashboardPage() {
  const users = useQuery({ queryKey: ['admin-users-count'], queryFn: () => getUsers(undefined, 0, 1) });
  const restaurants = useQuery({ queryKey: ['admin-restaurants-count'], queryFn: () => getRestaurants(undefined, undefined, 0, 1) });
  const orders = useQuery({ queryKey: ['admin-orders-count'], queryFn: () => getOrders(undefined, 0, 1) });
  const revenue = useQuery({ queryKey: ['admin-revenue-dash'], queryFn: () => getRevenueReport() });

  const uMeta = users.data?.data?.meta;
  const rMeta = restaurants.data?.data?.meta;
  const oMeta = orders.data?.data?.meta;
  const rev = revenue.data?.data?.data;

  if (users.isLoading) return <div className="page-content"><LoadingCenter /></div>;

  return (
    <div className="page-content">
      <PageHeader title="Dashboard" subtitle="Platform overview at a glance" />

      <div className="kpi-grid">
        <KpiCard
          icon={<Users size={28} />}
          label="Total Users"
          value={uMeta?.totalElements ?? '—'}
          colorClass="info"
        />
        <KpiCard
          icon={<Store size={28} />}
          label="Restaurants"
          value={rMeta?.totalElements ?? '—'}
          colorClass="accent"
        />
        <KpiCard
          icon={<ShoppingBag size={28} />}
          label="Total Orders"
          value={oMeta?.totalElements ?? '—'}
          colorClass="success"
        />
        <KpiCard
          icon={<TrendingUp size={28} />}
          label="Platform Revenue"
          value={rev ? formatCurrency(rev.revenue, rev.currencyCode || 'VND') : '—'}
          colorClass="warning"
          sub={rev ? `${rev.orderCount} orders` : undefined}
        />
      </div>

      {rev && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <StatCard label="Platform Profit" value={formatCurrency(rev.platformProfit, rev.currencyCode || 'VND')} />
          <StatCard label="Avg Order Value" value={formatCurrency(rev.avgOrderValue, rev.currencyCode || 'VND')} />
        </div>
      )}

      <div style={{ marginTop: 24 }}>
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Quick Actions</div>
              <div className="card-subtitle">Common administrative tasks</div>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <a href="/users" className="btn btn-ghost">Manage Users</a>
            <a href="/restaurants" className="btn btn-ghost">Review Restaurants</a>
            <a href="/orders" className="btn btn-ghost">Monitor Orders</a>
            <a href="/system-params" className="btn btn-ghost">Configure System</a>
          </div>
        </div>
      </div>
    </div>
  );
}

function KpiCard({ icon, label, value, colorClass, sub }: { icon: React.ReactNode; label: string; value: string | number; colorClass: string; sub?: string }) {
  return (
    <div className={`kpi-card ${colorClass}`}>
      <div className="kpi-icon">{icon}</div>
      <div className="kpi-label">{label}</div>
      <div className="kpi-value">{value}</div>
      {sub && <div className="kpi-sub">{sub}</div>}
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="card">
      <div className="kpi-label">{label}</div>
      <div style={{ fontSize: 20, fontWeight: 700, marginTop: 6 }}>{value}</div>
    </div>
  );
}
