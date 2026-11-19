import { Link } from 'react-router-dom';
import { ClipboardCheck, Scale, Send, Truck } from 'lucide-react';
import { DataTable, Empty, Note, Panel, TitleCell } from '../../../components/ui';
import { ago, num } from '../../../utils/format';
import { ADJUSTMENT_REASON } from '../../../utils/inventoryLabels';
import type { Approvals } from '../../../types/models';
import { AdjustmentActions, RequestActions, TransferActions } from '../ApprovalButtons';
import { Delta } from '../InventoryBadges';
import type { InventoryActions } from '../useInventoryActions';

/** Everything waiting for a manager, oldest decisions first in each section. */
export function ApprovalsTab({ data, actions }: { data: Approvals; actions: InventoryActions }) {
  const total = data.requestsToApprove.length + data.requestsToDispatch.length + data.transfersToApprove.length + data.transfersToDispatch.length + data.adjustmentsToApprove.length + data.stocktakesToApprove.length;
  if (total === 0) return <Panel><Empty title="Nothing waiting for approval" text="New transfer requests, adjustments and submitted stock counts will appear here." /></Panel>;
  return (
    <div className="stack">
      {data.secondApproverRequired && <Note>Requests you created must be approved by another manager; their Approve buttons are disabled for you.</Note>}
      {data.requestsToApprove.length > 0 && (
        <Panel title={`Stock requests to approve (${data.requestsToApprove.length})`} icon={<Truck size={19} />}>
          <DataTable rows={data.requestsToApprove} rowKey={r => r.id} columns={[
            { header: 'Request', cell: r => <TitleCell title={r.reference} sub={`${ago(r.created_at)} · ${r.requested_by_name ?? ''}`} /> },
            { header: 'To', cell: r => <Link className="link" to={`/locations/${r.operation_id}`}>{r.operation_name}</Link> },
            { header: 'Products', cell: r => r.items.map(i => (
              <div key={i.id}>{num(i.sent_quantity)} × {i.product_name}{i.central_quantity < i.sent_quantity && <small className="warn"> only {num(i.central_quantity)} in central stock</small>}</div>
            )) },
            { header: 'Units', align: 'right', cell: r => num(r.total_quantity) },
            { header: '', align: 'right', cell: r => <RequestActions r={r} actions={actions} /> },
          ]} />
        </Panel>
      )}
      {data.requestsToDispatch.length > 0 && (
        <Panel title={`Approved requests – ready to dispatch (${data.requestsToDispatch.length})`} icon={<Send size={19} />}>
          <DataTable rows={data.requestsToDispatch} rowKey={r => r.id} columns={[
            { header: 'Approved', cell: r => <TitleCell title={ago(r.approved_at)} sub={r.approved_by_name ?? undefined} /> },
            { header: 'Request', cell: r => <TitleCell title={r.reference} sub={r.operation_name} /> },
            { header: 'Products', cell: r => r.items.map(i => <div key={i.id}>{num(i.sent_quantity)} × {i.product_name}</div>) },
            { header: 'Units', align: 'right', cell: r => num(r.total_quantity) },
            { header: '', align: 'right', cell: r => <RequestActions r={r} actions={actions} /> },
          ]} />
        </Panel>
      )}
      {data.transfersToApprove.length > 0 && (
        <Panel title={`Transfer requests to approve (${data.transfersToApprove.length})`} icon={<Truck size={19} />}>
          <DataTable rows={data.transfersToApprove} rowKey={t => t.id} columns={[
            { header: 'Requested', cell: t => <TitleCell title={ago(t.created_at)} sub={t.created_by_name ?? undefined} /> },
            { header: 'Product', cell: t => <TitleCell title={t.product_name} sub={t.request_note ?? t.sku} /> },
            { header: 'To', cell: t => <Link className="link" to={`/locations/${t.operation_id}`}>{t.operation_name}</Link> },
            { header: 'Quantity', align: 'right', cell: t => num(t.sent_quantity) },
            { header: 'Central stock', align: 'right', cell: t => <span className={t.central_quantity < t.sent_quantity ? 'warn' : ''}>{num(t.central_quantity)}</span> },
            { header: '', align: 'right', cell: t => <TransferActions t={t} actions={actions} /> },
          ]} />
        </Panel>
      )}
      {data.transfersToDispatch.length > 0 && (
        <Panel title={`Approved – ready to dispatch (${data.transfersToDispatch.length})`} icon={<Send size={19} />}>
          <DataTable rows={data.transfersToDispatch} rowKey={t => t.id} columns={[
            { header: 'Approved', cell: t => <TitleCell title={ago(t.approved_at)} sub={t.approved_by_name ?? undefined} /> },
            { header: 'Product', cell: t => <TitleCell title={t.product_name} sub={t.sku} /> },
            { header: 'To', cell: t => t.operation_name },
            { header: 'Quantity', align: 'right', cell: t => num(t.sent_quantity) },
            { header: '', align: 'right', cell: t => <TransferActions t={t} actions={actions} /> },
          ]} />
        </Panel>
      )}
      {data.adjustmentsToApprove.length > 0 && (
        <Panel title={`Adjustments to approve (${data.adjustmentsToApprove.length})`} icon={<Scale size={19} />}>
          <DataTable rows={data.adjustmentsToApprove} rowKey={a => a.id} columns={[
            { header: 'Requested', cell: a => <TitleCell title={ago(a.created_at)} sub={a.requested_by_name ?? undefined} /> },
            { header: 'Location', cell: a => a.location_name },
            { header: 'Product', cell: a => <TitleCell title={a.product_name} sub={a.sku} /> },
            { header: 'Change', align: 'right', cell: a => <Delta value={a.quantity_delta} /> },
            { header: 'Reason', cell: a => <TitleCell title={ADJUSTMENT_REASON[a.reason][0]} sub={a.note} /> },
            { header: '', align: 'right', cell: a => <AdjustmentActions a={a} actions={actions} /> },
          ]} />
        </Panel>
      )}
      {data.stocktakesToApprove.length > 0 && (
        <Panel title={`Stock counts to approve (${data.stocktakesToApprove.length})`} icon={<ClipboardCheck size={19} />}>
          <DataTable rows={data.stocktakesToApprove} rowKey={s => s.id} columns={[
            { header: 'Submitted', cell: s => <TitleCell title={ago(s.submitted_at)} sub={s.submitted_by_name ?? undefined} /> },
            { header: 'Location', cell: s => s.location_name },
            { header: 'Products counted', align: 'right', cell: s => num(s.line_count) },
            { header: 'Differences', align: 'right', cell: s => <span className={s.variance_count ? 'warn' : ''}>{num(s.variance_count)}</span> },
            { header: '', align: 'right', cell: s => <Link className="btn sm gold" to={`/inventory/stocktakes/${s.id}`}>Review</Link> },
          ]} />
        </Panel>
      )}
    </div>
  );
}

export const approvalCount = (a: Approvals | undefined) =>
  a ? a.requestsToApprove.length + a.requestsToDispatch.length + a.transfersToApprove.length + a.transfersToDispatch.length + a.adjustmentsToApprove.length + a.stocktakesToApprove.length : 0;

