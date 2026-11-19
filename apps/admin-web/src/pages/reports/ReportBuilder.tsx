import { useState } from 'react';
import { Download, Play } from 'lucide-react';
import { reportsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { DataTable, ErrorNote, Field, Note, Panel } from '../../components/ui';
import { num, xaf } from '../../utils/format';
import { saveFile, toCsv } from '../../utils/files';
import type { BuilderReport } from '../../types/models';
import type { ReportRange } from './ReportPeriod';

const METRICS: [string, string, boolean][] = [['sales', 'Total Sales', true], ['transactions', 'Transactions', false], ['units', 'Units Sold', false],
  ['avg_order_value', 'Avg. Order Value', true], ['commission', 'Commission', true], ['points', 'Loyalty Points', false]];
const GROUPS: [string, string][] = [['day', 'Day'], ['week', 'Week'], ['month', 'Month'], ['operation', 'Operation'], ['seller', 'Seller'], ['channel', 'Channel']];

/** Report builder (design 13.4): pick metrics and a grouping, preview the result, export it. */
export function ReportBuilder({ range }: { range: ReportRange }) {
  const [metrics, setMetrics] = useState<string[]>(['sales', 'transactions']);
  const [group, setGroup] = useState('week');
  const [result, setResult] = useState<BuilderReport | null>(null);
  const run = useAction();
  const toggle = (m: string, on: boolean) => setMetrics(prev => (on ? [...prev, m] : prev.filter(x => x !== m)));
  const cell = (m: string, v: string | number | undefined) => (METRICS.find(x => x[0] === m)?.[2] ? xaf(Number(v ?? 0)) : num(Number(v ?? 0)));
  return (
    <div className="grid-form wide-left">
      <Panel title="1. Select data & metrics">
        <Note>Data source: Sales. Choose what to measure and how to group it, for the period selected above.</Note>
        <Field label="Metrics" group hint="Up to 6.">
          <div>{METRICS.map(([k, l]) => <label key={k} className="check-line"><input type="checkbox" checked={metrics.includes(k)} onChange={e => toggle(k, e.target.checked)} />{l}</label>)}</div>
        </Field>
        <Field label="Group by"><select className="input" value={group} onChange={e => setGroup(e.target.value)}>{GROUPS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}</select></Field>
        <button className="btn gold" disabled={!metrics.length || run.busy} onClick={() => run.run(async () => setResult(await reportsApi.builder({ metrics, groupBy: group, ...range })))}><Play size={16} />{run.busy ? 'Running…' : 'Run Report'}</button>
      </Panel>
      <Panel title="Preview" action={result && <button className="btn sm" onClick={() => saveFile(toCsv(result.rows, [['label', 'Group'], ...result.metrics.map(m => [m, METRICS.find(x => x[0] === m)![1]] as [string, string])]), `lutiano-report-${result.groupBy}.csv`)}><Download size={14} />Export</button>}>
        <ErrorNote error={run.error} />
        {!result ? <p className="muted">Run the report to see the preview.</p> : (
          <DataTable rows={[...result.rows, { label: 'Total', ...result.totals }]} rowKey={r => String(r.label)} columns={[
            { header: GROUPS.find(g => g[0] === result.groupBy)![1], cell: r => <b className={r.label === 'Total' ? '' : undefined}>{String(r.label)}</b> },
            ...result.metrics.map(m => ({ header: METRICS.find(x => x[0] === m)![1], align: 'right' as const, cell: (r: Record<string, string | number>) => (r[m] === undefined ? '—' : cell(m, r[m])) })),
          ]} />
        )}
      </Panel>
    </div>
  );
}
