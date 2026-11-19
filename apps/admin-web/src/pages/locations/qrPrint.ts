import { toast } from '../../components/ui';

/** Opens a print view with one QR code, its location name and a subtitle. {@code src} is an image URL (e.g. an object URL). */
export function printQr(src: string | undefined, code: string, name: string, subtitle: string) {
  const w = window.open('', '_blank', 'width=800,height=1000');
  if (!w || !src) { toast('Allow pop-ups to print the QR code.', 'error'); return; }
  const doc = w.document;
  doc.title = `${code} QR`;
  const style = doc.createElement('style');
  style.textContent = 'body{font-family:sans-serif;text-align:center;padding:40px}img{width:420px;height:420px}h1{margin:16px 0 4px}p{color:#555}';
  const img = doc.createElement('img'); img.src = src; img.alt = 'QR code';
  const h = doc.createElement('h1'); h.textContent = name;
  const p = doc.createElement('p'); p.textContent = subtitle;
  doc.head.appendChild(style);
  doc.body.append(img, h, p);
  img.onload = () => w.print();
}
