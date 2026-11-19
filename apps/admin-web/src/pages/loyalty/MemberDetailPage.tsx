import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { AlarmClock, Gift, History, Layers, Pencil, QrCode, RefreshCw, SlidersHorizontal, Star, User, Wallet } from 'lucide-react';
import { customersApi, loyaltyApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useObjectUrl } from '../../hooks/useObjectUrl';
import { ConfirmDialog, DataTable, ErrorNote, Facts, LabelPill, Loading, Note, PageHeader, Pager, Panel, ReasonModal, StatCard, StatRow, Tabs } from '../../components/ui';
import { date, dateTime, num, xaf } from '../../utils/format';
import { pts } from '../../utils/loyalty';
import { CustomerFormModal } from './CustomerFormModal';
import { RedeemModal } from './RedeemModal';
import { AdjustModal } from './AdjustModal';
import { LedgerTable } from './LedgerTable';
import { RedemptionsTable } from './RedemptionsTable';
import { TierBadge } from './TierBadge';

type Tab = 'ledger' | 'batches' | 'redemptions';

/** A member's balance, tier, point batches (with expiry), ledger and redemptions (A05/A10). */
export function MemberDetailPage() {
  const { id } = useParams();
  const { data: c, error, loading, reload: reloadMember } = useLoad(() => customersApi.get(id!), [id]);
  const ledger = usePagedQuery((page, size) => customersApi.ledger(id!, { page, size }), [id]);
  const batches = usePagedQuery((page, size) => customersApi.batches(id!, { page, size }), [id]);
  const redemptions = usePagedQuery((page, size) => customersApi.redemptions(id!, { page, size }), [id]);
  const reload = () => { reloadMember(); ledger.reload(); batches.reload(); redemptions.reload(); };
  const qr = useLoad(() => customersApi.qr(id!), [id, c?.loyalty_token]);
  const qrUrl = useObjectUrl(qr.data ?? null);
  const [tab, setTab] = useState<Tab>('ledger');
  const [modal, setModal] = useState<'' | 'edit' | 'redeem' | 'adjust' | 'reissue'>('');
  const [reversing, setReversing] = useState<string | null>(null);
  const action = useAction();

  if (loading && !c) return <Loading />;
  if (!c) return <ErrorNote error={error} retry={reloadMember} />;
  const done = () => { setModal(''); reload(); };
  const now = Date.now();

  return (
    <>
      <PageHeader title={c.full_name} badge={<TierBadge name={c.tier_name} color={c.tier_color} />} subtitle={`${c.code} · member since ${date(c.created_at)}`}
        crumbs={[['Loyalty', '/loyalty'], ['Members', '/loyalty?tab=members'], [c.full_name]]}
        actions={<>
          <button className="btn" onClick={() => setModal('edit')}><Pencil size={16} />Edit</button>
          <button className="btn" onClick={() => setModal('adjust')}><SlidersHorizontal size={16} />Adjust Points</button>
          <button className="btn gold" onClick={() => setModal('redeem')} disabled={c.status !== 'ACTIVE'}><Gift size={16} />Redeem Reward</button>
        </>} />
      {c.status !== 'ACTIVE' && <Note tone="warn">This account is suspended: it cannot earn or redeem points.</Note>}
      {c.expiring_soon > 0 && <Note tone="warn" icon={<AlarmClock size={16} />}>{pts(c.expiring_soon)} expire within {c.expiry_notice_days} days.</Note>}
      <StatRow>
        <StatCard tone="purple" icon={<Star size={20} />} value={pts(c.points_balance)} label="Available Points" />
        <StatCard tone="amber" icon={<AlarmClock size={20} />} value={pts(c.expiring_soon)} label={`Expiring in ${c.expiry_notice_days} days`} />
        <StatCard tone="green" icon={<Wallet size={20} />} value={xaf(c.lifetime_spend_xaf)} label={`Lifetime Spend (${num(c.purchases)} purchases)`} />
        <StatCard tone="blue" icon={<Gift size={20} />} value={num(c.rewards_redeemed)} label="Rewards Redeemed" />
      </StatRow>
      <div className="grid-2">
        <Panel title="Member Information" icon={<User size={19} />}>
          <Facts items={[
            ['Full name', c.full_name], ['Phone', <>{c.phone} {c.phone_verified ? <LabelPill map={{ Y: ['Verified', 'green'] }} value="Y" /> : <LabelPill map={{ N: ['Not verified', 'amber'] }} value="N" />}</>],
            ['Member code', c.code], ['Tier', <TierBadge name={c.tier_name} color={c.tier_color} />],
            ['Status', <LabelPill map={{ ACTIVE: ['Active', 'green'], SUSPENDED: ['Suspended', 'red'] }} value={c.status} />],
            ['Last purchase', c.last_purchase_at ? dateTime(c.last_purchase_at) : 'None yet'],
          ]} />
        </Panel>
        <Panel title="Loyalty QR" icon={<QrCode size={19} />} action={<button className="btn sm" onClick={() => setModal('reissue')}><RefreshCw size={14} />Reissue</button>}>
          <div className="member-qr">
            {qrUrl ? <img src={qrUrl} alt={`Loyalty QR code for ${c.full_name}`} width={180} height={180} /> : <Loading />}
            <p className="muted small">The member shows this QR at checkout to collect points. It belongs in their customer app; reissue it if it was lost or shared (the old one stops working).</p>
          </div>
        </Panel>
      </div>
      <Tabs value={tab} onChange={setTab} tabs={[['ledger', `Points Ledger (${num(ledger.pager.total)})`], ['batches', `Point Batches (${num(batches.pager.total)})`],
        ['redemptions', `Redemptions (${num(redemptions.pager.total)})`]]} />
      <Panel>
        {tab === 'ledger' && <>
          <ErrorNote error={ledger.error} retry={ledger.reload} />
          {ledger.items.length ? <LedgerTable rows={ledger.items} /> : !ledger.loading && <p className="muted">No points activity yet.</p>}
          <Pager {...ledger.pager} />
        </>}
        {tab === 'batches' && <>
          <ErrorNote error={batches.error} retry={batches.reload} />
          {batches.items.length ? (
          <DataTable rows={batches.items} rowKey={b => b.id} columns={[
            { header: 'Earned', cell: b => dateTime(b.earned_at) },
            { header: 'Source', cell: b => (b.source_type === 'SALE' ? 'Purchase' : b.source_type === 'ADJUSTMENT' ? 'Adjustment' : b.source_type) },
            { header: 'Points', align: 'right', cell: b => pts(b.points) },
            { header: 'Remaining', align: 'right', cell: b => <b>{pts(b.remaining)}</b> },
            { header: 'Expires', cell: b => date(b.expires_at) },
            { header: 'Status', cell: b => (new Date(b.expires_at).getTime() <= now ? <LabelPill map={{ X: ['Expired', 'grey'] }} value="X" />
              : b.remaining === 0 ? <LabelPill map={{ U: ['Used', 'blue'] }} value="U" /> : <LabelPill map={{ A: ['Available', 'green'] }} value="A" />) },
          ]} />
          ) : !batches.loading && <p className="muted">No points earned yet.</p>}
          <Pager {...batches.pager} />
        </>}
        {tab === 'redemptions' && <>
          <ErrorNote error={redemptions.error} retry={redemptions.reload} />
          {redemptions.items.length ? <RedemptionsTable rows={redemptions.items} onReverse={setReversing} /> : !redemptions.loading && <p className="muted">No redemptions yet.</p>}
          <Pager {...redemptions.pager} />
        </>}
        <p className="muted small"><Layers size={13} aria-hidden="true" /> Points expire by batch; redemptions use the earliest-expiring points first. <History size={13} aria-hidden="true" /> The ledger is permanent.</p>
      </Panel>

      {modal === 'edit' && <CustomerFormModal customer={c} onClose={() => setModal('')} onDone={done} />}
      {modal === 'redeem' && <RedeemModal customer={c} onClose={() => setModal('')} onDone={done} />}
      {modal === 'adjust' && <AdjustModal customerId={c.id} onClose={() => setModal('')} onDone={done} />}
      {modal === 'reissue' && (
        <ConfirmDialog title="Reissue loyalty QR?" confirmLabel="Reissue QR" busy={action.busy} onClose={() => setModal('')}
          onConfirm={async () => { if (await action.run(() => customersApi.reissueQr(c.id), 'New loyalty QR issued.')) done(); }}>
          <p>The current QR stops working immediately. Use this if the member lost their phone or the QR was shared.</p>
        </ConfirmDialog>
      )}
      {reversing && (
        <ReasonModal prompt={{ title: 'Reverse redemption', label: 'Reason', confirm: 'Reverse' }} busy={action.busy} error={action.error}
          onClose={() => { setReversing(null); action.setError(''); }}
          onSubmit={async reason => { if (await action.run(() => loyaltyApi.reverseRedemption(reversing, reason), 'Redemption reversed. Points returned.')) { setReversing(null); reload(); } }} />
      )}
    </>
  );
}
