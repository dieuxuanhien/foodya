import { api } from './client';

export interface RevenueReport {
  revenue: number;
  orderCount: number;
  platformProfit: number;
  avgOrderValue: number;
  currencyCode?: string;
  fromDate?: string;
  toDate?: string;
  series?: Array<{ period: string; revenue: number; orderCount: number; platformProfit: number }>;
}

export const getRevenueReport = (from?: string, to?: string) =>
  api.get<{ data: RevenueReport }>('/admin/reports/revenue', {
    params: { from: from || undefined, to: to || undefined },
  });
