import { Link } from 'react-router-dom';
import { ArrowRight, Receipt } from 'lucide-react';
import { salesApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Panel, TitleCell, ListState } from '../../components/ui';
import { CompletedPill, TypePill } from '../../components/domain';
import { dateTime, xaf } from '../../utils/format';
import { paymentLabel } from '../../utils/payment';

const LATEST = 5;

/** The latest sales across all locations. */
export function RecentTransactions() {
  const { data, error, loading, reload } = useLoad(() => salesApi.list({ page: 1, size: LATEST }), []);
  return (
    <Panel title="Recent Transactions" icon={<Receipt size={19} />} action={<Link className="link" to="/sales">View All Sales <ArrowRight size={15} /></Link>}>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={data?.items ?? []} empty={{ title: 'No sales yet' }}>
        <div className="compact-table"><DataTable rows={data?.items ?? []} rowKey={s => s.id} columns={[
          { header: '#', cell: s => <Link className="link" to={`/sales/${s.id}`}>{s.reference}</Link> },
          { header: 'Date & Time', cell: s => dateTime(s.created_at).replace(',', '') },
          { header: 'Location / Channel', cell: s => <TitleCell title={s.operation_name ?? '—'} sub={<TypePill type={s.operation_type} />} /> },
          { header: 'Amount', align: 'right', cell: s => xaf(s.total_xaf) },
          { header: 'Payment', cell: s => paymentLabel(s.payment_method ?? '') },
          { header: 'Status', cell: () => <CompletedPill /> },
        ]} /></div>
      </ListState>
    </Panel>
  );
}
