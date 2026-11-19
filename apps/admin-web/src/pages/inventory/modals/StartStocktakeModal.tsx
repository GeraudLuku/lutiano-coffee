import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { stocktakesApi } from '../../../api/services';
import { useAction } from '../../../hooks/useAction';
import { Field, FormModal, Note } from '../../../components/ui';
import { CENTRAL } from '../../../utils/inventoryLabels';
import type { Operation } from '../../../types/models';

/** Starts a count: the expected quantities are recorded now, then you enter what you find. */
export function StartStocktakeModal({ operations, onClose }: { operations: Operation[]; onClose: () => void }) {
  const nav = useNavigate();
  const [location, setLocation] = useState(CENTRAL);
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();

  async function submit() {
    const st = await run(() => stocktakesApi.start(location === CENTRAL ? null : location, note), 'Stock count started.');
    if (st) nav(`/inventory/stocktakes/${st.id}`);
  }

  return (
    <FormModal title="Start a stock count" submitLabel="Start Count" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Location to count" required>
        <select className="input" value={location} onChange={e => setLocation(e.target.value)}>
          <option value={CENTRAL}>Central stock</option>
          {operations.filter(o => o.active).map(o => <option key={o.id} value={o.id}>{o.name} ({o.code})</option>)}
        </select>
      </Field>
      <Field label="Note"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={500} placeholder="e.g. Month-end count" /></Field>
      <Note>The system records the expected quantities now. Sales made during the count are kept: only the differences you find are posted when the count is approved.</Note>
    </FormModal>
  );
}
