import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, Minus, Plus, Search, X } from 'lucide-react';
import { customersApi, paymentsApi, productsApi, salesApi, staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useDebouncedState } from '../../hooks/useDebounced';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Field, Note, PageHeader, Panel } from '../../components/ui';
import { ProductThumb } from '../../components/domain';
import { num, xaf } from '../../utils/format';
import type { Customer } from '../../types/models';

interface Line { productId: string; name: string; sku: string; price: number; image: string | null; quantity: number }

/** Record Sale (design 5.3): an administrator enters a cash sale for a seller who has an open shift. */
export function RecordSalePage() {
  const nav = useNavigate();
  const shifts = useLoad(() => salesApi.openShifts(), []);
  const products = useLoad(() => productsApi.all('sellable'), []);
  const methods = useLoad(() => paymentsApi.methods(), []);
  const [sellerId, setSellerId] = useState('');
  const seller = useLoad(() => (sellerId ? staffApi.get(sellerId) : Promise.resolve(null)), [sellerId]);
  const [operationId, setOperationId] = useState('');
  const [lines, setLines] = useState<Line[]>([]);
  const [find, setFind] = useState('');
  const [method, setMethod] = useState('CASH');
  const [cash, setCash] = useState('');
  const [customerQ, setCustomerQ, customerSearch] = useDebouncedState('');
  const matches = useLoad(() => (customerSearch.length >= 2 ? customersApi.list({ q: customerSearch, page: 1, size: 5 }) : Promise.resolve(null)), [customerSearch]);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [key] = useState(() => crypto.randomUUID());
  const { busy, error, setError, run } = useAction();

  const operations = (seller.data?.operations ?? []).filter(o => o.active);
  const total = lines.reduce((n, l) => n + l.price * l.quantity, 0);
  const results = useMemo(() => {
    const q = find.trim().toLowerCase();
    if (q.length < 2) return [];
    return (products.data ?? []).filter(p => p.active && (p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q))).slice(0, 6);
  }, [find, products.data]);

  const add = (p: NonNullable<typeof products.data>[number]) => {
    setLines(prev => (prev.some(l => l.productId === p.id) ? prev.map(l => (l.productId === p.id ? { ...l, quantity: l.quantity + 1 } : l)) : [...prev, { productId: p.id, name: p.name, sku: p.sku, price: p.price_xaf, image: p.image_url, quantity: 1 }]));
    setFind('');
  };
  const setQty = (id: string, q: number) => setLines(prev => prev.map(l => (l.productId === id ? { ...l, quantity: Math.max(1, q) } : l)));

  async function submit() {
    if (!sellerId || !operationId) { setError('Choose the seller and the sales location.'); return; }
    if (!lines.length) { setError('Add at least one product.'); return; }
    const received = Number(cash);
    if (method === 'CASH' && (cash === '' || !Number.isInteger(received) || received < total)) { setError(`Enter the cash received, at least ${xaf(total)}.`); return; }
    const sale = await run(async () => {
      const token = customer ? (await customersApi.get(customer.id)).loyalty_token : undefined;
      return salesApi.recordForSeller({ sellerId, operationId, lines: lines.map(l => ({ productId: l.productId, quantity: l.quantity, addonIds: [] })), paymentMethod: method, cashReceivedXaf: method === 'CASH' ? received : undefined, customerToken: token }, key);
    }, 'Sale recorded.');
    if (sale) nav(`/sales/${sale.id}`);
  }

  return (
    <>
      <PageHeader title="Record Sale" subtitle="Enter a sale for a seller who has an open cash shift. It counts as that seller's sale." crumbs={[['Sales Transactions', '/sales'], ['Record Sale']]} />
      <ErrorNote error={error} />
      <div className="grid-form wide-left">
        <div className="stack">
          <Panel title="Sale Information">
            <div className="form-grid">
              <Field label="Seller (open shift)" required>
                <select className="input" value={sellerId} onChange={e => { setSellerId(e.target.value); setOperationId(''); }}>
                  <option value="">{shifts.loading ? 'Loading…' : shifts.data?.length ? 'Choose a seller…' : 'No seller has an open shift'}</option>
                  {(shifts.data ?? []).map(s => <option key={s.id} value={s.seller_id}>{s.seller_name}</option>)}
                </select>
              </Field>
              <Field label="Sales Location" required>
                <select className="input" value={operationId} onChange={e => setOperationId(e.target.value)} disabled={!sellerId}>
                  <option value="">{sellerId ? 'Choose a location…' : 'Choose a seller first'}</option>
                  {operations.map(o => <option key={o.id} value={o.id}>{o.name} ({o.code})</option>)}
                </select>
              </Field>
            </div>
            <Field label="Customer (optional)" hint="Search a loyalty member by name, phone or code so they earn points.">
              {customer ? (
                <div className="chosen"><b>{customer.full_name}</b> · {customer.phone}<button type="button" className="btn sm" onClick={() => setCustomer(null)}><X size={14} />Remove</button></div>
              ) : (
                <>
                  <input className="input" value={customerQ} onChange={e => setCustomerQ(e.target.value)} placeholder="Search by name or phone…" />
                  {matches.data?.items.length ? <ul className="pick-list">{matches.data.items.map(c => <li key={c.id}><button type="button" onClick={() => { setCustomer(c); setCustomerQ(''); }}>{c.full_name} · {c.phone}</button></li>)}</ul> : null}
                </>
              )}
            </Field>
          </Panel>
          <Panel title="Add Products">
            <label className="input-icon"><Search size={17} aria-hidden="true" /><input value={find} onChange={e => setFind(e.target.value)} placeholder="Search products by name or SKU…" aria-label="Search products" /></label>
            {results.length > 0 && <ul className="pick-list">{results.map(p => <li key={p.id}><button type="button" onClick={() => add(p)}><ProductThumb url={p.image_url} size={28} />{p.name} · {xaf(p.price_xaf)}</button></li>)}</ul>}
            {lines.length === 0 ? <p className="muted">No products added yet.</p> : (
              <ul className="sale-lines">{lines.map(l => (
                <li key={l.productId}>
                  <ProductThumb url={l.image} size={36} />
                  <span><b>{l.name}</b><small>{l.sku} · {xaf(l.price)}</small></span>
                  <span className="qty"><button type="button" aria-label="Fewer" onClick={() => setQty(l.productId, l.quantity - 1)}><Minus size={14} /></button><b>{l.quantity}</b><button type="button" aria-label="More" onClick={() => setQty(l.productId, l.quantity + 1)}><Plus size={14} /></button></span>
                  <b>{xaf(l.price * l.quantity)}</b>
                  <button type="button" className="btn sm" aria-label={`Remove ${l.name}`} onClick={() => setLines(prev => prev.filter(x => x.productId !== l.productId))}><X size={14} /></button>
                </li>))}</ul>
            )}
            <Note>Add-ons are not available here; use the Seller app for sales with add-ons.</Note>
          </Panel>
        </div>
        <Panel title="Payment">
          <Field label="Payment method" group>
            <div className="method-tiles">
              {(methods.data ?? []).map(m => {
                const usable = m.available && m.enabled;
                return <button key={m.code} type="button" disabled={!usable} className={`method-tile${method === m.code ? ' on' : ''}`} onClick={() => setMethod(m.code)}>{m.label}{!usable && <small>Not available yet</small>}</button>;
              })}
            </div>
          </Field>
          {method === 'CASH' && <Field label="Cash received (XAF)" required><input className="input" type="number" min={0} step={1} value={cash} onChange={e => setCash(e.target.value)} /></Field>}
          <dl className="totals">
            <div><dt>Items</dt><dd>{num(lines.reduce((n, l) => n + l.quantity, 0))}</dd></div>
            <div className="grand"><dt>Total Amount</dt><dd>{xaf(total)}</dd></div>
            {method === 'CASH' && cash !== '' && Number(cash) >= total && <div><dt>Change</dt><dd>{xaf(Number(cash) - total)}</dd></div>}
          </dl>
          <div className="form-actions">
            <button type="button" className="btn" onClick={() => nav('/sales')}>Cancel</button>
            <button type="button" className="btn gold" disabled={busy} onClick={submit}><Check size={16} />{busy ? 'Recording…' : 'Complete Sale'}</button>
          </div>
        </Panel>
      </div>
    </>
  );
}
