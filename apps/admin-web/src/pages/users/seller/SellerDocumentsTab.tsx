import { useState } from 'react';
import { Check, Download, FileText, Upload, X } from 'lucide-react';
import { staffApi } from '../../../api/services';
import { useAction } from '../../../hooks/useAction';
import { DataTable, Empty, ErrorNote, Field, FormGrid, FormModal, Note, Panel, Pill, TitleCell } from '../../../components/ui';
import { EligibilityPill } from '../../../components/domain';
import { date, dateTime, doualaDate } from '../../../utils/format';
import { saveFile } from '../../../utils/files';
import { DOCUMENT_TYPE_LABEL } from '../../../utils/labels';
import type { DocumentType, SellerDocument, StaffDetail } from '../../../types/models';

const STATUS_TONE: Record<SellerDocument['status'], string> = { PENDING: 'amber', VERIFIED: 'green', REJECTED: 'red' };
const STATUS_LABEL: Record<SellerDocument['status'], string> = { PENDING: 'Awaiting review', VERIFIED: 'Verified', REJECTED: 'Rejected' };

/** Eligibility documents: upload, review (verify / reject with reason), download. Files are private to administrators. */
export function SellerDocumentsTab({ user, onChanged }: { user: StaffDetail; onChanged: () => void }) {
  const [uploading, setUploading] = useState(false);
  const [rejecting, setRejecting] = useState<SellerDocument | null>(null);
  const action = useAction();
  const today = doualaDate();

  async function verify(d: SellerDocument) {
    if (await action.run(() => staffApi.verifyDocument(user.id, d.id), `${DOCUMENT_TYPE_LABEL[d.doc_type]} verified.`)) onChanged();
  }
  async function download(d: SellerDocument) {
    await action.run(async () => saveFile(await staffApi.documentFile(user.id, d.id), d.file_name));
  }

  return (
    <div className="stack">
      <Panel title="Eligibility Documents" icon={<FileText size={19} />}
        action={<button className="btn gold sm" onClick={() => setUploading(true)}><Upload size={15} />Upload document</button>}>
        <p><EligibilityPill eligibility={user.eligibility} /> <span className="muted small">Required: {user.eligibility?.required.map(t => DOCUMENT_TYPE_LABEL[t]).join(', ')}. Each needs a verified, unexpired document.</span></p>
        <ErrorNote error={action.error} />
        {!user.documents?.length ? <Empty title="No documents yet" text="Upload the seller's ID and signed contract, then verify them." /> : (
          <DataTable rows={user.documents} rowKey={d => d.id} columns={[
            { header: 'Document', cell: d => <TitleCell title={DOCUMENT_TYPE_LABEL[d.doc_type]} sub={d.file_name} /> },
            { header: 'Expires', cell: d => (d.expires_on ? <span className={d.expires_on < today ? 'warn' : ''}>{date(`${d.expires_on}T12:00:00Z`)}{d.expires_on < today ? ' (expired)' : ''}</span> : 'No expiry') },
            { header: 'Uploaded', cell: d => <TitleCell title={dateTime(d.uploaded_at)} sub={d.uploaded_by_name ?? undefined} /> },
            { header: 'Status', cell: d => <TitleCell title={<Pill tone={STATUS_TONE[d.status]}>{STATUS_LABEL[d.status]}</Pill>} sub={[d.reviewed_by_name, d.review_note].filter(Boolean).join(': ') || undefined} /> },
            { header: '', align: 'right', cell: d => (
              <div className="row-actions">
                <button className="btn sm" onClick={() => download(d)} aria-label={`Download ${d.file_name}`}><Download size={14} />View</button>
                {d.status !== 'VERIFIED' && <button className="btn sm gold" disabled={action.busy} onClick={() => verify(d)}><Check size={14} />Verify</button>}
                {d.status === 'PENDING' && <button className="btn sm" disabled={action.busy} onClick={() => setRejecting(d)}><X size={14} />Reject</button>}
              </div>
            ) },
          ]} />
        )}
        <Note>Documents are stored privately and can only be opened by administrators. Every view is recorded in the audit log.</Note>
      </Panel>
      {uploading && <UploadModal userId={user.id} onClose={() => setUploading(false)} onDone={() => { setUploading(false); onChanged(); }} />}
      {rejecting && <RejectModal userId={user.id} doc={rejecting} onClose={() => setRejecting(null)} onDone={() => { setRejecting(null); onChanged(); }} />}
    </div>
  );
}

function UploadModal({ userId, onClose, onDone }: { userId: string; onClose: () => void; onDone: () => void }) {
  const [type, setType] = useState<DocumentType>('NATIONAL_ID');
  const [expiresOn, setExpiresOn] = useState('');
  const [note, setNote] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const { busy, error, setError, run } = useAction();
  async function submit() {
    if (!file) { setError('Choose a file.'); return; }
    if (file.size > 10 * 1024 * 1024) { setError('Files must be 10 MB or smaller.'); return; }
    if (await run(() => staffApi.uploadDocument(userId, { type, expiresOn, note, file }), 'Document uploaded. Review it to count towards eligibility.')) onDone();
  }
  return (
    <FormModal title="Upload document" submitLabel="Upload" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <FormGrid>
        <Field label="Document type" required>
          <select className="input" value={type} onChange={e => setType(e.target.value as DocumentType)}>
            {Object.entries(DOCUMENT_TYPE_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </Field>
        <Field label="Expiry date" hint="Leave empty if it does not expire."><input className="input" type="date" value={expiresOn} onChange={e => setExpiresOn(e.target.value)} /></Field>
      </FormGrid>
      <Field label="File" required hint="PDF, JPEG, PNG or WebP, up to 10 MB.">
        <input className="input" type="file" accept="application/pdf,image/jpeg,image/png,image/webp" onChange={e => setFile(e.target.files?.[0] ?? null)} required />
      </Field>
      <Field label="Note"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={500} /></Field>
    </FormModal>
  );
}

function RejectModal({ userId, doc, onClose, onDone }: { userId: string; doc: SellerDocument; onClose: () => void; onDone: () => void }) {
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  async function submit() {
    if (await run(() => staffApi.rejectDocument(userId, doc.id, note), 'Document rejected.')) onDone();
  }
  return (
    <FormModal title={`Reject ${DOCUMENT_TYPE_LABEL[doc.doc_type]}`} submitLabel="Reject" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Reason" required hint="e.g. Photo unreadable, name does not match."><textarea className="input" rows={3} value={note} onChange={e => setNote(e.target.value)} required maxLength={500} /></Field>
    </FormModal>
  );
}
