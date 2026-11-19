import { Link } from 'react-router-dom';
import { Users } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { Avatar, Panel } from '../../components/ui';
import { num, xaf } from '../../utils/format';
import type { DashboardData } from '../../types/models';
import { salesLink } from './period';

/** Sellers ranked by revenue. Administrators open the seller; others open the seller's sales. */
export function TopSellers({ rows, dates }: { rows: DashboardData['topSellers']; dates: { from: string; to: string } }) {
  const { can } = useAuth();
  return (
    <Panel title="Top Sellers" icon={<Users size={19} />}>
      {!rows.length ? <p className="muted pad">No sales in this period.</p> : (
        <ul className="rank-list">
          {rows.map((s, i) => (
            <li key={s.seller_id}>
              <span className="rank">{i + 1}</span>
              <div className="who-cell">
                <Avatar name={s.seller_name} size={32} />
                <div>
                  <Link className="link" to={can('ADMIN') ? `/users/${s.seller_id}` : salesLink(dates, { seller: s.seller_id })}>{s.seller_name}</Link>
                  <small><Link to={salesLink(dates, { seller: s.seller_id })}>{num(s.sales)} sales</Link></small>
                </div>
              </div>
              <b>{xaf(s.revenue_xaf)}</b>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}
