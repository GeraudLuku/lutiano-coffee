import { Link } from 'react-router-dom';
import { BarChart3, FileCheck2, Package, ShoppingCart, Timer, Wallet } from 'lucide-react';
import { DataTable, Kpi, KpiRow, Note, Panel, Pill } from '../../../components/ui';
import { EligibilityPill, TypePill } from '../../../components/domain';
import { ago, dateTime, num, xaf } from '../../../utils/format';
import { DOCUMENT_TYPE_LABEL } from '../../../utils/labels';
import type { StaffDetail } from '../../../types/models';

/** Performance summary (last 30 days), sales by location, the current cash shift and eligibility. */
export function SellerPerformance({ user, onOpenDocuments }: { user: StaffDetail; onOpenDocuments: () => void }) {
  const p = user.performance;
  const avg = p.sales ? Math.round(Number(p.revenue_xaf) / Number(p.sales)) : 0;
  const e = user.eligibility;
  return (
    <>
      <Panel title={<>Performance Summary <small>(Last 30 Days)</small></>} icon={<BarChart3 size={19} />} className="span-3"
        action={<Link className="link" to={`/sales?seller=${user.id}`}>All sales</Link>}>
        <KpiRow>
          <Kpi tone="navy" icon={<ShoppingCart />} value={num(p.sales)} label="Total Sales" />
          <Kpi tone="gold" icon={<Wallet />} value={xaf(p.revenue_xaf)} label="Sales Revenue" />
          <Kpi tone="blue" icon={<Package />} value={num(p.items)} label="Items Sold" />
          <Kpi tone="purple" icon={<Wallet />} value={xaf(avg)} label="Average Sale" />
        </KpiRow>
        {(user.performanceByOperation ?? []).length > 0 && (
          <DataTable rows={user.performanceByOperation!} rowKey={r => r.operation_id} columns={[
            { header: 'Sales location', cell: r => <><Link className="link" to={`/locations/${r.operation_id}`}>{r.operation_name}</Link> <TypePill type={r.operation_type} /></> },
            { header: 'Sales', align: 'right', cell: r => num(r.sales) },
            { header: 'Revenue', align: 'right', cell: r => xaf(r.revenue_xaf) },
          ]} />
        )}
        <p className="muted small">Last sale: {p.last_sale_at ? `${dateTime(p.last_sale_at)} (${ago(p.last_sale_at)})` : 'never'}.</p>
      </Panel>
      <Panel title="Cash Shift" icon={<Timer size={19} />}>
        {user.openShift ? (
          <>
            <p><Pill tone="green">Open</Pill> since {dateTime(user.openShift.opened_at)}</p>
            <p className="muted">Opening float {xaf(user.openShift.opening_float)} · expected cash {xaf(user.openShift.expected_cash)}</p>
            <Link className="link" to="/sales/shifts">Cash shifts</Link>
          </>
        ) : <p className="muted">No cash shift open.</p>}
      </Panel>
      <Panel title="Eligibility" icon={<FileCheck2 size={19} />} action={<button className="link" onClick={onOpenDocuments}>Documents</button>}>
        <p><EligibilityPill eligibility={e} /></p>
        {e && !e.eligible && (
          <ul className="plain-list">
            {e.missing.map(t => <li key={t}>Missing: {DOCUMENT_TYPE_LABEL[t]}</li>)}
            {e.pending.map(t => <li key={t}>Awaiting review: {DOCUMENT_TYPE_LABEL[t]}</li>)}
            {e.expired.map(t => <li key={t}>Expired: {DOCUMENT_TYPE_LABEL[t]}</li>)}
          </ul>
        )}
        {e && <p className="muted small">Required: {e.required.map(t => DOCUMENT_TYPE_LABEL[t]).join(', ') || 'none'}.</p>}
        {!user.eligibilityEnforced && e && !e.eligible && <Note>Eligibility is shown for review only; it does not block assignments yet.</Note>}
      </Panel>
    </>
  );
}
