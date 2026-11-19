import { useEffect, useState } from 'react';
import { TOAST_EVENT } from '../../utils/toast';

export { toast } from '../../utils/toast';

export function Toasts() {
  const [items, setItems] = useState<{ id: number; message: string; kind: string }[]>([]);
  useEffect(() => {
    const on = (e: Event) => {
      const { message, kind } = (e as CustomEvent).detail;
      const id = Date.now() + Math.random();
      setItems(x => [...x, { id, message, kind }]);
      setTimeout(() => setItems(x => x.filter(i => i.id !== id)), 4000);
    };
    window.addEventListener(TOAST_EVENT, on);
    return () => window.removeEventListener(TOAST_EVENT, on);
  }, []);
  return <div className="toasts" role="status" aria-live="polite">{items.map(t => <div key={t.id} className={`toast ${t.kind}`}>{t.message}</div>)}</div>;
}
