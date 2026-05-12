import { api } from './client';

export interface SystemParam {
  key: string;
  valueType: string;
  value: string;
  runtimeApplicable: boolean;
  description?: string;
  version: number;
  updatedBy?: string;
  updatedAt?: string;
}

interface RawSystemParam extends Omit<SystemParam, 'key'> {
  key?: string;
  paramKey?: string;
}

const normalizeParam = (param: RawSystemParam): SystemParam => ({
  ...param,
  key: param.key ?? param.paramKey ?? '',
});

const normalizeParamList = (raw: RawSystemParam[] | undefined): SystemParam[] =>
  (raw ?? []).map(normalizeParam).filter((param) => Boolean(param.key));

export const getSystemParams = () =>
  api.get<{ data: RawSystemParam[] }>('/admin/system-parameters').then((res) => ({
    ...res,
    data: {
      ...res.data,
      data: normalizeParamList(res.data?.data),
    },
  }));

export const putSystemParam = (key: string, body: { valueType: string; value: string; runtimeApplicable: boolean; description?: string }) =>
  api.put<{ data: RawSystemParam }>(`/admin/system-parameters/${key}`, body).then((res) => ({
    ...res,
    data: {
      ...res.data,
      data: normalizeParam(res.data.data),
    },
  }));

export const patchSystemParam = (key: string, body: Partial<{ valueType: string; value: string; runtimeApplicable: boolean; description: string }>) =>
  api.patch<{ data: RawSystemParam }>(`/admin/system-parameters/${key}`, body).then((res) => ({
    ...res,
    data: {
      ...res.data,
      data: normalizeParam(res.data.data),
    },
  }));
