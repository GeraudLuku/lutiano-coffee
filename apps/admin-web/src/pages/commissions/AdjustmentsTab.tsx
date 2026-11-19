import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { earningsApi, staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, Field, FilterBar, FormGrid, FormModal, LabelPill, Note, Pager, Panel, TitleCell, ListState } from '../../components/ui';
import { dateTime, num } from '../../utils/format';
import { ADJUSTMENT_TYPE, signedXaf } from '../../utils/earnings';
import type { EarningAdjustmentType } from '../../types/models';

/** Every change to earnings outside sale commissions: refund and bonus reversals, recoveries and manual corrections. */
export function AdjustmentsTab({ sellerId }: { sellerId?: string }) {
  const [type, setType] = useState<EarningAdjustmentType | ''>('');
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => earningsApi.adjustments({ sellerId, type, page, size }), [sellerId, type]);
  const [adding, setAdding] = useState(false);
  return (
    <Panel title={`Earnings Adjustments${summary ? ` · ${num(summary.adjustments)} totalling ${signedXaf(Number(summary.amount_xaf))}` : ''}`}
      action={<button className="btn gold sm" onClick={() => setAdding(true)}><Plus size={15} />New Adjustment</button>}>
      <FilterBar>
        <select className="input" value={type} onChange={e => setType(e.target.value as EarningAdjustmentType | '')} aria-label="Type">
          <option value="">All Types</option>{Object.entries(ADJUSTMENT_TYPE).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}
        </select>
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No adjustments' }}>
        <DataTable rows={items} rowKey={a => a.id} columns={[
          { header: 'Adjustment', cell: a => <TitleCell title={a.reference} sub={dateTime(a.created_at)} /> },
          ...(sellerId ? [] : [{ header: 'Seller', cell: (a: typeof items[number]) => <Link className="link" to={`/users/${a.seller_id}?tab=earnings`}>{a.seller_name}</Link> }]),
          { header: 'Type', cell: a => <LabelPill map={ADJUSTMENT_TYPE} value={a.adjustment_type} /> },
          { header: 'Reason', cell: a => <TitleCell title={a.note ?? '—'} sub={a.source_type === 'REFUND' && a.source_id ? <Link className="link" to={`/sales/refunds/${a.source_id}`}>Open refund</Link> : a.created_by_name ?? undefined} /> },
          { header: 'Amount', align: 'right', cell: a => <b className={a.amount_xaf < 0 ? 'warn' : 'ok'}>{signedXaf(Number(a.amount_xaf))}</b> },
        ]} />
      </ListState>
      <Pager {...pager} />
      {adding && <AdjustModal sellerId={sellerId} onClose={() => setAdding(false)} onDone={() => { setAdding(false); reload(); }} />}
    </Panel>
  );
}

function AdjustModal({ sellerId, onClose, onDone }: { sellerId?: string; onClose: () => void; onDone: () => void }) {
  const sellers = useLoad(() => (sellerId ? Promise.resolve([]) : staffApi.all().then(all => all.filter(s => s.role === 'SELLER'))), [sellerId]);
  const [seller, setSeller] = useState(sellerId ?? '');
  const [direction, setDirection] = useState<1 | -1>(1);
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  async function submit() {
    if (await run(() => earningsApi.adjust({ sellerId: seller, amountXaf: direction * Number(amount), note }), 'Adjustment posted.')) onDone();
  }
  return (
    <FormModal title="New earnings adjustment" submitLabel={direction > 0 ? 'Add to Earnings' : 'Deduct from Earnings'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      {!sellerId && (
        <Field label="Seller" required>
          <select className="input" value={seller} onChange={e => setSeller(e.target.value)} required>
            <option value="">Choose a seller…</option>{(sellers.data ?? []).map(s => <option key={s.id} value={s.id}>{s.full_name}</option>)}
          </select>
        </Field>
      )}
      <FormGrid>
        <Field label="Type" required>
          <select className="input" value={direction} onChange={e => setDirection(Number(e.target.value) as 1 | -1)}>
            <option value={1}>Add (bonus, correction)</option><option value={-1}>Deduct (recovery, correction)</option>
          </select>
        </Field>
        <Field label="Amount (XAF)" required><input className="input" type="number" min={1} value={amount} onChange={e => setAmount(e.target.value)} required /></Field>
      </FormGrid>
      <Field label="Reason" required><textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} required maxLength={500} /></Field>
      <Note>Posted in the current month. Finalised months are never changed; a deduction that exceeds a month's earnings is carried forward as a recovery.</Note>
    </FormModal>
  );
}
