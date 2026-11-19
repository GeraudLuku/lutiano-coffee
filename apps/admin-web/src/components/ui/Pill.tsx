import type { ReactNode } from 'react';

export type PillTone = 'grey' | 'green' | 'red' | 'amber' | 'blue' | 'purple' | 'gold' | 'navy';
export function Pill({ tone = 'grey', children }: { tone?: PillTone | string; children: ReactNode }) {
  return <span className={`pill pill-${tone}`}>{children}</span>;
}

/** A pill from a label table: map[value] = [label, tone]. */
export function LabelPill<K extends string>({ map, value }: { map: Record<K, [string, string]>; value: K }) {
  const [label, tone] = map[value] ?? [value, 'grey'];
  return <Pill tone={tone}>{label}</Pill>;
}
