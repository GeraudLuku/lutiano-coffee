import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, Download, Printer, QrCode } from 'lucide-react';
import { operationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { useObjectUrl } from '../../hooks/useObjectUrl';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { DataTable, ErrorNote, FilterBar, PageHeader, Pager, Panel, SearchInput, SelectFilter, TitleCell, ListState } from '../../components/ui';
import { StatusPill, TypePill } from '../../components/domain';
import { saveFile } from '../../utils/files';
import { TYPE_LABEL } from '../../utils/labels';
import type { Operation } from '../../types/models';
import { TypeIcon } from './locationTypes';
import { printQr } from './qrPrint';

/** QR Code Management (design: Sales Locations board, panel 4): every location's QR with Download and Print, and a bulk download. */
export function QrCodesPage() {
  const nav = useNavigate();
  const [q, setQ, search] = useDebouncedState('');
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => operationsApi.list({ q: search, type, active: status, page, size }), [search, type, status]);
  const bulk = useAction();
  return (
    <>
      <PageHeader title="QR Code Management" subtitle="View, download and print the QR codes of all your sales locations." crumbs={[['Sales Locations', '/locations'], ['QR Codes']]}
        actions={<button className="btn gold" disabled={bulk.busy || !data?.total} onClick={() => bulk.run(async () => saveFile(await operationsApi.qrZip({ type, active: status }), 'lutiano-qr-codes.zip'))}>
          <Download size={16} />{bulk.busy ? 'Preparing…' : 'Bulk Download'}</button>} />
      <ErrorNote error={error || bulk.error} retry={reload} />
      <Panel>
        <FilterBar>
          <SearchInput value={q} onChange={setQ} placeholder="Search location, code or city…" />
          <SelectFilter label="Type" value={type} onChange={setType} allLabel="All Types" options={Object.entries(TYPE_LABEL)} />
          <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Status" options={[['true', 'Active'], ['false', 'Inactive']]} />
        </FilterBar>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No sales locations' }}>
          <DataTable rows={items} rowKey={o => o.id} columns={[
            { header: 'Location', cell: o => <TitleCell lead={<TypeIcon type={o.type} />} title={<button className="link-btn" onClick={() => nav(`/locations/${o.id}`)}>{o.name}</button>} sub={o.code} /> },
            { header: 'Type', cell: o => <TypePill type={o.type} /> },
            { header: 'Location / Area', cell: o => [o.address, o.city].filter(Boolean).join(', ') || '—' },
            { header: 'QR Code', cell: o => <QrCell op={o} /> },
            { header: 'Status', cell: o => <StatusPill active={o.active} /> },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
    </>
  );
}

function QrCell({ op }: { op: Operation }) {
  const { data: png, error } = useLoad(() => operationsApi.qrImage(op.id), [op.id]);
  const src = useObjectUrl(png);
  return (
    <div className="qr-cell">
      <div className="qr-thumb">{src ? <img src={src} alt={`QR code for ${op.name}`} /> : error ? <AlertCircle size={20} /> : <QrCode size={22} className="muted" />}</div>
      <button className="btn sm" disabled={!png} onClick={() => png && saveFile(png, `${op.code}-qr.png`)}><Download size={14} />Download</button>
      <button className="btn sm" disabled={!src} onClick={() => printQr(src, op.code, op.name, `${op.code} • ${TYPE_LABEL[op.type]}`)}><Printer size={14} />Print</button>
    </div>
  );
}
