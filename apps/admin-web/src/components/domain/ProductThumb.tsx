import { Coffee } from 'lucide-react';
import { mediaUrl } from '../../api/client';

export function ProductThumb({ url, size = 46 }: { url?: string | null; size?: number }) {
  return url
    ? <img className="thumb" src={mediaUrl(url)} alt="" width={size} height={size} loading="lazy" />
    : <span className="thumb ph" style={{ width: size, height: size }} aria-hidden="true"><Coffee size={size * 0.45} /></span>;
}

/** Full-bleed product image with a coffee placeholder. */
export function ProductImage({ url }: { url?: string | null }) {
  return <div className="img">{url ? <img src={mediaUrl(url)} alt="" loading="lazy" /> : <Coffee size={42} aria-hidden="true" />}</div>;
}
