import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Award, Coins, HandCoins, Plus, Scale, Smartphone, Target, Wallet } from 'lucide-react';
import { commissionsApi, earningsApi, payoutsApi, targetsApi } from '../../../api/services';
import { useLoad } from '../../../hooks/useLoad';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { DataTable, ErrorNote, Pager, Panel, StatCard, StatRow, Tabs, TitleCell } from '../../../components/ui';
import { dateTime, num, xaf } from '../../../utils/format';
import { rateLabel, signedXaf } from '../../../utils/earnings';
import type { StaffDetail } from '../../../types/models';
import { DestinationModal } from '../../payouts/DestinationModal';
import { DestinationStatusPill } from '../../payouts/PayoutPills';
import { PayoutsList } from '../../payouts/PayoutsList';
import { RecordPayoutModal } from '../../payouts/RecordPayoutModal';
import { PAYOUT_METHOD } from '../../../utils/payouts';
import { StatementsTable } from '../../commissions/StatementsTable';
import { TargetsTable } from '../../commissions/TargetsTable';
import { TargetModal } from '../../commissions/TargetModal';
import { AdjustmentsTab } from '../../commissions/AdjustmentsTab';

type View = 'commissions' | 'targets' | 'statements' | 'adjustments' | 'payouts';

/** A seller's earnings: this month so far, commission lines, targets, monthly statements and adjustments. */
export function SellerEarningsTab({ user }: { user: StaffDetail }) {
  const summary = useLoad(() => earningsApi.seller(user.id), [user.id]);
  const [view, setView] = useState<View>('commissions');
  const [adding, setAdding] = useState(false);
  const [paying, setPaying] = useState(false);
  const [addingNumber, setAddingNumber] = useState(false);
  const balance = useLoad(() => payoutsApi.balance(user.id), [user.id]);
  const lines = usePagedQuery((page, size) => commissionsApi.lines({ sellerId: user.id, page, size }), [user.id]);
  const targets = usePagedQuery((page, size) => targetsApi.list({ sellerId: user.id, page, size }), [user.id]);
  const statements = usePagedQuery((page, size) => earningsApi.statements(user.id, { page, size }), [user.id]);
  const c = summary.data?.current ?? {};
  return (
    <>
      <ErrorNote error={summary.error} retry={summary.reload} />
      <StatRow>
        <StatCard tone="amber" icon={<Coins size={20} />} value={xaf(c.commission_xaf ?? 0)} label="Commission this month" />
        <StatCard tone="purple" icon={<Award size={20} />} value={xaf(c.bonus_xaf ?? 0)} label="Target bonuses this month" />
        <StatCard tone="blue" icon={<Scale size={20} />} value={signedXaf(Number(c.adjustments_xaf ?? 0))} label="Adjustments this month" />
        <StatCard tone="green" icon={<Wallet size={20} />} value={xaf(c.total_xaf ?? 0)} label="Earned so far (live)" />
      </StatRow>
      <Tabs value={view} onChange={setView} tabs={[['commissions', `Commission Lines (${num(lines.pager.total)})`], ['targets', `Targets (${num(targets.pager.total)})`],
        ['statements', `Monthly Statements (${num(statements.pager.total)})`], ['adjustments', 'Adjustments'], ['payouts', 'Payouts']]} />
      {view === 'commissions' && (
        <Panel title={`Commission per sale line${lines.summary ? ` · ${xaf(lines.summary.commission_xaf)} on ${xaf(lines.summary.sales_xaf)}` : ''}`} icon={<Coins size={19} />}>
          <ErrorNote error={lines.error} retry={lines.reload} />
          {lines.items.length ? (
            <DataTable rows={lines.items} rowKey={l => `${l.sale_id}-${l.line_no}`} columns={[
              { header: 'Sale', cell: l => <TitleCell title={<Link className="link" to={`/sales/${l.sale_id}`}>{l.sale_reference}</Link>} sub={dateTime(l.sale_created_at)} /> },
              { header: 'Product', cell: l => <TitleCell title={l.product_name} sub={l.operation_name} /> },
              { header: 'Line value', align: 'right', cell: l => <TitleCell title={xaf(l.line_value_xaf)} sub={`${num(l.quantity)} units`} /> },
              { header: 'Rate', cell: l => rateLabel(l.basis, l.rate) },
              { header: 'Commission', align: 'right', cell: l => <TitleCell title={<b>{xaf(l.commission_xaf)}</b>} sub={l.reversed_xaf ? `${xaf(l.reversed_xaf)} reversed` : undefined} /> },
            ]} />
          ) : !lines.loading && <p className="muted">No commission yet. Set rates in Commissions → Commission Rules.</p>}
          <Pager {...lines.pager} />
        </Panel>
      )}
      {view === 'targets' && (
        <Panel title="Targets" icon={<Target size={19} />} action={<button className="btn gold sm" onClick={() => setAdding(true)}><Plus size={15} />New Target</button>}>
          {targets.items.length ? <TargetsTable rows={targets.items} /> : !targets.loading && <p className="muted">No targets for this seller.</p>}
          <Pager {...targets.pager} />
        </Panel>
      )}
      {view === 'statements' && (
        <Panel title="Monthly Statements" icon={<Wallet size={19} />}>
          {statements.items.length ? <StatementsTable rows={statements.items} byPeriod /> : !statements.loading && <p className="muted">No finalised months yet.</p>}
          <Pager {...statements.pager} />
        </Panel>
      )}
      {view === 'adjustments' && <AdjustmentsTab sellerId={user.id} />}
      {view === 'payouts' && (
        <>
          <StatRow>
            <StatCard tone="purple" icon={<Wallet size={20} />} value={xaf(balance.data?.owed_xaf ?? 0)} label="Earned (finalised months)" />
            <StatCard tone="green" icon={<HandCoins size={20} />} value={xaf(balance.data?.paid_xaf ?? 0)} label="Paid" />
            <StatCard tone="blue" icon={<Scale size={20} />} value={xaf(balance.data?.pending_xaf ?? 0)} label="Recorded, not yet paid" />
            <StatCard tone="amber" icon={<Coins size={20} />} value={xaf(balance.data?.outstanding_xaf ?? 0)} label="Outstanding" />
          </StatRow>
          <Panel title="Payout numbers" icon={<Smartphone size={19} />} action={<button className="btn sm" onClick={() => setAddingNumber(true)}><Plus size={15} />Add Number</button>}>
            {balance.data?.destinations?.length ? (
              <ul className="check-list">{balance.data.destinations.map(d => (
                <li key={d.id}><span>{PAYOUT_METHOD[d.provider]} · <b>{d.phone}</b><small className="block">{d.account_name}</small></span><DestinationStatusPill status={d.status} /></li>
              ))}</ul>
            ) : <p className="muted">No mobile money number yet. Cash payouts need none.</p>}
          </Panel>
          <div className="toolbar-end">
            <button className="btn gold sm" disabled={!balance.data?.outstanding_xaf} onClick={() => setPaying(true)}><Plus size={15} />Record Payout</button>
          </div>
          <PayoutsList sellerId={user.id} />
        </>
      )}
      {paying && <RecordPayoutModal sellerId={user.id} onClose={() => setPaying(false)} onDone={() => { setPaying(false); balance.reload(); }} />}
      {addingNumber && <DestinationModal sellerId={user.id} onClose={() => setAddingNumber(false)} onDone={() => { setAddingNumber(false); balance.reload(); }} />}
      {adding && <TargetModal sellerId={user.id} onClose={() => setAdding(false)} onDone={() => { setAdding(false); targets.reload(); summary.reload(); }} />}
    </>
  );
}
