import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { categoriesApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field } from '../../components/ui';
import type { Category } from '../../types/models';

const NEW = '__new__';

/**
 * Category picker over the managed category list. "+ New category…" creates one immediately
 * (categories are managed records, so products can only use existing active categories).
 */
export function CategoryField({ value, categories, onChange, onCreated }: {
  value: string; categories: Category[]; onChange: (name: string) => void; onCreated: () => void;
}) {
  const active = categories.filter(c => c.active || c.name === value);
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState('');
  const { busy, error, run } = useAction();

  useEffect(() => {
    if (!value && !adding && active.length) onChange(active[0]!.name);
  }, [value, adding, active, onChange]);

  async function create() {
    const created = await run(() => categoriesApi.create({ name, description: '', sortOrder: 0, active: true }), `Category "${name}" created.`);
    if (created) { onChange(created.name); setAdding(false); setName(''); onCreated(); }
  }

  return (
    <Field label="Category" required group
      hint={error || (adding ? 'The category is created right away.' : undefined)}>
      <select className="input" aria-label="Category" value={adding ? NEW : value} required
        onChange={e => { if (e.target.value === NEW) setAdding(true); else { setAdding(false); onChange(e.target.value); } }}>
        {!active.length && <option value="">No categories yet</option>}
        {active.map(c => <option key={c.id} value={c.name}>{c.name}{c.active ? '' : ' (inactive)'}</option>)}
        <option value={NEW}>+ New category…</option>
      </select>
      {adding && (
        <div className="inline-add">
          <input className="input" aria-label="New category name" placeholder="e.g. Pastries" value={name} maxLength={60} autoFocus
            onChange={e => setName(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); if (name.trim()) create(); } }} />
          <button type="button" className="btn sm gold" disabled={!name.trim() || busy} onClick={create}>Add</button>
          <button type="button" className="btn sm" onClick={() => { setAdding(false); setName(''); }}>Cancel</button>
        </div>
      )}
      <small><Link className="link" to="/products/categories">Manage categories</Link></small>
    </Field>
  );
}
