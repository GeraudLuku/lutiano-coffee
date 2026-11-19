import { AlertCircle, Download, Printer, QrCode } from 'lucide-react';
import { operationsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { useObjectUrl } from '../../hooks/useObjectUrl';
import { ErrorNote, Panel } from '../../components/ui';
import { saveFile } from '../../utils/files';
import { TYPE_LABEL } from '../../utils/labels';
import type { OperationDetail } from '../../types/models';
import { printQr } from './qrPrint';

/** The location's permanent QR code with Download and Print (design: location Overview). */
export function QrCodePanel({ op }: { op: OperationDetail }) {
  const { data: png, error } = useLoad(() => operationsApi.qrImage(op.id), [op.id]);
  const src = useObjectUrl(png);
  return (
    <Panel title="Permanent QR Code" icon={<QrCode size={19} />}>
      <div className="qr-mini">
        <div className="qr-box">{src ? <img src={src} alt={`QR code for ${op.name}`} /> : error ? <AlertCircle /> : <QrCode size={64} className="muted" />}</div>
        <ul className="checks small">
          <li>This QR code identifies this sales operation.</li>
          <li>It stays the same even if the assigned seller changes.</li>
          <li>Customers scan it to start a purchase.</li>
        </ul>
      </div>
      <ErrorNote error={error} />
      <div className="qr-actions">
        <button className="btn gold" disabled={!png} onClick={() => png && saveFile(png, `${op.code}-qr.png`)}><Download size={17} />Download QR</button>
        <button className="btn" disabled={!src} onClick={() => printQr(src, op.code, op.name, `${op.code} • ${TYPE_LABEL[op.type]}`)}><Printer size={17} />Print</button>
      </div>
      {!op.active && <p className="muted small">This location is inactive, so its QR code cannot be used for purchases.</p>}
    </Panel>
  );
}
