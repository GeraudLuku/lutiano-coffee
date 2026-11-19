import { Link } from 'react-router-dom';
import { Activity, AlertTriangle } from 'lucide-react';
import { num } from '../../utils/format';
import type { Attention } from '../../types/models';

/** Operational exceptions (replaces the design's "System Health" card, which has no data source yet). */
export function AttentionList({ attention, openShifts, inactiveSellers }: { attention: Attention; openShifts: number; inactiveSellers: number }) {
  const rows: [string, number, string, boolean][] = [
    ['Waiting for approval or dispatch', attention.pendingApprovals, '/inventory?tab=approvals', true],
    ['Transfers in transit', attention.inTransit, '/inventory?tab=transfers', false],
    ['Receipt discrepancies', attention.discrepancies, '/inventory?tab=discrepancies', true],
    ['Cash shift variances', attention.cashVariances, '/sales/shifts', true],
    ['Low stock at locations', attention.lowStockOperations, '/inventory?tab=locations', true],
    ['Inactive sellers to review', inactiveSellers, '#inactive-sellers', true],
    ['Open cash shifts', openShifts, '/sales/shifts', false],
  ];
  return (
    <ul className="attention">
      {rows.map(([label, n, to, isProblem]) => {
        const warn = isProblem && n > 0;
        return (
          <li key={label}><Link to={to} onClick={to.startsWith('#') ? e => { e.preventDefault(); document.querySelector(to)?.scrollIntoView({ behavior: 'smooth' }); } : undefined}>
            {warn ? <AlertTriangle size={18} className="warn-ic" /> : <Activity size={18} className="ok-ic" />}
            <span>{label}</span><b className={warn ? 'warn' : ''}>{num(n)}</b>
          </Link></li>
        );
      })}
    </ul>
  );
}
