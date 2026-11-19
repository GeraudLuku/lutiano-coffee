import type { ReactNode } from 'react';

/**
 * Labelled form field. Use `group` for controls that bring their own labels (switches, tile groups)
 * so <label> elements are never nested — nested labels make clicks toggle the wrong control.
 */
export function Field({ label, required, hint, group, children }: { label: string; required?: boolean; hint?: string; group?: boolean; children: ReactNode }) {
  const content = <><span>{label}{required && <i aria-hidden="true">*</i>}</span>{children}{hint && <small>{hint}</small>}</>;
  return group
    ? <div className="field" role="group" aria-label={label}>{content}</div>
    : <label className="field">{content}</label>;
}

export function FormGrid({ children, columns = 2 }: { children: ReactNode; columns?: 2 | 3 }) {
  return <div className={`form-grid ${columns === 3 ? 'three' : ''}`}>{children}</div>;
}

/** Large selectable cards for a single choice (location type, role). */
export function ChoiceTiles<T extends string>({ name, value, options, onChange, columns = 1 }: {
  name: string; value: T; onChange: (v: T) => void; columns?: 1 | 3;
  options: { value: T; label: string; description: string; icon?: ReactNode }[];
}) {
  return (
    <div className={`choice-tiles ${columns === 3 ? 'three' : ''}`} role="radiogroup">
      {options.map(o => (
        <label key={o.value} className={`tile ${value === o.value ? 'selected' : ''}`}>
          <input type="radio" name={name} value={o.value} checked={value === o.value} onChange={() => onChange(o.value)} />
          {o.icon}<b>{o.label}</b><small>{o.description}</small>
        </label>
      ))}
    </div>
  );
}

export function Switch({ checked, onChange, label, disabled }: { checked: boolean; onChange: (v: boolean) => void; label: string; disabled?: boolean }) {
  return <label className={`switch ${disabled ? 'disabled' : ''}`}><input type="checkbox" checked={checked} disabled={disabled} onChange={e => onChange(e.target.checked)} /><span />{label}</label>;
}

export function FilterBar({ children, labelled }: { children: ReactNode; labelled?: boolean }) {
  return <div className={`filters ${labelled ? 'labelled' : ''}`}>{children}</div>;
}

export function SearchInput({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
  return <input className="input" type="search" placeholder={placeholder} aria-label={placeholder} value={value} onChange={e => onChange(e.target.value)} />;
}

/** Select whose first option means "no filter". */
export function SelectFilter({ value, onChange, allLabel, options, label }: {
  value: string; onChange: (v: string) => void; allLabel: string; options: [string, string][]; label: string;
}) {
  return (
    <select className="input" value={value} onChange={e => onChange(e.target.value)} aria-label={label}>
      <option value="">{allLabel}</option>
      {options.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
    </select>
  );
}
