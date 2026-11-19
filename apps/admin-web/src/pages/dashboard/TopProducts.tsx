import { Link } from 'react-router-dom';
import { Award } from 'lucide-react';
import { Panel } from '../../components/ui';
import { num, xaf } from '../../utils/format';
import type { DashboardData } from '../../types/models';

/** Best sellers by revenue (add-ons included), with each product's share of the period's revenue. */
export function TopProducts({ rows, totalRevenue }: { rows: DashboardData['topProducts']; totalRevenue: number }) {
  return (
    <Panel title="Top Products" icon={<Award size={19} />} action={<Link className="link" to="/products">Catalogue</Link>}>
      {!rows.length ? <p className="muted pad">No sales in this period.</p> : (
        <ul className="rank-list">
          {rows.map((p, i) => {
            const share = totalRevenue ? (Number(p.revenue_xaf) / totalRevenue) * 100 : 0;
            return (
              <li key={p.product_id}>
                <span className="rank">{i + 1}</span>
                <div>
                  <Link className="link" to={`/products/${p.product_id}`}>{p.product_name}</Link>
                  <small>{num(p.units)} units · {share.toFixed(1)}% of revenue</small>
                  <div className="share-bar"><span style={{ width: `${share}%` }} /></div>
                </div>
                <b>{xaf(p.revenue_xaf)}</b>
              </li>
            );
          })}
        </ul>
      )}
    </Panel>
  );
}
