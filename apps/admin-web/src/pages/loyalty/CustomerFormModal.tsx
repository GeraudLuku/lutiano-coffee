import { useState } from 'react';
import { customersApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field, FormModal, Note, Switch } from '../../components/ui';
import type { CustomerDetail } from '../../types/models';

/** Adds or edits a loyalty member. Full customer management (self-registration, OTP verification) arrives with A05. */
export function CustomerFormModal({ customer, onClose, onDone }: { customer?: CustomerDetail; onClose: () => void; onDone: (c: CustomerDetail) => void }) {
  const [name, setName] = useState(customer?.full_name ?? '');
  const [phone, setPhone] = useState(customer?.phone ?? '');
  const [verified, setVerified] = useState(customer?.phone_verified ?? false);
  const [status, setStatus] = useState(customer?.status ?? 'ACTIVE');
  const { busy, error, run } = useAction();

  async function submit() {
    const body = { fullName: name, phone, phoneVerified: verified, status };
    const saved = await run(() => (customer ? customersApi.update(customer.id, body) : customersApi.create(body)), customer ? 'Member updated.' : 'Member added.');
    if (saved) onDone(saved);
  }

  return (
    <FormModal title={customer ? 'Edit member' : 'Add loyalty member'} submitLabel={customer ? 'Save' : 'Add Member'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Full name" required><input className="input" value={name} onChange={e => setName(e.target.value)} required maxLength={150} autoFocus /></Field>
      <Field label="Phone number" required><input className="input" value={phone} onChange={e => setPhone(e.target.value)} required maxLength={30} placeholder="+237 6XX XX XX XX" /></Field>
      <Field label="Verification" group><Switch checked={verified} onChange={setVerified} label="Phone number verified" /></Field>
      {customer && (
        <Field label="Status">
          <select className="input" value={status} onChange={e => setStatus(e.target.value as 'ACTIVE' | 'SUSPENDED')}>
            <option value="ACTIVE">Active</option><option value="SUSPENDED">Suspended (cannot earn or redeem)</option>
          </select>
        </Field>
      )}
      <Note>Members earn points only when they show their loyalty QR at checkout. A seller typing a phone number never awards points.</Note>
    </FormModal>
  );
}
