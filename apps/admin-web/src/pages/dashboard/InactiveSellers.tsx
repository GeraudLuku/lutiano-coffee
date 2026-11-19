import { Link } from 'react-router-dom';
import { UserX } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { DataTable, Panel, Pill, TitleCell } from '../../components/ui';
import { ago, date } from '../../utils/format';
import type { DashboardData } from '../../types/models';

/** Inactivity review flags: active sellers with no sale recently. Managers decide; nobody is deactivated automatically. */
export function InactiveSellers({ rows, days }: { rows: DashboardData['inactiveSellers']; days: number }) {
  const { can } = useAuth();
  return (
    <Panel title={`Inactive Sellers (${rows.length})`} icon={<UserX size={19} />}>
      <p className="muted small">Active sellers with no completed sale in the last {days} days. This is a review flag only; accounts are never deactivated automatically.</p>
      {!rows.length ? <p className="muted pad">Every active seller has sold in the last {days} days.</p> : (
        <DataTable rows={rows} rowKey={s => s.seller_id} columns={[
          { header: 'Seller', cell: s => <TitleCell title={can('ADMIN') ? <Link className="link" to={`/users/${s.seller_id}`}>{s.seller_name}</Link> : s.seller_name} sub={`Since ${date(s.created_at)}`} /> },
          { header: 'Last sale', cell: s => (s.last_sale_at ? ago(s.last_sale_at) : <Pill tone="amber">Never sold</Pill>) },
          { header: 'Last sign-in', cell: s => (s.last_login_at ? ago(s.last_login_at) : 'Never') },
          { header: 'Active locations', align: 'right', cell: s => (s.active_operations ? s.active_operations : <Pill tone="red">None assigned</Pill>) },
        ]} />
      )}
    </Panel>
  );
}
