import { Link } from 'react-router-dom';

/** `to` makes the legend entry a link to the records behind the segment. */
export interface Segment { label: string; value: number; color: string; to?: string }

/** Inside the ring (radius 70, stroke 28) about 100 units of width are free; the centre value shrinks to fit them. */
const HOLE_WIDTH = 100, MAX_FONT = 26, MIN_FONT = 11, CHAR_WIDTH = 0.62;

export function Donut({ segments, centre, label }: { segments: Segment[]; centre: string; label: string }) {
  const total = segments.reduce((s, x) => s + x.value, 0);
  const r = 70, c = 2 * Math.PI * r;
  const fontSize = Math.max(MIN_FONT, Math.min(MAX_FONT, HOLE_WIDTH / (Math.max(1, centre.length) * CHAR_WIDTH)));
  let offset = 0;
  return (
    <div className="donut-wrap">
    <div className="donut">
      <svg viewBox="0 0 200 200" role="img" aria-label={`${label} by channel`}>
        <circle cx="100" cy="100" r={r} className="donut-track" />
        {total > 0 && segments.filter(s => s.value > 0).map(s => {
          const len = (s.value / total) * c;
          const el = <circle key={s.label} cx="100" cy="100" r={r} stroke={s.color} strokeDasharray={`${Math.max(0, len - 2)} ${c}`} strokeDashoffset={-offset} className="donut-seg" />;
          offset += len;
          return el;
        })}
        <text x="100" y={100 + fontSize * 0.1} textAnchor="middle" className="donut-value" style={{ fontSize }}>{centre}</text>
        <text x="100" y={100 + fontSize * 0.1 + 20} textAnchor="middle" className="donut-label">{label}</text>
      </svg>
      <ul>
        {segments.map(s => {
          const content = <><i style={{ background: s.color }} /><span>{s.label}</span><b>{total ? ((s.value / total) * 100).toFixed(1) : '0.0'}%</b></>;
          return <li key={s.label}>{s.to ? <Link to={s.to} className="legend-link">{content}</Link> : content}</li>;
        })}
      </ul>
    </div>
    </div>
  );
}
