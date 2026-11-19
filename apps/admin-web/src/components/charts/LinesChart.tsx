import { useState } from 'react';
import { compact, num, shortDay } from '../../utils/format';

export interface LineSeries { key: string; label: string; color: string }

/** Several series over days on one axis (e.g. points earned vs redeemed). Missing days should be filled with 0 by the caller. */
export function LinesChart<T extends { date: string }>({ data, series, unit, label }: {
  data: T[]; series: LineSeries[]; unit: string; label: string;
}) {
  const [hover, setHover] = useState<number | null>(null);
  const W = 640, H = 240, L = 48, R = 16, T = 14, B = 30;
  const iw = W - L - R, ih = H - T - B;
  const value = (d: T, k: string) => Number((d as Record<string, unknown>)[k] ?? 0);
  const max = Math.max(4, ...data.flatMap(d => series.map(s => value(d, s.key))));
  const top = Math.ceil(max / 4) * 4;
  const x = (i: number) => L + (data.length <= 1 ? iw / 2 : (iw * i) / (data.length - 1));
  const y = (v: number) => T + ih - (v / top) * ih;
  const labelEvery = Math.ceil(data.length / 8);
  const h = hover !== null ? data[hover] : null;
  return (
    <div className="chart">
      <div className="legend">{series.map(s => <span key={s.key}><i className="sw" style={{ background: s.color }} />{s.label}</span>)}</div>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={label} onMouseLeave={() => setHover(null)}>
        {[0, 1, 2, 3, 4].map(t => {
          const yy = T + (ih * t) / 4;
          return <g key={t}><line x1={L} x2={W - R} y1={yy} y2={yy} className="grid" /><text x={L - 8} y={yy + 4} textAnchor="end">{compact(Math.round(top - (top * t) / 4))}</text></g>;
        })}
        {series.map(s => (
          <g key={s.key}>
            <polygon points={`${x(0)},${T + ih} ${data.map((d, i) => `${x(i)},${y(value(d, s.key))}`).join(' ')} ${x(data.length - 1)},${T + ih}`} fill={s.color} opacity="0.08" />
            <polyline points={data.map((d, i) => `${x(i)},${y(value(d, s.key))}`).join(' ')} fill="none" stroke={s.color} strokeWidth="2.5" />
            {data.map((d, i) => <circle key={d.date} cx={x(i)} cy={y(value(d, s.key))} r={hover === i ? 4.5 : 0} fill={s.color} />)}
          </g>
        ))}
        {data.map((d, i) => (
          <g key={d.date} onMouseEnter={() => setHover(i)}>
            <rect x={x(i) - iw / Math.max(1, data.length) / 2} y={T} width={iw / Math.max(1, data.length)} height={ih} fill="transparent" />
            {i % labelEvery === 0 && <text x={x(i)} y={H - 8} textAnchor="middle">{shortDay(d.date)}</text>}
          </g>
        ))}
      </svg>
      {h && <div className="tip"><b>{shortDay(h.date)}</b>{series.map(s => `${s.label}: ${num(value(h, s.key))} ${unit}`).join(' · ')}</div>}
    </div>
  );
}
