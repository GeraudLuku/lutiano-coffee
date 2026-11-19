import { Link } from 'react-router-dom';
import { Award } from 'lucide-react';
import { productsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Loading, Note, Panel, Pill, TitleCell } from '../../components/ui';
import { rateLabel } from '../../utils/earnings';
import { SELLER_ROLE_LABEL } from '../../utils/labels';
import { num, xaf } from '../../utils/format';

/** What buying or selling one unit earns: loyalty points for the customer, commission for each seller role. Rules are set in Loyalty and Commissions. */
export function RewardsTab({ productId }: { productId: string }) {
  const { data, error, loading, reload } = useLoad(() => productsApi.rewards(productId), [productId]);
  if (loading && !data) return <Loading />;
  return (
    <Panel title="Loyalty & Commission" icon={<Award size={19} />}>
      <ErrorNote error={error} retry={reload} />
      <Note>Points are what one unit earns a customer today under the loyalty rules, before any tier bonus, and are awarded only after a completed sale. Commission is paid to the seller by role.
        Change them in <Link className="link" to="/loyalty?tab=rules">Loyalty → Earning Rules</Link> and <Link className="link" to="/commissions?tab=rules">Commissions → Commission Rules</Link>.</Note>
      <DataTable rows={data ?? []} rowKey={r => r.product_id} columns={[
        { header: 'Item', cell: r => <TitleCell title={r.variant_name ?? r.name} sub={r.sku} /> },
        { header: 'Price', align: 'right', cell: r => xaf(r.price_xaf) },
        { header: 'Loyalty points / unit', align: 'right', cell: r => (r.loyalty_eligible ? <b>{num(r.loyalty_points)} pts</b> : <Pill tone="grey">Not eligible</Pill>) },
        ...(['FIXED_POS_BARISTA', 'MOBILE_BARISTA', 'MARKET_AGENT'] as const).map(role => ({
          header: SELLER_ROLE_LABEL[role], align: 'right' as const,
          cell: (r: NonNullable<typeof data>[number]) => { const c = r.commission.find(x => x.seller_role === role); return c?.basis && c.rate != null ? rateLabel(c.basis, c.rate) : <span className="muted">—</span>; },
        })),
      ]} />
    </Panel>
  );
}
