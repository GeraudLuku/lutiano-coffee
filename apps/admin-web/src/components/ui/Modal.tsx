import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';

export function Modal({ title, onClose, children, footer }: { title: string; onClose: () => void; children: ReactNode; footer?: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    ref.current?.querySelector<HTMLElement>('input,select,textarea')?.focus();
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} ref={ref}>
        <header><h3>{title}</h3><button className="icon-btn" onClick={onClose} aria-label="Close"><X size={18} /></button></header>
        <div className="modal-body">{children}</div>
        {footer && <footer>{footer}</footer>}
      </div>
    </div>
  );
}

/** Yes/no confirmation for destructive or sensitive actions. */
export function ConfirmDialog({ title, children, confirmLabel, danger, busy, onConfirm, onClose }: {
  title: string; children: ReactNode; confirmLabel: string; danger?: boolean; busy?: boolean; onConfirm: () => void; onClose: () => void;
}) {
  return (
    <Modal title={title} onClose={onClose}
      footer={<><button className="btn" onClick={onClose}>Cancel</button><button className={danger ? 'btn danger-solid' : 'btn gold'} disabled={busy} onClick={onConfirm}>{busy ? 'Working…' : confirmLabel}</button></>}>
      {children}
    </Modal>
  );
}

/** Form inside a modal with Cancel / submit buttons and an error slot. */
export function FormModal({ title, submitLabel, busy, error, onSubmit, onClose, children }: {
  title: string; submitLabel: string; busy: boolean; error?: string; onSubmit: () => void; onClose: () => void; children: ReactNode;
}) {
  return (
    <Modal title={title} onClose={onClose}>
      <form className="stack" onSubmit={e => { e.preventDefault(); onSubmit(); }}>
        {children}
        {error && <div className="error-note" role="alert">{error}</div>}
        <div className="modal-actions">
          <button type="button" className="btn" onClick={onClose}>Cancel</button>
          <button className="btn gold" disabled={busy}>{busy ? 'Saving…' : submitLabel}</button>
        </div>
      </form>
    </Modal>
  );
}
