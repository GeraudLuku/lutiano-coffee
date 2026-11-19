import { useSearchParams } from 'react-router-dom';
import { AlertTriangle, ArrowUpRight, ClipboardCheck, HandCoins, Timer, Wallet } from 'lucide-react';
import { cashApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ErrorNote, Note, PageHeader, Pager, Panel, StatCard, StatRow, Tabs, ListState } from '../../components/ui';
import { num, xaf } from '../../utils/format';
import { OutflowsTable, RemittancesTable, ShiftsTable } from './CashTables';
import { useCashActions } from './useCashActions';

type Tab = 'open' | 'review' | 'closed' | 'outflows' | 'remittances';
const TABS: Tab[] = ['open', 'review', 'closed', 'outflows', 'remittances'];

/**
 * A09 Cash Management: seller cash shifts and their custody. Expected cash = opening float + cash sales − refunds paid
 * − approved outflows − remittances. Remittances stay outstanding until the recipient acknowledges them.
 */
export function CashManagementPage() {
  const [params, setParams] = useSearchParams();
  const tab = TABS.includes(params.get('tab') as Tab) ? (params.get('tab') as Tab) : 'open';
  const summary = useLoad(() => cashApi.summary(), []);
  const s = summary.data;

  return (
    <>
      <PageHeader title="Cash Management" subtitle="Cash shifts, outflows, remittances and variance review for every seller."
        crumbs={[['Sales', '/sales'], ['Cash Management']]} />
      <ErrorNote error={summary.error} retry={summary.reload} />
      <StatRow five>
        <StatCard tone="green" icon={<Timer size={20} />} value={s ? num(s.open_shifts) : '—'} label="Open Shifts" />
        <StatCard tone="blue" icon={<Wallet size={20} />} value={s ? xaf(s.cash_in_open_shifts_xaf) : '—'} label="Cash in Open Drawers" />
        <StatCard tone="red" icon={<ClipboardCheck size={20} />} value={s ? num(s.shifts_to_review) : '—'} label="Shifts to Review" />
        <StatCard tone="amber" icon={<ArrowUpRight size={20} />} value={s ? num(s.pending_outflows) : '—'} label="Outflows Awaiting Approval" />
        <StatCard tone="purple" icon={<HandCoins size={20} />} value={s ? xaf(s.outstanding_remittances_xaf) : '—'} label={`Outstanding Remittances (${s ? num(s.outstanding_remittances) : 0})`} />
      </StatRow>
      {s && s.disputed_remittances > 0 && (
        <Note tone="warn" icon={<AlertTriangle size={16} />}>{num(s.disputed_remittances)} remittance{s.disputed_remittances === 1 ? '' : 's'} received a different amount than the seller sent. See Remittances.</Note>
      )}
      <Tabs value={tab} onChange={t => setParams({ tab: t }, { replace: true })} tabs={[
        ['open', `Open Shifts${s ? ` (${s.open_shifts})` : ''}`], ['review', `Variance Review${s?.shifts_to_review ? ` (${s.shifts_to_review})` : ''}`],
        ['closed', 'Closed Shifts'], ['outflows', `Outflows${s?.pending_outflows ? ` (${s.pending_outflows})` : ''}`],
        ['remittances', `Remittances${s?.outstanding_remittances ? ` (${s.outstanding_remittances})` : ''}`],
      ]} />
      {tab === 'open' && <ShiftsTab status="OPEN" />}
      {tab === 'review' && <ShiftsTab review />}
      {tab === 'closed' && <ShiftsTab status="CLOSED" />}
      {tab === 'outflows' && <OutflowsTab onChanged={summary.reload} />}
      {tab === 'remittances' && <RemittancesTab onChanged={summary.reload} />}
    </>
  );
}

function ShiftsTab({ status, review }: { status?: 'OPEN' | 'CLOSED'; review?: boolean }) {
  const { data, items, error, loading, reload, pager } = usePagedQuery(
    (page, size) => cashApi.shifts({ status: review ? 'CLOSED' : status, review: review ? 'OPEN_REVIEW' : undefined, page, size }), [status, review]);
  return (
    <Panel>
      <ErrorNote error={error} retry={reload} />
      {review && <p className="muted small">Shifts closed with a difference between counted and expected cash. Accept the variance with a note, or ask the seller to count again.</p>}
      <ListState loading={loading} data={data} items={items} empty={{ title: review ? 'No variances to review' : 'No shifts to show' }}><ShiftsTable rows={items} /></ListState>
      <Pager {...pager} />
    </Panel>
  );
}

function OutflowsTab({ onChanged }: { onChanged: () => void }) {
  const { data, items, error, loading, reload, pager } = usePagedQuery((page, size) => cashApi.outflows({ page, size }), []);
  const actions = useCashActions(() => { reload(); onChanged(); });
  return (
    <Panel>
      <ErrorNote error={error} retry={reload} />
      <p className="muted small">Cash a seller takes from the drawer for a business expense. It leaves the drawer only once an administrator approves it.</p>
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No outflows yet' }}><OutflowsTable rows={items} actions={actions.outflowActions} /></ListState>
      <Pager {...pager} />
      {actions.modals}
    </Panel>
  );
}

function RemittancesTab({ onChanged }: { onChanged: () => void }) {
  const { data, items, error, loading, reload, pager } = usePagedQuery((page, size) => cashApi.remittances({ page, size }), []);
  const actions = useCashActions(() => { reload(); onChanged(); });
  return (
    <Panel>
      <ErrorNote error={error} retry={reload} />
      <p className="muted small">Cash handed to a manager or deposited at a bank. It stays outstanding until the person who received it, or an administrator for bank deposits, confirms the amount.</p>
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No remittances yet' }}><RemittancesTable rows={items} actions={actions.remittanceActions} /></ListState>
      <Pager {...pager} />
      {actions.modals}
    </Panel>
  );
}
