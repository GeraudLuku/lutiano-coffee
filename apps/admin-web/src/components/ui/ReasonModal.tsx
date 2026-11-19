import { useState } from 'react';
import { FormModal } from './Modal';
import { Field } from './Form';

export interface ReasonText { title: string; label: string; confirm: string; required?: boolean }

/** Asks for the note or reason an action needs (reject, cancel, approve with note…). Required unless `required` is false. */
export function ReasonModal({ prompt, busy, error, onSubmit, onClose }: {
  prompt: ReasonText; busy: boolean; error?: string; onSubmit: (note: string) => void; onClose: () => void;
}) {
  const [note, setNote] = useState('');
  const required = prompt.required !== false;
  return (
    <FormModal title={prompt.title} submitLabel={prompt.confirm} busy={busy} error={error} onSubmit={() => onSubmit(note)} onClose={onClose}>
      <Field label={prompt.label} required={required}>
        <textarea className="input" rows={3} value={note} onChange={e => setNote(e.target.value)} required={required} maxLength={500} />
      </Field>
    </FormModal>
  );
}
