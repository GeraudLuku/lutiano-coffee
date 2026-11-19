import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Check, ClipboardList, Save, Send, X } from 'lucide-react';
import { inventoryApi, stocktakesApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Facts, Loading, Note, PageHeader, Panel, StatCard, StatRow, TitleCell, ReasonModal } from '../../components/ui';
import { ProductThumb } from '../../components/domain';
import { dateTime, num } from '../../utils/format';
import type { StocktakeDetail } from '../../types/models';
import { Delta, StocktakeStatusPill } from './InventoryBadges';
import { selfApprovalHint } from './ApprovalButtons';
import type { ReasonPrompt } from './useInventoryActions';

/** Count entry and approval for one stocktake. */
export function StocktakePage() {
  const { id } = useParams();
  const nav = useNavigate();
  const { session } = useAuth();
  const { data, error, loading, reload } = useLoad(() => stocktakesApi.get(id!), [id]);
  const policy = useLoad(() => inventoryApi.approvals(), []);
  const [st, setSt] = useState<StocktakeDetail | null>(null);
  const [counts, setCounts] = useState<Record<string, string>>({});
  const [prompt, setPrompt] = useState<ReasonPrompt | null>(null);
  const action = useAction();

  useEffect(() => { if (data) adopt(data); }, [data]);
  function adopt(d: StocktakeDetail) {
    setSt(d);
    setCounts(Object.fromEntries(d.lines.map(l => [l.product_id, l.counted_quantity == null ? '' : String(l.counted_quantity)])));
  }

  const dirty = useMemo(() => !!st && st.lines.some(l => (l.counted_quantity == null ? '' : String(l.counted_quantity)) !== (counts[l.product_id] ?? '')), [st, counts]);
  if (loading && !st) return <Loading />;
  if (!st) return <ErrorNote error={error} retry={reload} />;

  const counting = st.status === 'COUNTING', submitted = st.status === 'SUBMITTED';
  const mine = (policy.data?.secondApproverRequired ?? true) && st.submitted_by === session!.user.id;
  const counted = st.lines.filter(l => (counts[l.product_id] ?? '') !== '').length;
  const diffOf = (expected: number, value: string) => (value === '' ? null : Number(value) - expected);
  const differences = st.lines.filter(l => { const d = diffOf(l.expected_quantity, counts[l.product_id] ?? ''); return d != null && d !== 0; }).length;

  async function save() {
    const body = st!.lines.map(l => ({ productId: l.product_id, countedQuantity: counts[l.product_id] === '' ? null : Number(counts[l.product_id]) }));
    const updated = await action.run(() => stocktakesApi.saveCounts(st!.id, body), 'Counts saved.');
    if (updated) adopt(updated);
    return updated;
  }
  async function submit() {
    if (dirty && !(await save())) return;
    const updated = await action.run(() => stocktakesApi.submit(st!.id), 'Count submitted for approval.');
    if (updated) adopt(updated);
  }
  async function approve() {
    const updated = await action.run(() => stocktakesApi.approve(st!.id), 'Count approved; differences posted to stock.');
    if (updated) adopt(updated);
  }
  async function submitPrompt(note: string) {
    if (!prompt) return;
    const updated = await action.run(() => prompt.run(note) as Promise<StocktakeDetail>, prompt.success);
    if (updated) { adopt(updated); setPrompt(null); }
  }

  return (
    <>
      <PageHeader title={`Stock count — ${st.location_name}`} badge={<StocktakeStatusPill status={st.status} />}
        subtitle={`Started ${dateTime(st.created_at)} by ${st.created_by_name ?? 'unknown'}${st.note ? ` · ${st.note}` : ''}`}
        crumbs={[['Inventory', '/inventory?tab=stocktakes'], ['Stock count']]}
        actions={<>
          {counting && <button className="btn" disabled={!dirty || action.busy} onClick={save}><Save size={16} />Save counts</button>}
          {counting && <button className="btn gold" disabled={action.busy || counted < st.lines.length} onClick={submit}><Send size={16} />Submit for approval</button>}
          {submitted && <button className="btn" disabled={action.busy} onClick={() => setPrompt({ title: 'Send back for recount', label: 'What should be recounted?', confirm: 'Send back', run: n => stocktakesApi.reject(st.id, n), success: 'Count sent back for recounting.' })}><X size={16} />Reject</button>}
          {submitted && <button className="btn gold" disabled={action.busy || mine} title={mine ? selfApprovalHint : undefined} onClick={approve}><Check size={16} />Approve & post differences</button>}
          {(counting || submitted) && <button className="btn danger" disabled={action.busy} onClick={() => setPrompt({ title: 'Cancel stock count', label: 'Reason for cancelling', confirm: 'Cancel count', danger: true, run: n => stocktakesApi.cancel(st.id, n), success: 'Stock count cancelled.' })}>Cancel count</button>}
        </>} />
      <ErrorNote error={prompt ? undefined : action.error} />
      {counting && st.decision_note && <Note tone="warn">Sent back by {st.decided_by_name}: {st.decision_note}</Note>}
      {submitted && mine && <Note>You submitted this count, so another manager must approve it.</Note>}
      <StatRow>
        <StatCard tone="blue" icon={<ClipboardList size={20} />} value={`${num(counted)} / ${num(st.lines.length)}`} label="Products counted" />
        <StatCard tone={differences ? 'red' : 'green'} icon={<ClipboardList size={20} />} value={num(differences)} label="Products with a difference" />
        <StatCard tone="purple" icon={<ClipboardList size={20} />} value={st.submitted_by_name ?? '—'} label="Submitted by" />
        <StatCard tone="amber" icon={<ClipboardList size={20} />} value={st.decided_by_name && st.status === 'APPROVED' ? st.decided_by_name : '—'} label="Approved by" />
      </StatRow>
      <Panel title="Count sheet" icon={<ClipboardList size={19} />}
        action={counting && <span className="muted small">Enter 0 for products you cannot find. Expected quantities were recorded when the count started.</span>}>
        <DataTable rows={st.lines} rowKey={l => l.product_id} columns={[
          { header: 'Product', cell: l => <TitleCell lead={<ProductThumb url={l.image_url} size={36} />} title={l.product_name} sub={`${l.sku} · ${l.category}`} /> },
          { header: 'Expected', align: 'right', cell: l => num(l.expected_quantity) },
          { header: 'Counted', align: 'right', cell: l => counting
            ? <input className="input count-input" type="number" min={0} step={1} inputMode="numeric" aria-label={`Counted ${l.product_name}`}
                value={counts[l.product_id] ?? ''} onChange={e => setCounts(c => ({ ...c, [l.product_id]: e.target.value }))} />
            : (l.counted_quantity == null ? '—' : num(l.counted_quantity)) },
          { header: 'Difference', align: 'right', cell: l => { const d = diffOf(l.expected_quantity, counts[l.product_id] ?? ''); return d == null ? '—' : <Delta value={d} />; } },
        ]} />
      </Panel>
      {!counting && !submitted && (
        <Panel title="Outcome">
          <Facts items={[['Status', <StocktakeStatusPill status={st.status} />], ['Decided by', st.decided_by_name ?? '—'], ['Decided at', dateTime(st.decided_at)], ['Note', st.decision_note ?? '—']]} />
          <div className="form-actions"><button className="btn" onClick={() => nav('/inventory?tab=stocktakes')}>Back to stock counts</button></div>
        </Panel>
      )}
      {prompt && <ReasonModal prompt={prompt} busy={action.busy} error={action.error} onSubmit={submitPrompt} onClose={() => { setPrompt(null); action.setError(''); }} />}
    </>
  );
}
