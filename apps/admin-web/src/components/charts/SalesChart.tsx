import { useState } from 'react';
import { compact, num, shortDay } from '../../utils/format';

export interface DayPoint { date: string; sales: number; revenueXaf: number }

/** Bars = number of sales (left axis), line = revenue in XAF (right axis). */
/** `onSelect` is called with the ISO date of a clicked bar (drill-through to that day's sales). */
export function SalesChart({ data, onSelect }: { data: DayPoint[]; onSelect?: (date: string) => void }) {
  const [hover, setHover] = useState<number | null>(null);
  const W = 640, H = 240, L = 44, R = 56, T = 14, B = 30;
  const iw = W - L - R, ih = H - T - B;
  const topS = Math.max(4, Math.ceil(Math.max(0, ...data.map(d => d.sales)) / 4) * 4);
  const topR = Math.max(4000, Math.ceil(Math.max(0, ...data.map(d => d.revenueXaf)) / 4000) * 4000);
  const slot = iw / Math.max(1, data.length), bw = Math.min(28, slot * 0.6);
  const x = (i: number) => L + slot * i + slot / 2;
  const yS = (v: number) => T + ih - (v / topS) * ih;
  const yR = (v: number) => T + ih - (v / topR) * ih;
  const labelEvery = Math.ceil(data.length / 8);
  const h = hover !== null ? data[hover] : null;
  return (
    <div className="chart">
      <div className="legend"><span><i className="sw navy" />Sales</span><span><i className="sw gold" />Revenue (XAF)</span></div>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Sales count and revenue by day" onMouseLeave={() => setHover(null)}>
        {[0, 1, 2, 3, 4].map(t => {
          const y = T + (ih * t) / 4;
          return (
            <g key={t}>
              <line x1={L} x2={W - R} y1={y} y2={y} className="grid" />
              <text x={L - 8} y={y + 4} textAnchor="end">{num(Math.round(topS - (topS * t) / 4))}</text>
              <text x={W - R + 8} y={y + 4}>{compact(Math.round(topR - (topR * t) / 4))}</text>
            </g>
          );
        })}
        {data.map((d, i) => (
          <g key={d.date} onMouseEnter={() => setHover(i)} onClick={onSelect ? () => onSelect(d.date) : undefined}
            className={onSelect ? 'clickable-bar' : undefined} role={onSelect ? 'button' : undefined}
            aria-label={onSelect ? `${shortDay(d.date)}: ${num(d.sales)} sales, view them` : undefined}>
            <rect x={L + slot * i} y={T} width={slot} height={ih} fill="transparent" />
            <rect x={x(i) - bw / 2} y={yS(d.sales)} width={bw} height={Math.max(0, T + ih - yS(d.sales))} rx="3" className={`bar ${hover === i ? 'hot' : ''}`} />
            {i % labelEvery === 0 && <text x={x(i)} y={H - 8} textAnchor="middle">{shortDay(d.date)}</text>}
          </g>
        ))}
        <polyline className="line" points={data.map((d, i) => `${x(i)},${yR(d.revenueXaf)}`).join(' ')} />
        {data.map((d, i) => <circle key={d.date} cx={x(i)} cy={yR(d.revenueXaf)} r={hover === i ? 5 : 3} className="dot" />)}
      </svg>
      {h && <div className="tip"><b>{shortDay(h.date)}</b>{num(h.sales)} sales · {num(h.revenueXaf)} XAF</div>}
    </div>
  );
}
