import { api } from './client';

export interface SystemParam {
  paramKey: string;
  valueType: string;
  value: string;
  runtimeApplicable: boolean;
  description?: string;
  version: number;
  updatedAt?: string;
}

export const getSystemParams = () =>
  api.get<{ data: SystemParam[] }>('/admin/system-parameters');

export const putSystemParam = (key: string, body: { valueType: string; value: string; runtimeApplicable: boolean; description?: string }) =>
  api.put<{ data: SystemParam }>(`/admin/system-parameters/${key}`, body);

export const patchSystemParam = (key: string, body: Partial<{ valueType: string; value: string; runtimeApplicable: boolean; description: string }>) =>
  api.patch<{ data: SystemParam }>(`/admin/system-parameters/${key}`, body);
