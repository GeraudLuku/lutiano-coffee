import { Link } from 'react-router-dom';
import { Undo2 } from 'lucide-react';
import { DataTable, LabelPill, TitleCell } from '../../components/ui';
import { dateTime } from '../../utils/format';
import { REWARD_TYPE, pts } from '../../utils/loyalty';
import type { Redemption } from '../../types/models';

export function RedemptionsTable({ rows, withCustomer, onReverse }: { rows: Redemption[]; withCustomer?: boolean; onReverse?: (id: string) => void }) {
  return (
    <DataTable rows={rows} rowKey={x => x.id} columns={[
      { header: 'Redemption', cell: x => <TitleCell title={x.reference} sub={dateTime(x.created_at)} /> },
      ...(withCustomer ? [{ header: 'Member', cell: (x: Redemption) => <Link className="link" to={`/loyalty/members/${x.customer_id}`}>{x.customer_name}</Link> }] : []),
      { header: 'Reward', cell: x => <TitleCell title={x.reward_name} sub={<LabelPill map={REWARD_TYPE} value={x.reward_type} />} /> },
      { header: 'Points', align: 'right', cell: x => <b>{pts(x.points)}</b> },
      { header: 'Stock from', cell: x => x.operation_name ?? '—' },
      { header: 'By', cell: x => x.created_by_name },
      { header: 'Status', cell: x => <TitleCell title={<LabelPill map={{ COMPLETED: ['Completed', 'green'], REVERSED: ['Reversed', 'grey'] }} value={x.status} />}
          sub={x.reversed_at ? `${x.reversed_by_name ?? ''} ${dateTime(x.reversed_at)} · ${x.reversal_reason ?? ''}` : undefined} /> },
      ...(onReverse ? [{ header: '', cell: (x: Redemption) => (x.status === 'COMPLETED'
        ? <button className="btn sm" onClick={() => onReverse(x.id)}><Undo2 size={14} />Reverse</button> : null) }] : []),
    ]} />
  );
}
