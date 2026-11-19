import { useEffect, useState } from 'react';
import { BellRing, Radio } from 'lucide-react';
import { notificationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Facts, Loading, Note, Panel, Pill, Switch } from '../../components/ui';
import { ROLE_LABEL } from '../../utils/labels';

/** How SMS is sent right now, and who receives "waiting for approval" messages. */
export function SettingsTab() {
  const { data, error, loading, reload } = useLoad(() => notificationsApi.settings(), []);
  const [chosen, setChosen] = useState<Set<string>>(new Set());
  const { busy, error: saveError, run } = useAction();
  useEffect(() => { if (data) setChosen(new Set(data.approvers.filter(a => a.approval_alerts).map(a => a.id))); }, [data]);
  if (loading && !data) return <Loading />;
  const toggle = (id: string, on: boolean) => setChosen(prev => { const n = new Set(prev); if (on) n.add(id); else n.delete(id); return n; });
  const dirty = data ? data.approvers.some(a => a.approval_alerts !== chosen.has(a.id)) : false;
  return (
    <>
      <ErrorNote error={error} retry={reload} />
      <Panel title="SMS channel" icon={<Radio size={19} />}>
        <Facts items={[
          ['Mode', data?.mode === 'live' ? <Pill tone="green">Live: messages are sent through Hombee</Pill> : <Pill tone="purple">Dev: messages are written to the API log, not sent</Pill>],
          ['Sender name', data?.sender_id ?? '—'],
          ['Push notifications', <Pill tone="grey">Not available yet</Pill>],
        ]} />
        {data?.mode === 'dev' && <Note tone="warn">Set LUTIANO_SMS_MODE=live and HOMBEE_SMS_API_KEY on the server to send real messages. In dev mode the configured dev code is accepted at sign-in.</Note>}
      </Panel>
      <Panel title="Approval alerts" icon={<BellRing size={19} />}>
        <Note>These people receive an SMS when a refund, payout, cash outflow or stock request is waiting for approval (never the person who made the request). Only people with a valid mobile number can be chosen.</Note>
        <ErrorNote error={saveError} />
        <ul className="check-list">
          {(data?.approvers ?? []).map(a => (
            <li key={a.id}>
              <span>{a.full_name}<small className="block">{ROLE_LABEL[a.role]} · {a.has_phone ? a.phone_masked : 'No valid mobile number'}</small></span>
              <Switch checked={chosen.has(a.id)} onChange={on => toggle(a.id, on)} disabled={!a.has_phone} label={chosen.has(a.id) ? 'Receives alerts' : 'Off'} />
            </li>
          ))}
        </ul>
        <div className="form-actions"><button className="btn gold" disabled={!dirty || busy} onClick={async () => { if (await run(() => notificationsApi.saveApprovers([...chosen]), 'Approval alerts saved.')) reload(); }}>{busy ? 'Saving…' : 'Save'}</button></div>
      </Panel>
    </>
  );
}
