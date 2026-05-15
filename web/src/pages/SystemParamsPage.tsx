import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Save, X } from 'lucide-react';
import axios from 'axios';
import { getSystemParams, putSystemParam, type SystemParam } from '../api/system-params';
import { PageHeader, LoadingCenter, EmptyState, formatDate } from '../components/ui';

export default function SystemParamsPage() {
  const qc = useQueryClient();
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-system-params'],
    queryFn: getSystemParams,
  });

  const mutPut = useMutation({
    mutationFn: (p: { key: string; param: SystemParam }) =>
      putSystemParam(p.key, {
        valueType: p.param.valueType,
        value: editValue,
        runtimeApplicable: p.param.runtimeApplicable,
        description: p.param.description,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-system-params'] });
      setEditingKey(null);
    },
  });

  const params = data?.data?.data ?? [];
  const putErrorMessage = axios.isAxiosError(mutPut.error)
    ? mutPut.error.response?.data?.message ?? mutPut.error.message
    : mutPut.error
      ? 'Failed to update parameter'
      : '';

  return (
    <div className="page-content">
      <PageHeader title="System Parameters" subtitle="Configure global platform settings and AI limits" />

      <div className="card">
        {isLoading ? <LoadingCenter /> : params.length === 0 ? (
          <EmptyState title="No parameters found" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Parameter Key</th>
                  <th>Value</th>
                  <th>Type</th>
                  <th>Runtime</th>
                  <th>Description</th>
                  <th>Last Updated</th>
                </tr>
              </thead>
              <tbody>
                {params.map((p) => {
                  const isEditing = editingKey === p.key;
                  return (
                    <tr key={p.key}>
                      <td><span className="font-mono text-accent" style={{ color: 'var(--accent)' }}>{p.key}</span></td>
                      <td>
                        {isEditing ? (
                          <div className="param-value-cell">
                            <input
                              className="input param-input"
                              value={editValue}
                              onChange={(e) => setEditValue(e.target.value)}
                              autoFocus
                            />
                            <button
                              className="btn btn-success btn-sm btn-icon"
                              onClick={() => mutPut.mutate({ key: p.key, param: p })}
                              disabled={mutPut.isPending}
                            >
                              <Save size={13} />
                            </button>
                            <button
                              className="btn btn-ghost btn-sm btn-icon"
                              onClick={() => setEditingKey(null)}
                            >
                              <X size={13} />
                            </button>
                          </div>
                        ) : (
                          <div
                            style={{
                              cursor: p.runtimeApplicable ? 'pointer' : 'not-allowed',
                              borderBottom: p.runtimeApplicable ? '1px dashed var(--border-subtle)' : 'none',
                              display: 'inline-block',
                            }}
                            onClick={() => {
                              if (!p.runtimeApplicable) return;
                              setEditingKey(p.key);
                              setEditValue(p.value);
                            }}
                            title={p.runtimeApplicable ? 'Click to edit' : 'Non-runtime parameter (requires controlled restart/redeploy)'}
                          >
                            {p.value}
                          </div>
                        )}
                      </td>
                      <td className="text-muted text-sm">{p.valueType}</td>
                      <td>{p.runtimeApplicable ? <span className="badge badge-success">Yes</span> : <span className="badge badge-muted">No</span>}</td>
                      <td className="text-muted text-sm" style={{ maxWidth: 200, whiteSpace: 'normal' }}>{p.description}</td>
                      <td className="text-muted text-sm">{formatDate(p.updatedAt)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {putErrorMessage && (
              <p style={{ color: 'var(--danger)', marginTop: 10, fontSize: 13 }}>
                {putErrorMessage}
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
