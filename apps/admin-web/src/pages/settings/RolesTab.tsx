import { useState } from 'react';
import { Shield, UserRound } from 'lucide-react';
import { staffApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { Panel } from '../../components/ui';
import { ROLE_DESCRIPTION, ROLE_LABEL } from '../../utils/labels';
import type { Role } from '../../types/models';
import { PermissionList } from '../users/PermissionList';

const ROLES = Object.keys(ROLE_LABEL) as Role[];

export function RolesTab({ canSeeCounts }: { canSeeCounts: boolean }) {
  const [role, setRole] = useState<Role>('ADMIN');
  const staff = useLoad(() => (canSeeCounts ? staffApi.all() : Promise.resolve([])), [canSeeCounts]);
  const count = (r: Role) => (staff.data ?? []).filter(s => s.role === r).length;
  return (
    <div className="grid-roles">
      <Panel title="Roles" icon={<Shield size={19} />}>
        <ul className="role-list">
          {ROLES.map(r => (
            <li key={r}><button className={role === r ? 'active' : ''} onClick={() => setRole(r)}>
              <UserRound size={18} /><span><b>{ROLE_LABEL[r]}</b><small>{ROLE_DESCRIPTION[r]}</small></span>
              {canSeeCounts && <em>{count(r)} {count(r) === 1 ? 'user' : 'users'}</em>}
            </button></li>
          ))}
        </ul>
      </Panel>
      <Panel title={`${ROLE_LABEL[role]} permissions`} icon={<Shield size={19} />}>
        <PermissionList role={role} />
        <p className="muted small">Roles are fixed in V1 and enforced by the server on every request. Assign a role from Users.</p>
      </Panel>
    </div>
  );
}
