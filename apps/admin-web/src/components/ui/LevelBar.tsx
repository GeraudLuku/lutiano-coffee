export function LevelBar({ value, max, tone }: { value: number; max: number; tone: string }) {
  const pct = Math.max(0, Math.min(100, max > 0 ? (value / max) * 100 : 0));
  return <div className="level" role="presentation"><span className={tone} style={{ width: `${pct}%` }} /></div>;
}
