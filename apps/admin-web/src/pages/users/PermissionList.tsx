import { Check, X } from 'lucide-react';
import { PERMISSIONS } from '../../utils/labels';
import type { Role } from '../../types/models';

export function PermissionList({ role }: { role: Role }) {
  return (
    <ul className="perm-list">
      {PERMISSIONS.map(([label, roles]) => {
        const yes = roles.includes(role);
        return <li key={label} className={yes ? 'yes' : 'no'}>{yes ? <Check size={15} aria-label="Allowed" /> : <X size={15} aria-label="Not allowed" />}{label}</li>;
      })}
    </ul>
  );
}
