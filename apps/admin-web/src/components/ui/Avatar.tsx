import { mediaUrl } from '../../api/client';
import { initials } from '../../utils/format';

/** The person's photo when they have one, otherwise their initials. */
export function Avatar({ name, url, size = 36 }: { name?: string; url?: string | null; size?: number }) {
  return url
    ? <img className="avatar" src={mediaUrl(url)} alt="" width={size} height={size} loading="lazy" style={{ objectFit: 'cover' }} />
    : <span className="avatar" style={{ width: size, height: size, fontSize: size * 0.38 }} aria-hidden="true">{initials(name)}</span>;
}
