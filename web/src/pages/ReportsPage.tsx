import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { getRevenueReport } from '../api/reports';
import { PageHeader, LoadingCenter, EmptyState, formatCurrency } from '../components/ui';

export default function ReportsPage() {
  const [dateRange, setDateRange] = useState({ from: '', to: '' });
  const [appliedRange, setAppliedRange] = useState({ from: '', to: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['admin-revenue-report', appliedRange],
    queryFn: () => getRevenueReport(appliedRange.from, appliedRange.to),
  });

  const rep = data?.data?.data;

  const handleApply = () => setAppliedRange(dateRange);
  const handleClear = () => { setDateRange({ from: '', to: '' }); setAppliedRange({ from: '', to: '' }); };

  return (
    <div className="page-content">
      <PageHeader title="Revenue & Analytics" subtitle="Platform financial performance and commission tracking" />

      <div className="card" style={{ marginBottom: 24 }}>
        <div className="toolbar" style={{ marginBottom: 0 }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">From Date</label>
            <input type="date" className="input" value={dateRange.from} onChange={(e) => setDateRange({ ...dateRange, from: e.target.value })} />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">To Date</label>
            <input type="date" className="input" value={dateRange.to} onChange={(e) => setDateRange({ ...dateRange, to: e.target.value })} />
          </div>
          <div style={{ alignSelf: 'flex-end', display: 'flex', gap: 8 }}>
            <button className="btn btn-primary" onClick={handleApply}>Generate Report</button>
            {(appliedRange.from || appliedRange.to) && <button className="btn btn-ghost" onClick={handleClear}>Clear</button>}
          </div>
        </div>
      </div>

      {isLoading ? <LoadingCenter label="Generating report…" /> : !rep ? <EmptyState title="No report data" /> : (
        <>
          <div className="kpi-grid">
            <StatCard title="Total Revenue" value={formatCurrency(rep.revenue, rep.currencyCode || 'VND')} color="var(--success)" />
            <StatCard title="Total Orders" value={rep.orderCount} color="var(--info)" />
            <StatCard title="Platform Profit" value={formatCurrency(rep.platformProfit, rep.currencyCode || 'VND')} color="var(--warning)" />
            <StatCard title="Avg Order Value" value={formatCurrency(rep.avgOrderValue, rep.currencyCode || 'VND')} color="var(--accent)" />
          </div>

          <div className="grid-2">
            <div className="card">
              <div className="card-header"><div className="card-title">Revenue Breakdown</div></div>
              <div className="chart-area">
                {rep.series && rep.series.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={rep.series}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="period" />
                      <YAxis tickFormatter={(val) => `₫${(val / 1000).toFixed(0)}k`} />
                      <Tooltip formatter={(val: number) => formatCurrency(val, rep.currencyCode || 'VND')} cursor={{ fill: 'var(--bg-hover)' }} contentStyle={{ background: 'var(--bg-elevated)', border: 'none', borderRadius: 8, color: '#fff' }} />
                      <Bar dataKey="revenue" fill="var(--info)" name="Total Revenue" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <EmptyState title="No timeline data available" />
                )}
              </div>
            </div>

            <div className="card">
              <div className="card-header"><div className="card-title">Platform Profit Breakdown</div></div>
              <div className="chart-area">
                {rep.series && rep.series.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={rep.series}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="period" />
                      <YAxis tickFormatter={(val) => `₫${(val / 1000).toFixed(0)}k`} />
                      <Tooltip formatter={(val: number) => formatCurrency(val, rep.currencyCode || 'VND')} cursor={{ fill: 'var(--bg-hover)' }} contentStyle={{ background: 'var(--bg-elevated)', border: 'none', borderRadius: 8, color: '#fff' }} />
                      <Bar dataKey="platformProfit" fill="var(--warning)" name="Platform Profit" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <EmptyState title="No timeline data available" />
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function StatCard({ title, value, color }: { title: string; value: string | number; color: string }) {
  return (
    <div className="card" style={{ borderTop: `3px solid ${color}` }}>
      <div className="kpi-label">{title}</div>
      <div className="kpi-value" style={{ fontSize: 24, marginTop: 10 }}>{value}</div>
    </div>
  );
}
