import { useState } from 'react';
import { Info, Pencil, Plus, Target, Trash2 } from 'lucide-react';
import { loyaltyApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ConfirmDialog, DataTable, ErrorNote, LabelPill, Panel, TitleCell, ListState } from '../../components/ui';
import { date, num } from '../../utils/format';
import { RULE_METHOD } from '../../utils/loyalty';
import type { EarningRule } from '../../types/models';
import { RuleModal } from './RuleModal';

const value = (r: EarningRule) => r.method === 'RATE' ? `${num(r.points)} pts / ${num(r.amount_xaf)} XAF`
  : r.method === 'MULTIPLIER' ? `${(Number(r.multiplier_pct) / 100).toFixed(2).replace(/\.?0+$/, '')}× points`
  : r.method === 'PER_UNIT' ? `${num(r.points)} pts per unit` : `${num(r.points)} pts once`;
const appliesTo = (r: EarningRule) => r.scope === 'ALL' ? 'All products' : r.scope === 'CATEGORY' ? `Category: ${r.category}` : `Product: ${r.product_name}`;
const status = (r: EarningRule) => {
  const today = new Date().toISOString().slice(0, 10);
  if (!r.active) return 'INACTIVE';
  if (r.starts_on && r.starts_on.slice(0, 10) > today) return 'SCHEDULED';
  if (r.ends_on && r.ends_on.slice(0, 10) < today) return 'ENDED';
  return 'ACTIVE';
};

/** Design 8.2: default rule from the settings, specific rules, and the order they apply in. */
export function RulesTab({ onEditDefault }: { onEditDefault: () => void }) {
  const { data, error, loading, reload } = useLoad(() => loyaltyApi.rules(), []);
  const settings = useLoad(() => loyaltyApi.settings(), []);
  const [editing, setEditing] = useState<EarningRule | 'new' | null>(null);
  const [deleting, setDeleting] = useState<EarningRule | null>(null);
  const action = useAction();
  const s = settings.data;
  return (
    <>
      <div className="grid-form wide-left">
        <section className="panel rule-default">
          <Target size={26} aria-hidden="true" />
          <div>
            <h3>Default Earning Rule</h3>
            <p className="muted">Applies to every loyalty-eligible product without a specific rate.</p>
            <p className="rule-rate">{s ? `${num(s.base_points)} pts per ${num(s.base_amount_xaf)} XAF` : '…'} {s && <LabelPill map={{ Y: ['Active', 'green'], N: ['Programme paused', 'amber'] }} value={s.program_active ? 'Y' : 'N'} />}</p>
          </div>
          <button className="btn sm" onClick={onEditDefault}><Pencil size={14} />Edit</button>
        </section>
        <Panel title="How rules combine" icon={<Info size={19} />}>
          <ol className="muted small rule-order">
            <li>Rate: product rule, then category rule, then the default rule.</li>
            <li>The member’s tier multiplier applies to those base points.</li>
            <li>Bonuses stack on top: per-unit, promotion multipliers (promotion-eligible products only) and the first-purchase bonus.</li>
            <li>Points are awarded only after a paid sale completes; changes apply to future sales.</li>
          </ol>
        </Panel>
      </div>
      <Panel title="Specific Earning Rules" action={<button className="btn gold sm" onClick={() => setEditing('new')}><Plus size={15} />Add Rule</button>}>
        <ErrorNote error={error || action.error} retry={reload} />
        <ListState loading={loading} data={data} items={data ?? []} empty={{ title: 'No specific rules yet' }}>
          <DataTable rows={data ?? []} rowKey={r => r.id} columns={[
            { header: 'Rule Name', cell: r => <b>{r.name}</b> },
            { header: 'Applies To', cell: r => appliesTo(r) },
            { header: 'Earning Method', cell: r => RULE_METHOD[r.method][0] },
            { header: 'Value', cell: r => value(r) },
            { header: 'Period', cell: r => (r.starts_on || r.ends_on ? <TitleCell title={`${r.starts_on ? date(r.starts_on) : '…'} – ${r.ends_on ? date(r.ends_on) : '…'}`} /> : 'Always') },
            { header: 'Status', cell: r => <LabelPill map={{ ACTIVE: ['Active', 'green'], SCHEDULED: ['Scheduled', 'purple'], ENDED: ['Ended', 'grey'], INACTIVE: ['Inactive', 'red'] }} value={status(r)} /> },
            { header: '', cell: r => (
              <div className="row-actions">
                <button className="icon-btn" aria-label={`Edit ${r.name}`} onClick={() => setEditing(r)}><Pencil size={15} /></button>
                <button className="icon-btn danger" aria-label={`Delete ${r.name}`} onClick={() => setDeleting(r)}><Trash2 size={15} /></button>
              </div>
            ) },
          ]} />
        </ListState>
      </Panel>
      {editing && <RuleModal rule={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); reload(); }} />}
      {deleting && (
        <ConfirmDialog title={`Delete “${deleting.name}”?`} confirmLabel="Delete Rule" danger busy={action.busy} onClose={() => setDeleting(null)}
          onConfirm={async () => { if (await action.run(() => loyaltyApi.deleteRule(deleting.id), 'Rule deleted.') !== undefined) { setDeleting(null); reload(); } }}>
          <p>Future sales stop using this rule. Points already awarded are not changed. To pause it instead, edit it and switch it off.</p>
        </ConfirmDialog>
      )}
    </>
  );
}
