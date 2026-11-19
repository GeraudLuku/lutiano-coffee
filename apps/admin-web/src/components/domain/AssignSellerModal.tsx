import { useState } from 'react';
import { operationsApi, staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { Field, FormModal } from '../ui';
import { SELLER_ROLE_CHANNEL, SELLER_ROLE_LABEL, TYPE_LABEL } from '../../utils/labels';
import type { OperationType, SellerRole } from '../../types/models';

/**
 * Links a seller to a sales location. Pass `sellerId` (+ `sellerRole`) to pick a location, or `operationId`
 * (+ `operationType`) to pick a seller. Only matching channels are offered (the server enforces the same rule).
 */
export function AssignSellerModal({ sellerId, sellerRole, operationId, operationType, exclude = [], onClose, onDone }: {
  sellerId?: string; sellerRole?: SellerRole | null; operationId?: string; operationType?: OperationType;
  exclude?: string[]; onClose: () => void; onDone: () => void;
}) {
  const pickLocation = !!sellerId;
  const channel = sellerRole ? SELLER_ROLE_CHANNEL[sellerRole] : undefined;
  const { data, loading } = useLoad(async (): Promise<[string, string][]> => pickLocation
    ? (await operationsApi.all()).filter(o => o.active && !exclude.includes(o.id) && (!channel || o.type === channel)).map(o => [o.id, `${o.name} (${o.code})`])
    : (await staffApi.all()).filter(s => s.role === 'SELLER' && s.active && !exclude.includes(s.id)
        && (!operationType || !s.seller_role || SELLER_ROLE_CHANNEL[s.seller_role] === operationType))
        .map(s => [s.id, `${s.full_name}${s.seller_role ? ` – ${SELLER_ROLE_LABEL[s.seller_role]}` : ''}`]),
  [pickLocation, channel, operationType]);
  const [choice, setChoice] = useState('');
  const { busy, error, run } = useAction();

  async function save() {
    const ok = await run(() => staffApi.assign(sellerId ?? choice, operationId ?? choice), 'Assignment saved.');
    if (ok) onDone();
  }
  const hint = pickLocation
    ? channel && `Only ${TYPE_LABEL[channel]} locations match this seller's role.`
    : operationType && `Only sellers whose role works ${TYPE_LABEL[operationType]} locations are listed.`;
  return (
    <FormModal title={pickLocation ? 'Assign sales location' : 'Assign seller'} submitLabel="Assign" busy={busy || !choice} error={error} onSubmit={save} onClose={onClose}>
      <Field label={pickLocation ? 'Sales location' : 'Seller'} required hint={hint || undefined}>
        <select className="input" value={choice} onChange={e => setChoice(e.target.value)} required>
          <option value="">Choose…</option>
          {(data ?? []).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
      </Field>
      {!loading && data?.length === 0 && (
        <p className="muted small">{pickLocation ? 'No other active locations match this seller\'s role.' : 'No other active sellers match this location. Create one in Users first.'}</p>
      )}
    </FormModal>
  );
}
