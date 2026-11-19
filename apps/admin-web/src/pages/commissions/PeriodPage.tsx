import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Award, Coins, Lock, Scale, Wallet } from 'lucide-react';
import { earningsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ConfirmDialog, ErrorNote, LabelPill, Loading, Note, PageHeader, Pager, Panel, StatCard, StatRow } from '../../components/ui';
import { date, dateTime, num, xaf } from '../../utils/format';
import { signedXaf } from '../../utils/earnings';
import { StatementsTable } from './StatementsTable';

/** One earning period: totals and every seller's statement; finalise once the month has ended. */
export function PeriodPage() {
  const { id } = useParams();
  const { data: p, error, loading, reload } = useLoad(() => earningsApi.period(id!), [id]);
  const sellers = usePagedQuery((page, size) => earningsApi.periodSellers(id!, { page, size }), [id, p?.status]);
  const [confirming, setConfirming] = useState(false);
  const action = useAction();
  if (loading && !p) return <Loading />;
  if (!p) return <ErrorNote error={error} retry={reload} />;
  const month = new Date(`${p.starts_on.slice(0, 10)}T12:00:00Z`).toLocaleDateString('en-GB', { month: 'long', year: 'numeric' });
  const open = p.status === 'OPEN';
  return (
    <>
      <PageHeader title={`Earnings · ${month}`} badge={<LabelPill map={{ OPEN: ['Open', 'blue'], FINALIZED: ['Finalised', 'green'] }} value={p.status} />}
        subtitle={`${date(p.starts_on)} – ${date(p.ends_on)}${p.finalized_at ? ` · finalised ${dateTime(p.finalized_at)} by ${p.finalized_by_name}` : ''}`}
        crumbs={[['Commissions', '/commissions'], ['Earnings', '/commissions?tab=earnings'], [month]]}
        actions={p.can_finalize && <button className="btn gold" onClick={() => setConfirming(true)}><Lock size={16} />Finalise Period</button>} />
      {open && <Note>{p.can_finalize ? 'This month has ended and is ready to finalise.' : 'This month is still running: figures are live and will change until it is finalised.'}</Note>}
      <StatRow five>
        <StatCard tone="amber" icon={<Coins size={20} />} value={xaf(p.totals.commission_xaf)} label="Commissions" />
        <StatCard tone="purple" icon={<Award size={20} />} value={xaf(p.totals.bonus_xaf)} label="Target bonuses" />
        <StatCard tone="blue" icon={<Scale size={20} />} value={signedXaf(p.totals.adjustments_xaf)} label="Adjustments" />
        <StatCard tone="navy" icon={<Wallet size={20} />} value={xaf(p.totals.total_xaf)} label="Total earnings" />
        <StatCard tone="green" icon={<Wallet size={20} />} value={xaf(p.totals.payable_xaf)} label={`Payable (${num(p.totals.sellers)} sellers)`} />
      </StatRow>
      <Panel title="Seller Statements">
        <ErrorNote error={sellers.error} retry={sellers.reload} />
        {sellers.items.length ? <StatementsTable rows={sellers.items} /> : !sellers.loading && <p className="muted">No earnings in this period.</p>}
        <Pager {...sellers.pager} />
      </Panel>
      {confirming && (
        <ConfirmDialog title={`Finalise ${month}?`} confirmLabel="Finalise" busy={action.busy} onClose={() => setConfirming(false)}
          onConfirm={async () => { if (await action.run(() => earningsApi.finalize(p.id), `${month} finalised.`)) { setConfirming(false); reload(); } }}>
          <p>Every seller's statement is frozen as shown. Refunds after this are posted as adjustments in the current month, and negative totals are carried forward as recoveries.</p>
          <ErrorNote error={action.error} />
        </ConfirmDialog>
      )}
    </>
  );
}
