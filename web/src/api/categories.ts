import { api } from './client';

export interface CategoryTaxonomy {
  code: string;
  displayName: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export const getCategories = () =>
  api.get<{ data: CategoryTaxonomy[] }>('/admin/category-taxonomies');

export const createCategory = (data: {
  code: string;
  displayName: string;
  description?: string;
  icon?: string;
  sortOrder: number;
  isActive: boolean;
}) =>
  api.post<{ data: CategoryTaxonomy }>('/admin/category-taxonomies', data);

export const updateCategory = (code: string, data: {
  displayName: string;
  description?: string;
  icon?: string;
  sortOrder: number;
  isActive: boolean;
}) =>
  api.put<{ data: CategoryTaxonomy }>(`/admin/category-taxonomies/${code}`, data);

export const deleteCategory = (code: string) =>
  api.delete(`/admin/category-taxonomies/${code}`);
