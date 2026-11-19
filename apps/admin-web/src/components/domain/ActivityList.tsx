import { Pill } from '../ui';
import { ago, dateTime, humanize } from '../../utils/format';
import { MODULE } from '../../utils/labels';
import type { AuditEvent } from '../../types/models';

/** Timeline of audit events; `showModule` adds the coloured module tag used on the dashboard. */
export function ActivityList({ rows, showModule, empty = 'No activity recorded yet.' }: { rows?: AuditEvent[]; showModule?: boolean; empty?: string }) {
  if (!rows?.length) return <p className="muted pad">{empty}</p>;
  return (
    <ul className={`timeline ${showModule ? '' : 'compact'}`}>
      {rows.map((e, i) => {
        const [module, tone] = MODULE[e.entity_type ?? ''] ?? [e.entity_type ?? '', 'grey'];
        return (
          <li key={e.id ?? i} className={`dot-${tone}`}>
            <span className="when">{ago(e.created_at)}</span>
            <div><b>{humanize(e.action)}</b><small>{[e.actor_name, e.details].filter(Boolean).join(' · ') || dateTime(e.created_at)}</small></div>
            {showModule && module && <Pill tone={tone}>{module}</Pill>}
          </li>
        );
      })}
    </ul>
  );
}
