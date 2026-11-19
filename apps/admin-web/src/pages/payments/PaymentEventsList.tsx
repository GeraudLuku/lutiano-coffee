import { dateTime } from '../../utils/format';
import { EVENT_LABEL, EVENT_SOURCE } from '../../utils/paymentStatus';
import type { PaymentEvent } from '../../types/models';
import { PaymentStatusPill } from './PaymentBadges';

/** Append-only payment history, oldest first. */
export function PaymentEventsList({ events }: { events: PaymentEvent[] }) {
  if (!events.length) return <p className="muted pad">No events recorded.</p>;
  return (
    <ul className="timeline compact">
      {events.map(e => (
        <li key={e.id} className="dot-green">
          <span className="when">{dateTime(e.created_at)}</span>
          <div>
            <b>{EVENT_LABEL[e.event_type] ?? e.event_type}</b>
            <small>{[EVENT_SOURCE[e.source] ?? e.source, e.actor_name, e.details].filter(Boolean).join(' · ')}</small>
            {e.to_status && <div><PaymentStatusPill status={e.to_status} /></div>}
          </div>
        </li>
      ))}
    </ul>
  );
}
