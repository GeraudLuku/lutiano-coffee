import { Award } from 'lucide-react';
import { DataTable, ErrorNote, Loading, Note, Panel } from '../../components/ui';
import { bonusLabel } from '../../utils/earnings';
import { num, xaf } from '../../utils/format';
import { useAllTargets } from './useAllTargets';

/** How bonuses are configured today: each target carries its own bonus, a fixed amount or an amount per unit, paid only when it is reached. */
export function BonusTab() {
  const { data, error, loading, reload } = useAllTargets();
  if (loading && !data) return <Loading />;
  if (!data) return <ErrorNote error={error} retry={reload} />;
  const groups = new Map<string, { label: string; targets: number; reached: number; earned: number }>();
  for (const t of data) {
    const key = bonusLabel(t), g = groups.get(key) ?? { label: key, targets: 0, reached: 0, earned: 0 };
    g.targets += 1; g.reached += t.reached ? 1 : 0; g.earned += t.bonus_earned;
    groups.set(key, g);
  }
  return (
    <Panel title="Bonus Rules in Use" icon={<Award size={19} />}>
      <Note>A bonus is set on each target when it is created: a fixed amount, or an amount for each unit sold in the period. It is paid only if the target is reached, counts net of refunds,
        and is added to the seller's monthly earnings. Tiered and campaign bonuses are not available yet.</Note>
      <DataTable rows={[...groups.values()]} rowKey={g => g.label} columns={[
        { header: 'Bonus', cell: g => <b>{g.label}</b> }, { header: 'Active targets', align: 'right', cell: g => num(g.targets) },
        { header: 'Reached', align: 'right', cell: g => num(g.reached) }, { header: 'Earned so far', align: 'right', cell: g => xaf(g.earned) },
      ]} />
    </Panel>
  );
}
