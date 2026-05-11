import { api } from './client';

export interface CategoryTaxonomy {
  code: string;
  name: string;
  description?: string;
  icon?: string;
  displayOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export const getCategories = () =>
  api.get<{ data: CategoryTaxonomy[] }>('/admin/category-taxonomies');

export const createCategory = (data: {
  code: string;
  name: string;
  description?: string;
  icon?: string;
  displayOrder?: number;
}) =>
  api.post<{ data: CategoryTaxonomy }>('/admin/category-taxonomies', data);

export const updateCategory = (code: string, data: {
  name: string;
  description?: string;
  icon?: string;
  displayOrder?: number;
}) =>
  api.put<{ data: CategoryTaxonomy }>(`/admin/category-taxonomies/${code}`, data);

export const deleteCategory = (code: string) =>
  api.delete(`/admin/category-taxonomies/${code}`);
