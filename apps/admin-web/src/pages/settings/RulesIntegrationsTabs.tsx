import { Link } from 'react-router-dom';
import { Bell, KeyRound, Plug, ShieldCheck } from 'lucide-react';
import { businessApi, notificationsApi, payoutsApi, paymentsApi, refundsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Facts, Loading, Note, Panel, Pill } from '../../components/ui';
import { xaf } from '../../utils/format';

/** Settings → Business Rules: the controls that apply to money and stock, with their current values. */
export function BusinessRulesTab() {
  const refunds = useLoad(() => refundsApi.settings(), []);
  const payouts = useLoad(() => payoutsApi.settings(), []);
  if ((refunds.loading && !refunds.data) || (payouts.loading && !payouts.data)) return <Loading />;
  return (
    <Panel title="Business Rules" icon={<ShieldCheck size={19} />}>
      <ErrorNote error={refunds.error || payouts.error} />
      <Facts items={[
        ['Refunds', refunds.data?.secondApprover ? 'The administrator who requests a refund cannot approve it.' : 'The requester may approve their own refund.'],
        ['Payouts', payouts.data ? (payouts.data.approvalThresholdXaf === 0 ? 'Every payout needs a second administrator.' : `Payouts above ${xaf(payouts.data.approvalThresholdXaf)} need a second administrator.`) : '—'],
        ['Stock transfers and adjustments', 'Approved by someone other than the requester; stock moves only when approved.'],
        ['Refunds and stock', 'A refund never puts stock back by itself; returned goods are approved separately.'],
        ['Cash shifts', 'A difference at closing needs a reason and a manager review.'],
        ['Commissions', 'Finalised months are frozen; later refunds adjust the current month.'],
      ]} />
      <Note>Change the payout approval rule under <Link className="link" to="/payouts?tab=settings">Payouts → Approval Rule</Link>.</Note>
    </Panel>
  );
}

/** Settings → Integrations: the outside services the platform uses and their status. */
export function IntegrationsTab() {
  const sms = useLoad(() => notificationsApi.settings(), []);
  const methods = useLoad(() => paymentsApi.methods(), []);
  if (sms.loading && !sms.data) return <Loading />;
  return (
    <>
      <Panel title="SMS (Hombee)" icon={<Plug size={19} />}>
        <Facts items={[['Mode', sms.data?.mode === 'live' ? <Pill tone="green">Live</Pill> : <Pill tone="purple">Dev: messages are logged, not sent</Pill>], ['Sender name', sms.data?.sender_id ?? '—']]} />
        <Note>Set LUTIANO_SMS_MODE=live and HOMBEE_SMS_API_KEY on the server to send real messages.</Note>
      </Panel>
      <Panel title="Mobile Money providers" icon={<Plug size={19} />}>
        <Facts items={(methods.data ?? []).filter(m => m.provider).map(m => [m.label, m.available && m.enabled ? <Pill tone="green">Enabled</Pill> : <Pill tone="grey">Not available yet</Pill>] as [string, JSX.Element])} />
      </Panel>
    </>
  );
}

/** Settings → Security. */
export function SecurityTab() {
  const { data, error, loading } = useLoad(() => businessApi.security(), []);
  if (loading && !data) return <Loading />;
  return (
    <Panel title="Security" icon={<KeyRound size={19} />}>
      <ErrorNote error={error} />
      <Facts items={[
        ['Administrator sign-in', data ? (data.mfaEnabled ? `Password, then a ${data.codeLength}-digit SMS code every time.` : 'Password only: two-step sign-in is switched off on this server.') : '—'],
        ['Sign-in code', data ? `Valid for ${data.codeMinutes} minutes, ${data.maxAttempts} tries, then locked.` : '—'],
        ['Sessions', data ? `Expire after ${data.sessionHours} hours; an administrator can sign anyone out of every device.` : '—'], ['Lost phone', 'Another administrator resets 2FA from the user page.'],
        ['Permissions', 'Fixed roles enforced by the server on every request.'], ['Audit', 'Every sensitive action is logged with who, when and from which address.'],
      ]} />
    </Panel>
  );
}

/** Settings → Notifications: a pointer to the Notifications page. */
export function NotificationsPointer() {
  return (
    <Panel title="Notifications" icon={<Bell size={19} />}>
      <p>Your messages, the SMS delivery log, message texts in French and English, and who receives approval alerts.</p>
      <Link className="btn gold" to="/notifications">Open Notifications</Link>
    </Panel>
  );
}
