import { useState } from 'react';
import { Plus } from 'lucide-react';
import { campaignsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ConfirmDialog, DataTable, ErrorNote, FilterBar, LabelPill, ListState, Note, Pager, Panel, TitleCell } from '../../components/ui';
import { date, xaf } from '../../utils/format';
import { CampaignModal } from './CampaignModal';
import { CampaignStandings } from './CampaignStandings';
import type { Campaign } from '../../types/models';

const STATE: Record<string, [string, string]> = { CURRENT: ['In progress', 'blue'], UPCOMING: ['Upcoming', 'grey'], ENDED: ['Ended', 'navy'], CANCELLED: ['Cancelled', 'grey'], SETTLED: ['Bonuses paid', 'green'] };
const statusOf = (c: Campaign) => (c.status === 'CANCELLED' ? 'CANCELLED' : c.settled ? 'SETTLED' : c.state);

/** Sales campaigns with tiered bonuses: set one up, follow the standings, pay the bonuses once it is over. */
export function CampaignsTab() {
  const [state, setState] = useState('');
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => campaignsApi.list({ state, page, size }), [state]);
  const [adding, setAdding] = useState(false);
  const [viewing, setViewing] = useState<Campaign | null>(null);
  const [confirm, setConfirm] = useState<{ campaign: Campaign; kind: 'settle' | 'cancel' } | null>(null);
  const action = useAction();

  async function confirmed() {
    if (!confirm) return;
    const { campaign, kind } = confirm;
    const ok = await action.run(() => (kind === 'settle' ? campaignsApi.settle(campaign.id) : campaignsApi.cancel(campaign.id)), kind === 'settle' ? 'Bonuses paid.' : 'Campaign cancelled.');
    if (ok) { setConfirm(null); reload(); }
  }

  return (
    <Panel title="Sales Campaigns" action={<button className="btn gold sm" onClick={() => setAdding(true)}><Plus size={15} />New Campaign</button>}>
      <FilterBar>
        <select className="input" value={state} onChange={e => setState(e.target.value)} aria-label="State">
          <option value="">All campaigns</option><option value="CURRENT">In progress</option><option value="UPCOMING">Upcoming</option><option value="ENDED">Ended or cancelled</option>
        </select>
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No campaigns', text: 'Reward sellers with a bonus when their sales reach a tier.' }}>
        <DataTable rows={items} rowKey={c => c.id} columns={[
          { header: 'Campaign', cell: c => <TitleCell title={c.name} sub={c.description ?? ''} /> },
          { header: 'Period', cell: c => <TitleCell title={`${date(c.starts_on)} – ${date(c.ends_on)}`} sub={c.operation_name ?? 'All sellers'} /> },
          { header: 'Status', cell: c => <LabelPill map={STATE} value={statusOf(c)} /> },
          { header: '', cell: c => (
            <div className="row-actions">
              <button className="btn sm" onClick={() => setViewing(c)}>Standings</button>
              {c.status === 'ACTIVE' && !c.settled && c.state === 'ENDED' && <button className="btn gold sm" onClick={() => setConfirm({ campaign: c, kind: 'settle' })}>Pay bonuses</button>}
              {c.status === 'ACTIVE' && !c.settled && c.state !== 'ENDED' && <button className="btn sm" onClick={() => setConfirm({ campaign: c, kind: 'cancel' })}>Cancel</button>}
            </div>
          ) },
        ]} />
      </ListState>
      <Pager {...pager} />
      {adding && <CampaignModal onClose={() => setAdding(false)} onDone={() => { setAdding(false); reload(); }} />}
      {viewing && <CampaignStandings campaign={viewing} onClose={() => setViewing(null)} />}
      {confirm && (
        <ConfirmDialog title={confirm.kind === 'settle' ? 'Pay campaign bonuses?' : 'Cancel campaign?'} confirmLabel={confirm.kind === 'settle' ? 'Pay Bonuses' : 'Cancel Campaign'}
          danger={confirm.kind === 'cancel'} busy={action.busy} onConfirm={confirmed} onClose={() => { setConfirm(null); action.setError(''); }}>
          {confirm.kind === 'settle'
            ? <p>Each seller who reached a tier of <b>{confirm.campaign.name}</b> gets the bonus of the highest tier reached, added to this month&apos;s earnings. This can be done only once and cannot be undone ({(confirm.campaign.tiers ?? []).length ? `top bonus ${xaf(Math.max(...(confirm.campaign.tiers ?? []).map(t => t.bonus_xaf)))}` : 'see standings first'}).</p>
            : <p><b>{confirm.campaign.name}</b> will stop and no bonus will be paid for it.</p>}
          {action.error && <div className="error-note" role="alert">{action.error}</div>}
          {confirm.kind === 'settle' && <Note>Refunds made after the bonuses are paid do not take a bonus back.</Note>}
        </ConfirmDialog>
      )}
    </Panel>
  );
}
