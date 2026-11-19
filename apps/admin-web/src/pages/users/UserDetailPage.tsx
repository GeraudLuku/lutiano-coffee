import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Activity, Ban, Check, LogOut, Mail, MapPin, Pencil, Phone, RefreshCw, Shield, ShoppingCart, User, ShieldCheck } from 'lucide-react';
import { staffApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { Avatar, ConfirmDialog, ErrorNote, Facts, Loading, Note, PageHeader, Panel, Tabs } from '../../components/ui';
import { AssignSellerModal, EligibilityPill, PagedActivity, PagedSales, RolePill, StatusPill } from '../../components/domain';
import { dateTime } from '../../utils/format';
import { ROLE_LABEL, SELLER_ROLE_LABEL } from '../../utils/labels';
import type { StaffDetail } from '../../types/models';
import { ResetAccessModal } from './ResetAccessModal';
import { DeactivateModal } from './DeactivateModal';
import { SellerPerformance } from './seller/SellerOverview';
import { SellerLocationsTab } from './seller/SellerLocationsTab';
import { SellerDocumentsTab } from './seller/SellerDocumentsTab';
import { SellerEarningsTab } from './seller/SellerEarningsTab';
import { PermissionList } from './PermissionList';

type Tab = 'overview' | 'sales' | 'locations' | 'documents' | 'earnings' | 'permissions' | 'activity';

/** User Details (design: User Details and Actions). Sellers get performance, locations & stock, documents and earnings tabs. */
export function UserDetailPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const { session } = useAuth();
  const [params, setParams] = useSearchParams();
  const { data: u, error, loading, reload } = useLoad(() => staffApi.get(id!), [id]);
  const [modal, setModal] = useState<'' | 'reset' | 'deactivate' | 'assign' | 'signout' | 'mfa'>('');
  const action = useAction();

  async function perform(fn: () => Promise<unknown>, msg: string) {
    if (await action.run(fn, msg) !== undefined) { setModal(''); reload(); }
  }

  if (loading && !u) return <Loading />;
  if (!u) return <ErrorNote error={error} retry={reload} />;
  const self = session!.user.id === u.id, seller = u.role === 'SELLER';
  const tabs: [Tab, string][] = seller
    ? [['overview', 'Overview'], ['sales', 'Sales Activity'], ['locations', 'Locations & Stock'], ['documents', `Documents (${u.documents?.length ?? 0})`],
      ['earnings', 'Commissions & Earnings'], ['permissions', 'Permissions'], ['activity', 'Activity Log']]
    : [['overview', 'Overview'], ['permissions', 'Permissions'], ['activity', 'Activity Log']];
  const tab = tabs.some(t => t[0] === params.get('tab')) ? (params.get('tab') as Tab) : 'overview';
  const setTab = (t: Tab) => setParams({ tab: t }, { replace: true });

  return (
    <>
      <PageHeader title="User Details" crumbs={[['Users', '/users'], ...(seller ? [['Sellers', '/users/sellers'] as [string, string]] : []), ['User Details']]}
        actions={<>
          <button className="btn" onClick={() => nav(`/users/${id}/edit`)}><Pencil size={16} />Edit User</button>
          <button className="btn" onClick={() => setModal('reset')}><RefreshCw size={16} />Reset Access</button>
          {!self && u.role === 'ADMIN' && <button className="btn" onClick={() => setModal('mfa')}><ShieldCheck size={16} />Reset 2FA</button>}
          {!self && u.active && <button className="btn" onClick={() => setModal('signout')}><LogOut size={16} />Sign out everywhere</button>}
          {!self && (u.active
            ? <button className="btn danger" onClick={() => setModal('deactivate')}><Ban size={16} />Deactivate</button>
            : <button className="btn" onClick={() => perform(() => staffApi.activate(u.id), 'User reactivated.')}><Check size={16} />Reactivate</button>)}
        </>} />
      <ErrorNote error={action.error} />
      {!u.active && (
        <Note tone="warn">Deactivated {u.deactivated_at ? dateTime(u.deactivated_at) : ''}{u.deactivated_by_name ? ` by ${u.deactivated_by_name}` : ''}{u.deactivation_reason ? `: ${u.deactivation_reason}` : '.'}</Note>
      )}
      <ProfileCard user={u} />
      <Tabs value={tab} onChange={setTab} tabs={tabs} />

      {tab === 'overview' && (
        <div className="grid-3">
          {seller && <SellerPerformance user={u} onOpenDocuments={() => setTab('documents')} />}
          <Panel title="Profile Information" icon={<User size={19} />}>
            <Facts items={[
              ['Full Name', u.full_name], ['Phone Number', u.phone ?? '—'], ['Email Address', u.email],
              ['Role', <RolePill role={u.role} sellerRole={u.seller_role} />], ['Location / Area', u.area ?? '—'], ['Address', u.address ?? '—'],
              ['Status', <StatusPill active={u.active} />], ['Date Created', dateTime(u.created_at)], ['Last Login', u.last_login_at ? dateTime(u.last_login_at) : 'Never'],
            ]} />
          </Panel>
          <Panel title="Permissions" icon={<Shield size={19} />}>
            <PermissionList role={u.role} />
            <p className="muted small">Permissions come from the role and are enforced by the server.</p>
          </Panel>
          <Panel title="Recent Activity" icon={<Activity size={19} />}><PagedActivity actorId={u.id} showModule /></Panel>
        </div>
      )}
      {tab === 'sales' && (
        <Panel title="Recent Sales" icon={<ShoppingCart size={19} />} action={<Link className="link" to={`/sales?seller=${u.id}`}>All sales</Link>}>
          <PagedSales sellerId={u.id} variant="bySeller" />
        </Panel>
      )}
      {tab === 'locations' && seller && (
        <SellerLocationsTab user={u} busy={action.busy} onAssign={() => setModal('assign')}
          onRemove={opId => perform(() => staffApi.unassign(u.id, opId), 'Location removed.')} />
      )}
      {tab === 'documents' && seller && <SellerDocumentsTab user={u} onChanged={reload} />}
      {tab === 'earnings' && seller && <SellerEarningsTab user={u} />}
      {tab === 'permissions' && (
        <Panel title={`${ROLE_LABEL[u.role]} permissions`} icon={<ShieldCheck size={19} />}>
          <PermissionList role={u.role} />
          <p className="muted small">Roles are fixed and enforced by the server on every request. To give someone different access, change their role under Edit User.</p>
        </Panel>
      )}
      {tab === 'activity' && <Panel title="Activity Log" icon={<Activity size={19} />}><PagedActivity actorId={u.id} showModule /></Panel>}

      {modal === 'reset' && <ResetAccessModal userId={u.id} name={u.full_name} onClose={() => setModal('')} />}
      {modal === 'deactivate' && <DeactivateModal userId={u.id} name={u.full_name} seller={seller} onClose={() => setModal('')} onDone={() => { setModal(''); reload(); }} />}
      {modal === 'signout' && (
        <ConfirmDialog title="Sign out of every device?" confirmLabel="Sign out everywhere" busy={action.busy} onClose={() => setModal('')}
          onConfirm={() => perform(() => staffApi.revokeSessions(u.id), `${u.full_name} was signed out of every device.`)}>
          <p>{u.full_name} will need to sign in again on the Seller app and the Admin Portal. Use this if a phone is lost or shared.</p>
        </ConfirmDialog>
      )}
      {modal === 'mfa' && (
        <ConfirmDialog title="Reset two-step sign-in?" confirmLabel="Reset 2FA" busy={action.busy} onClose={() => setModal('')}
          onConfirm={() => perform(() => staffApi.resetMfa(u.id), `${u.full_name}'s two-step sign-in was reset.`)}>
          <p>{u.full_name} is signed out everywhere and must confirm their phone with an SMS code at their next sign-in. Use this after a lost phone or a new number.</p>
        </ConfirmDialog>
      )}
      {modal === 'assign' && <AssignSellerModal sellerId={u.id} sellerRole={u.seller_role} exclude={u.operations.map(o => o.id)} onClose={() => setModal('')} onDone={() => { setModal(''); reload(); }} />}
    </>
  );
}

