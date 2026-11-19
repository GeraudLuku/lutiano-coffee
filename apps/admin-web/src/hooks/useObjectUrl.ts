import { useEffect, useState } from 'react';

/** Turns a Blob/File into an object URL and revokes it when it changes or on unmount. */
export function useObjectUrl(source: Blob | null | undefined): string | undefined {
  const [url, setUrl] = useState<string>();
  useEffect(() => {
    if (!source) { setUrl(undefined); return; }
    const u = URL.createObjectURL(source);
    setUrl(u);
    return () => URL.revokeObjectURL(u);
  }, [source]);
  return url;
}
