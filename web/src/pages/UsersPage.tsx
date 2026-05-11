import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Lock, Unlock, Trash2, Edit2, Key, Plus } from 'lucide-react';
import { getUsers, lockUser, unlockUser, deleteUser, createUser, updateUser, resetUserPassword, type User } from '../api/users';
import { PageHeader, LoadingCenter, EmptyState, StatusBadge, Pagination, ConfirmModal } from '../components/ui';
import { UserFormModal } from '../components/UserFormModal';
import { ResetPasswordModal } from '../components/ResetPasswordModal';

type Action = { type: 'lock' | 'unlock' | 'delete'; user: User };

export default function UsersPage() {
  const qc = useQueryClient();
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<Action | null>(null);
  const [showFormModal, setShowFormModal] = useState(false);
  const [showResetModal, setShowResetModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | undefined>(undefined);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-users', search, page],
    queryFn: () => getUsers(search || undefined, page, 20),
  });

  const mutLock = useMutation({ mutationFn: (id: string) => lockUser(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-users'] }); setAction(null); } });
  const mutUnlock = useMutation({ mutationFn: (id: string) => unlockUser(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-users'] }); setAction(null); } });
  const mutDelete = useMutation({ mutationFn: (id: string) => deleteUser(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-users'] }); setAction(null); } });
  const mutCreate = useMutation({ mutationFn: createUser, onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-users'] }); setShowFormModal(false); } });
  const mutUpdate = useMutation({ mutationFn: (vars: { id: string, data: Partial<User> }) => updateUser(vars.id, vars.data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-users'] }); setShowFormModal(false); } });
  const mutReset = useMutation({ mutationFn: (vars: { id: string, pass: string }) => resetUserPassword(vars.id, vars.pass), onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-users'] }); setShowResetModal(false); } });

  const users = data?.data?.data ?? [];
  const meta = data?.data?.meta;
  const isMutating = mutLock.isPending || mutUnlock.isPending || mutDelete.isPending;

  const handleConfirm = () => {
    if (!action) return;
    if (action.type === 'lock') mutLock.mutate(action.user.id);
    else if (action.type === 'unlock') mutUnlock.mutate(action.user.id);
    else mutDelete.mutate(action.user.id);
  };

  return (
    <div className="page-content">
      <PageHeader 
        title="User Management" 
        subtitle="Search, create, update, and delete platform users"
        actions={
          <button className="btn btn-primary" onClick={() => { setSelectedUser(undefined); setShowFormModal(true); }}>
            <Plus size={16} /> New User
          </button>
        }
      />

      <div className="card">
        <div className="toolbar">
          <div style={{ position: 'relative', flex: 1, maxWidth: 280 }}>
            <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              id="users-search"
              className="input"
              style={{ paddingLeft: 32 }}
              placeholder="Search by username, email…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') { setSearch(q); setPage(0); } }}
            />
          </div>
          <button className="btn btn-ghost" onClick={() => { setSearch(q); setPage(0); }}>Search</button>
          {search && <button className="btn btn-ghost btn-sm" onClick={() => { setQ(''); setSearch(''); setPage(0); }}>Clear</button>}
        </div>

        {isLoading ? <LoadingCenter /> : users.length === 0 ? <EmptyState title="No users found" desc="Try a different search query" /> : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Full Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td><span className="font-mono">{u.username}</span></td>
                    <td>{u.fullName}</td>
                    <td className="truncate">{u.email}</td>
                    <td>{u.phoneNumber}</td>
                    <td><span className="badge badge-info">{u.role}</span></td>
                    <td><StatusBadge status={u.status} /></td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button className="btn btn-ghost btn-sm btn-icon" title="Edit" onClick={() => { setSelectedUser(u); setShowFormModal(true); }}>
                          <Edit2 size={13} />
                        </button>
                        <button className="btn btn-ghost btn-sm btn-icon" title="Reset Password" onClick={() => { setSelectedUser(u); setShowResetModal(true); }}>
                          <Key size={13} />
                        </button>
                        {u.status === 'ACTIVE' && (
                          <button id={`lock-${u.id}`} className="btn btn-warning btn-sm btn-icon" title="Lock" onClick={() => setAction({ type: 'lock', user: u })}>
                            <Lock size={13} />
                          </button>
                        )}
                        {u.status === 'LOCKED' && (
                          <button id={`unlock-${u.id}`} className="btn btn-success btn-sm btn-icon" title="Unlock" onClick={() => setAction({ type: 'unlock', user: u })}>
                            <Unlock size={13} />
                          </button>
                        )}
                        <button id={`delete-user-${u.id}`} className="btn btn-danger btn-sm btn-icon" title="Delete" onClick={() => setAction({ type: 'delete', user: u })}>
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {meta && <Pagination page={page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />}
      </div>

      {action && (
        <ConfirmModal
          title={action.type === 'delete' ? 'Delete User' : action.type === 'lock' ? 'Lock User' : 'Unlock User'}
          message={
            action.type === 'delete'
              ? `Permanently delete "${action.user.username}"? This cannot be undone.`
              : action.type === 'lock'
              ? `Lock account "${action.user.username}"? They will not be able to login.`
              : `Unlock account "${action.user.username}"?`
          }
          onConfirm={handleConfirm}
          onCancel={() => setAction(null)}
          loading={isMutating}
          danger={action.type === 'delete'}
        />
      )}

      {showFormModal && (
        <UserFormModal 
          user={selectedUser} 
          onClose={() => setShowFormModal(false)}
          onSubmit={(data) => selectedUser ? mutUpdate.mutate({ id: selectedUser.id, data }) : mutCreate.mutate(data)}
          loading={mutCreate.isPending || mutUpdate.isPending}
        />
      )}

      {showResetModal && selectedUser && (
        <ResetPasswordModal
          user={selectedUser}
          onClose={() => setShowResetModal(false)}
          onSubmit={(pass) => mutReset.mutate({ id: selectedUser.id, pass })}
          loading={mutReset.isPending}
        />
      )}
    </div>
  );
}