function ProfileCard({ user: u }: { user: StaffDetail }) {
  return (
    <section className="panel profile">
      <Avatar name={u.full_name} url={u.image_url} size={104} />
      <div className="profile-main">
        <h2>{u.full_name}</h2><p>{u.role === 'SELLER' && u.seller_role ? SELLER_ROLE_LABEL[u.seller_role] : ROLE_LABEL[u.role]}</p>
        <span><Phone size={15} aria-hidden="true" />{u.phone ?? 'No phone'}</span>
        <span><Mail size={15} aria-hidden="true" />{u.email}</span>
        {u.area && <span><MapPin size={15} aria-hidden="true" />{u.area}</span>}
      </div>
      <Facts items={[
        ['Role', <RolePill role={u.role} sellerRole={u.seller_role} />],
        ['Assigned Locations', u.role !== 'SELLER' ? '—' : u.operations.length
          ? <div className="link-list">{u.operations.map(o => <Link key={o.id} className="link" to={`/locations/${o.id}`}>{o.code} – {o.name}</Link>)}</div>
          : 'None'],
        ...(u.role === 'SELLER' ? [['Eligibility', <EligibilityPill eligibility={u.eligibility} />] as [string, JSX.Element]] : []),
        ['Account Status', <StatusPill active={u.active} />],
        ...(u.role === 'ADMIN' ? [['Two-step sign-in', u.mfa_enrolled_at ? `On since ${dateTime(u.mfa_enrolled_at)}` : 'Not confirmed yet (set up at next sign-in)'] as [string, string]] : []),
        ['Message language', u.language === 'en' ? 'English' : 'French'],
        ['Date Created', dateTime(u.created_at)],
        ['Last Login', u.last_login_at ? dateTime(u.last_login_at) : 'Never'],
      ]} />
    </section>
  );
}
