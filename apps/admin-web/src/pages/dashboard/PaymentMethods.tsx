import { Link } from 'react-router-dom';
import { CreditCard } from 'lucide-react';
import { Panel } from '../../components/ui';
import { xaf } from '../../utils/format';
import { PAYMENT_COLOR, PAYMENT_LABEL } from '../../utils/payment';
import type { DashboardData } from '../../types/models';
import { salesLink } from './period';

const METHODS = Object.keys(PAYMENT_LABEL);

/** Revenue by payment method with its share; every method is listed, so Mobile Money shows 0 until it is enabled. */
export function PaymentMethods({ rows, dates }: { rows: DashboardData['paymentMix']; dates: { from: string; to: string } }) {
  const total = rows.reduce((n, r) => n + Number(r.revenue_xaf), 0);
  return (
    <Panel title="Payment Methods" icon={<CreditCard size={19} />}>
      <ul className="method-bars">
        {METHODS.map(m => {
          const amount = Number(rows.find(r => r.payment_method === m)?.revenue_xaf ?? 0), pct = total ? (amount / total) * 100 : 0;
          return (
            <li key={m}>
              <div><Link className="link" to={salesLink(dates, { payment: m })}>{PAYMENT_LABEL[m]}</Link><b>{pct.toFixed(1)}%</b></div>
              <small>{xaf(amount)}</small>
              <div className="bar"><i style={{ width: `${pct}%`, background: PAYMENT_COLOR[m] }} /></div>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}
