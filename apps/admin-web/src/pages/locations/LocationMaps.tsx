import { useEffect, useMemo, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { LocateFixed } from 'lucide-react';
import type { Operation, OperationType } from '../../types/models';
import { TYPE_LABEL } from '../../utils/labels';
import { CITY_ZOOM, distanceKm, placeView } from '../../utils/places';

/** OpenStreetMap tiles; their usage policy requires the attribution shown in the map corner. */
const TILES = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
const DEFAULT_CENTER: [number, number] = [4.05, 9.72];
/** A pin further than this from the chosen town is probably in the wrong place. */
const FAR_KM = 40;

export type Position = { lat: number; lng: number };

function createMap(el: HTMLElement, center: [number, number], zoom: number) {
  const map = L.map(el, { center, zoom, scrollWheelZoom: false });
  L.tileLayer(TILES, { maxZoom: 19, attribution: ATTRIBUTION }).addTo(map);
  return map;
}

/** A teardrop pin coloured by location type; grey when inactive. */
function pin(type: OperationType | undefined, active = true) {
  const tone = !active ? 'off' : type === 'MOBILE' ? 'mobile' : type === 'MARKET_AGENT' ? 'market' : 'fixed';
  return L.divIcon({ className: '', html: `<span class="map-pin ${tone}"></span>`, iconSize: [28, 28], iconAnchor: [14, 28], tooltipAnchor: [0, -26] });
}

const esc = (s: string) => s.replace(/[&<>"']/g, c => `&#${c.charCodeAt(0)};`);

/**
 * Every given location with a position, as a pin that opens it. City buttons narrow the view when the locations span
 * several cities; it opens on the city with the most locations. The view re-fits when the pins or the map's size change.
 */
export function LocationsMap({ locations, onOpen }: { locations: Operation[]; onOpen: (id: string) => void }) {
  const el = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const layer = useRef<L.LayerGroup | null>(null);
  const bounds = useRef<L.LatLngBounds | null>(null);
  const open = useRef(onOpen);
  open.current = onOpen;

  const placed = useMemo(() => locations.filter(o => o.latitude != null && o.longitude != null), [locations]);
  const cities = useMemo(() => {
    const counts = new Map<string, number>();
    for (const o of placed) if (o.city) counts.set(o.city, (counts.get(o.city) ?? 0) + 1);
    return [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).map(([c]) => c);
  }, [placed]);
  /** null = not chosen yet (the busiest city), '' = all cities. */
  const [city, setCity] = useState<string | null>(null);
  const shownCity = city === '' ? null : city && cities.includes(city) ? city : cities.length > 1 ? cities[0] : null;

  const fit = () => {
    if (map.current && bounds.current) map.current.fitBounds(bounds.current, { padding: [36, 36], maxZoom: 14 });
  };

  useEffect(() => {
    const m = createMap(el.current!, DEFAULT_CENTER, 12);
    map.current = m;
    layer.current = L.layerGroup().addTo(m);
    const resize = new ResizeObserver(() => { m.invalidateSize(); fit(); });
    resize.observe(el.current!);
    return () => { resize.disconnect(); m.remove(); map.current = null; };
  }, []);

  useEffect(() => {
    const group = layer.current;
    if (!group) return;
    group.clearLayers();
    for (const o of placed) {
      L.marker([o.latitude!, o.longitude!], { icon: pin(o.type, !!o.active), title: o.name, keyboard: true })
        .bindTooltip(`<b>${esc(o.name)}</b><br>${esc(o.code)} · ${TYPE_LABEL[o.type]}${o.active ? '' : ' · Inactive'}`)
        .on('click', () => open.current(o.id))
        .addTo(group);
    }
  }, [placed]);

  useEffect(() => {
    const inView = shownCity ? placed.filter(o => o.city === shownCity) : placed;
    bounds.current = inView.length ? L.latLngBounds(inView.map(o => [o.latitude!, o.longitude!])) : null;
    fit();
  }, [placed, shownCity]);

  return (
    <div className="loc-map-wrap">
      <div ref={el} className="loc-map" role="region" aria-label="Map of sales locations" />
      {cities.length > 1 && (
        <div className="map-cities" role="group" aria-label="Show city">
          {cities.map(c => <button key={c} type="button" aria-pressed={shownCity === c} onClick={() => setCity(c)}>{c}</button>)}
          <button type="button" aria-pressed={shownCity === null} onClick={() => setCity('')}>All</button>
        </div>
      )}
    </div>
  );
}

/** Click (or drag the pin) to set a location's position. The map moves to the region or city whenever one is chosen. */
export function PositionPicker({ value, region, city, type, onChange }: {
  value: Position | null; region: string; city: string; type: OperationType; onChange: (p: Position) => void;
}) {
  const el = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const marker = useRef<L.Marker | null>(null);
  const change = useRef(onChange);
  change.current = onChange;
  const place = placeView(region, city);

  useEffect(() => {
    const start = value ? { center: [value.lat, value.lng] as [number, number], zoom: 16 } : place ?? { center: DEFAULT_CENTER, zoom: CITY_ZOOM };
    const m = createMap(el.current!, start.center, start.zoom);
    m.on('click', e => change.current({ lat: e.latlng.lat, lng: e.latlng.lng }));
    map.current = m;
    return () => { m.remove(); map.current = null; marker.current = null; };
  // The map is created once; later changes are applied by the effects below.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const m = map.current;
    if (!m) return;
    if (!value) { marker.current?.remove(); marker.current = null; return; }
    if (!marker.current) {
      marker.current = L.marker([value.lat, value.lng], { draggable: true, icon: pin(type) }).addTo(m);
      marker.current.on('dragend', () => { const p = marker.current!.getLatLng(); change.current({ lat: p.lat, lng: p.lng }); });
    } else {
      marker.current.setLatLng([value.lat, value.lng]);
      marker.current.setIcon(pin(type));
    }
    if (!m.getBounds().contains([value.lat, value.lng])) m.setView([value.lat, value.lng], Math.max(m.getZoom(), 15));
  }, [value, type]);

  // Show the chosen region or city. Skipped on first render so editing opens on the saved pin.
  const first = useRef(true);
  useEffect(() => {
    if (first.current) { first.current = false; return; }
    if (place) map.current?.setView(place.center, place.zoom);
  // Keyed on the names, not the object, which is new on every render.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [region, city]);

  const here = () => navigator.geolocation?.getCurrentPosition(p => {
    change.current({ lat: p.coords.latitude, lng: p.coords.longitude });
    map.current?.setView([p.coords.latitude, p.coords.longitude], 16);
  });
  const far = !!(value && place && place.zoom === CITY_ZOOM && distanceKm(place.center, [value.lat, value.lng]) > FAR_KM);

  return (
    <div className="pos-picker">
      <div ref={el} className="loc-map small" role="application" aria-label="Click the map to set the location's position" />
      <div className="pos-row">
        <span className={value ? '' : 'muted'}>{value ? `${value.lat.toFixed(6)}, ${value.lng.toFixed(6)}` : 'Click the map to place the pin.'}</span>
        {'geolocation' in navigator && <button type="button" className="btn sm" onClick={here}><LocateFixed size={15} />Use my current position</button>}
      </div>
      {far && <p className="pos-warn">This pin is over {FAR_KM} km from {place!.name}. Move it if that is not intended.</p>}
    </div>
  );
}
