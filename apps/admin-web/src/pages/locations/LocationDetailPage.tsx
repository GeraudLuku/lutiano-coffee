import { useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Ban, Boxes, Camera, Check, Info, Layers, Pencil, Plus, Receipt, ShoppingCart, Store, Truck, UserCog, Users, Wallet } from 'lucide-react';
import { inventoryApi, operationsApi, staffApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { mediaUrl } from '../../api/client';
import { ConfirmDialog, Empty, ErrorNote, Facts, Loading, PageHeader, Panel, Pill, StatCard, StatRow, Tabs, toast } from '../../components/ui';
import { AssignSellerModal, PagedActivity, PagedSales, ProductStockTable, StatusPill, TypePill } from '../../components/domain';
import { dateTime, num, xaf } from '../../utils/format';
import { TYPE_LABEL } from '../../utils/labels';
import type { OperationDetail } from '../../types/models';
import { QrCodePanel } from './QrCodePanel';
import { LocationPaymentsTab } from './LocationPaymentsTab';
import { MAX_IMAGE_BYTES } from '../../utils/images';

type Tab = 'overview' | 'stock' | 'sales' | 'payments' | 'seller' | 'activity';

export function LocationDetailPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const { can } = useAuth();
  const { data: o, error, loading, reload } = useLoad(() => operationsApi.get(id!), [id]);
  const stock = useLoad(() => inventoryApi.locations(id), [id]);
  const [tab, setTab] = useState<Tab>('overview');
  const [modal, setModal] = useState<'' | 'assign' | 'deactivate'>('');
  const action = useAction();
  const admin = can('ADMIN');
  const photoInput = useRef<HTMLInputElement>(null);
  async function changePhoto(file?: File) {
    if (!file) return;
    if (file.size > MAX_IMAGE_BYTES) { toast('Images must be 5 MB or smaller.', 'error'); return; }
    if (await action.run(() => operationsApi.uploadImage(id!, file), 'Photo updated.') !== undefined) reload();
    if (photoInput.current) photoInput.current.value = '';
  }

  async function perform(fn: () => Promise<unknown>, msg: string) {
    if (await action.run(fn, msg) !== undefined) { setModal(''); reload(); }
  }
  if (loading && !o) return <Loading />;
  if (!o) return <ErrorNote error={error} retry={reload} />;
  const held = (stock.data ?? []).filter(s => s.quantity > 0);

  return (
    <>
      <PageHeader title={o.name} badge={<StatusPill active={o.active} />} crumbs={[['Sales Locations', '/locations'], [o.name]]}
        subtitle={[TYPE_LABEL[o.type], o.code, [o.address, o.city].filter(Boolean).join(', ')].filter(Boolean).join('  •  ')}
        actions={admin && <>
          <button className="btn" onClick={() => nav(`/locations/${id}/edit`)}><Pencil size={16} />Edit Location</button>
          {o.active
            ? <button className="btn danger" onClick={() => setModal('deactivate')}><Ban size={16} />Deactivate</button>
            : <button className="btn" onClick={() => perform(() => operationsApi.setActive(o.id, true), 'Location reactivated.')}><Check size={16} />Reactivate</button>}
        </>} />
      <ErrorNote error={action.error} />
      <Tabs value={tab} onChange={setTab} tabs={[['overview', 'Overview'], ['stock', 'Stock'], ['sales', 'Sales'], ['payments', 'Payments'], ['seller', 'Assigned Seller'], ['activity', 'Activity']]} />

      {tab === 'overview' && (
        <>
          <div className="grid-3">
            <Panel title="Operation Information" icon={<Info size={19} />} className="span-2">
              <div className="loc-head">
                <div>
                  {o.image_url ? <img className="loc-photo" src={mediaUrl(o.image_url)} alt={`${o.name}`} /> : <div className="loc-photo"><Store size={48} aria-hidden="true" /></div>}
                  {admin && <>
                    <button type="button" className="btn sm" onClick={() => photoInput.current?.click()}><Camera size={14} />{o.image_url ? 'Change photo' : 'Add photo'}</button>
                    <input ref={photoInput} type="file" hidden accept="image/jpeg,image/png,image/webp" onChange={e => changePhoto(e.target.files?.[0])} />
                  </>}
                </div>
                <Facts items={[
                  ['Operation Type', <TypePill type={o.type} />], ['Code', o.code], ['Address', o.address ?? '—'], ['Description', o.description ?? '—'],
                  ['Region / City', [o.region, o.city].filter(Boolean).join(' / ') || '—'], ['Accepts', o.payment_methods.length ? o.payment_methods.join(', ').replace(/_/g, ' ') : '—'],
                  ['Status', <StatusPill active={o.active} />], ['Created', dateTime(o.created_at)],
                ]} />
              </div>
            </Panel>
            <Panel title="Assigned Seller" icon={<Users size={19} />}
              action={admin && <button className="btn sm" onClick={() => setModal('assign')}><Plus size={15} />Assign Seller</button>}>
              <SellerList op={o} admin={admin} onRemove={s => perform(() => staffApi.unassign(s, o.id), 'Seller removed from location.')} />
            </Panel>
          </div>
          <StatRow>
            <StatCard tone="green" icon={<Wallet size={20} />} value={xaf(o.stats.today_xaf)} label="Today's Sales" />
            <StatCard tone="gold" icon={<Wallet size={20} />} value={xaf(o.stats.week_xaf)} label="Last 7 Days" />
            <StatCard tone="blue" icon={<Wallet size={20} />} value={xaf(o.stats.month_xaf)} label="This Month" />
            <StatCard tone="amber" icon={<Receipt size={20} />} value={num(o.stats.transactions)} label="Transactions (all time)" />
          </StatRow>
          <div className="grid-2">
            <QrCodePanel op={o} />
            <Panel title="Quick Actions" icon={<Boxes size={19} />}>
              <div className="quick-list">
                {admin && <button className="btn" onClick={() => nav(`/inventory?action=transfer&operationId=${o.id}`)}><Truck size={16} />Allocate Stock</button>}
                <button className="btn" onClick={() => nav(`/sales?operation=${o.id}`)}><ShoppingCart size={16} />View Sales</button>
                {admin && <button className="btn" onClick={() => setTab('seller')}><UserCog size={16} />Change Seller</button>}
                {admin && (o.active
                  ? <button className="btn danger" onClick={() => setModal('deactivate')}><Ban size={16} />Deactivate</button>
                  : <button className="btn" onClick={() => perform(() => operationsApi.setActive(o.id, true), 'Location reactivated.')}><Check size={16} />Reactivate</button>)}
              </div>
            </Panel>
          </div>
          <div className="grid-2">
            <Panel title="Stock Summary" icon={<Layers size={19} />} action={<button className="link" onClick={() => setTab('stock')}>View All</button>}>
              <ProductStockTable rows={held.slice(0, 5)} />
            </Panel>
            <Panel title="Recent Sales" icon={<ShoppingCart size={19} />} action={<Link className="link" to={`/sales?operation=${o.id}`}>View All</Link>}>
              <PagedSales operationId={o.id} variant="byLocation" />
            </Panel>
          </div>
        </>
      )}
      {tab === 'stock' && <Panel title="Stock at this location" icon={<Layers size={19} />}><ProductStockTable rows={stock.data ?? []} /></Panel>}
      {tab === 'sales' && (
        <Panel title="Recent Sales" icon={<ShoppingCart size={19} />} action={<Link className="link" to={`/sales?operation=${o.id}`}>Open in Sales</Link>}>
          <PagedSales operationId={o.id} variant="byLocation" />
        </Panel>
      )}
      {tab === 'payments' && <LocationPaymentsTab operationId={o.id} />}
      {tab === 'seller' && (
        <Panel title="Assigned Seller" icon={<Users size={19} />} action={admin && <button className="btn sm" onClick={() => setModal('assign')}><Plus size={15} />Assign Seller</button>}>
          <SellerList op={o} admin={admin} onRemove={s => perform(() => staffApi.unassign(s, o.id), 'Seller removed from location.')} />
        </Panel>
      )}
      {tab === 'activity' && <Panel title="Activity" icon={<Info size={19} />}><PagedActivity entityType="operation" entityId={o.id} /></Panel>}

      {modal === 'assign' && <AssignSellerModal operationId={o.id} operationType={o.type} exclude={o.sellers.map(s => s.id)} onClose={() => setModal('')} onDone={() => { setModal(''); reload(); }} />}
      {modal === 'deactivate' && (
        <ConfirmDialog title="Deactivate location?" confirmLabel="Deactivate" danger busy={action.busy} onClose={() => setModal('')}
          onConfirm={() => perform(() => operationsApi.setActive(o.id, false), 'Location deactivated.')}>
          <p>Sellers will no longer be able to record sales at <b>{o.name}</b>, and its QR code will stop accepting purchases. History and stock records are kept.</p>
        </ConfirmDialog>
      )}
    </>
  );
}

function SellerList({ op, admin, onRemove }: { op: OperationDetail; admin: boolean; onRemove: (sellerId: string) => void }) {
  if (!op.sellers.length) return <Empty title="No seller assigned" text="Assign a seller so sales can be recorded here." />;
  return (
    <ul className="people">
      {op.sellers.map(s => (
        <li key={s.id}>
          <div>{admin ? <Link className="link" to={`/users/${s.id}`}><b>{s.full_name}</b></Link> : <b>{s.full_name}</b>}<small>{s.email}{s.phone ? ` · ${s.phone}` : ''}</small></div>
          {!s.active && <Pill tone="red">Inactive</Pill>}
          {admin && <button className="btn sm" onClick={() => onRemove(s.id)}>Remove</button>}
        </li>
      ))}
    </ul>
  );
}
