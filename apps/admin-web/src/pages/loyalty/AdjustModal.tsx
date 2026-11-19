import { useState } from 'react';
import { customersApi, loyaltyApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { useDebouncedState } from '../../hooks/useDebounced';
import { Field, FormGrid, FormModal, Note } from '../../components/ui';
import { ADJUSTMENT_REASON, pts } from '../../utils/loyalty';

/** Design 8.4: manual add or deduct with a reason. Deductions cannot take a balance below zero. */
export function AdjustModal({ customerId, onClose, onDone }: { customerId?: string; onClose: () => void; onDone: () => void }) {
  const [find, setFind, search] = useDebouncedState('');
  const members = useLoad(() => (customerId ? Promise.resolve([]) : customersApi.list({ q: search, size: 10 }).then(p => p.items)), [customerId, search]);
  const [member, setMember] = useState(customerId ?? '');
  const [direction, setDirection] = useState<1 | -1>(1);
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('GOODWILL');
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  const chosen = (members.data ?? []).find(m => m.id === member);

  async function submit() {
    const ok = await run(() => loyaltyApi.adjust({ customerId: member, points: direction * Number(amount), reason, note }), 'Points adjusted.');
    if (ok) onDone();
  }

  return (
    <FormModal title="New points adjustment" submitLabel={direction > 0 ? 'Add Points' : 'Deduct Points'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      {!customerId && (
        <Field label="Find member" hint="Name, phone or member code">
          <input className="input" value={find} onChange={e => setFind(e.target.value)} placeholder="Search…" autoFocus />
        </Field>
      )}
      {!customerId && (
        <Field label="Member" required hint={chosen ? `Balance: ${pts(chosen.points_balance)}` : members.data?.length === 10 ? 'Showing the first 10 matches; refine the search.' : undefined}>
          <select className="input" value={member} onChange={e => setMember(e.target.value)} required>
            <option value="">Choose a member…</option>{(members.data ?? []).map(m => <option key={m.id} value={m.id}>{m.full_name} ({m.code} · {m.phone})</option>)}
          </select>
        </Field>
      )}
      <FormGrid>
        <Field label="Type" required>
          <select className="input" value={direction} onChange={e => setDirection(Number(e.target.value) as 1 | -1)}>
            <option value={1}>Add points</option><option value={-1}>Deduct points</option>
          </select>
        </Field>
        <Field label="Points" required><input className="input" type="number" min={1} step={1} value={amount} onChange={e => setAmount(e.target.value)} required /></Field>
      </FormGrid>
      <Field label="Reason" required>
        <select className="input" value={reason} onChange={e => setReason(e.target.value)}>
          {Object.entries(ADJUSTMENT_REASON).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </Field>
      <Field label="Explanation" required><textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} required maxLength={500} /></Field>
      <Note>{direction > 0 ? 'Added points form a new batch that expires on the normal schedule.' : 'Deductions use the earliest-expiring points first and cannot go below zero.'}</Note>
    </FormModal>
  );
}
