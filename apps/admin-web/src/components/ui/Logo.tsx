/** The LUTIANO mark from the design: a golden coffee bean, tilted, split by an S-shaped crease. */
export function Logo({ size = 44 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <defs>
        <linearGradient id="logo-gold" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#E8B95E" />
          <stop offset="1" stopColor="#C8973C" />
        </linearGradient>
      </defs>
      <g transform="rotate(30 32 32)">
        <ellipse cx="32" cy="32" rx="19" ry="27" fill="url(#logo-gold)" />
        <path d="M33 5C20 18 44 44 31 59" fill="none" stroke="#0B1F3A" strokeWidth="3.4" strokeLinecap="round" />
      </g>
    </svg>
  );
}
