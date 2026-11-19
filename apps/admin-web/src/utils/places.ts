/** Approximate centres of Cameroon's regions (their capitals) and of the towns offered in the location form: [latitude, longitude]. */
type LatLng = [number, number];

const REGIONS: Record<string, LatLng> = {
  Adamawa: [7.32, 13.58], Centre: [3.87, 11.52], East: [4.58, 13.68], 'Far North': [10.59, 14.32], Littoral: [4.05, 9.7],
  North: [9.3, 13.4], 'North-West': [5.96, 10.15], South: [2.9, 11.15], 'South-West': [4.15, 9.24], West: [5.48, 10.42],
};

const CITIES: Record<string, LatLng> = {
  'Ngaoundéré': [7.32, 13.58], Meiganga: [6.52, 14.37], Tibati: [6.47, 12.63], 'Tignère': [7.37, 12.65], Banyo: [6.75, 11.82],
  'Yaoundé': [3.87, 11.52], Mbalmayo: [3.52, 11.5], Obala: [4.17, 11.53], Bafia: [4.75, 11.23], Soa: [3.97, 11.6], Mfou: [3.72, 11.64],
  'Eséka': [3.65, 10.77], Akonolinga: [3.77, 12.25], 'Nanga-Eboko': [4.68, 12.37],
  Bertoua: [4.58, 13.68], Batouri: [4.43, 14.36], 'Abong-Mbang': [3.98, 13.18], Yokadouma: [3.52, 15.05], 'Bélabo': [4.93, 13.3],
  Maroua: [10.59, 14.32], 'Kousséri': [12.08, 15.03], Mokolo: [10.74, 13.8], Yagoua: [10.34, 15.24], Mora: [11.05, 14.14], 'Kaélé': [10.11, 14.45],
  Douala: [4.05, 9.7], 'Edéa': [3.8, 10.13], Nkongsamba: [4.95, 9.94], Loum: [4.72, 9.73], Mbanga: [4.5, 9.57], Manjo: [4.85, 9.82], Yabassi: [4.46, 9.97],
  Garoua: [9.3, 13.4], Guider: [9.93, 13.95], Figuil: [9.76, 13.96], Pitoa: [9.38, 13.52], Poli: [8.48, 12.88], 'Tcholliré': [8.4, 14.17],
  Bamenda: [5.96, 10.15], Kumbo: [6.2, 10.68], Ndop: [5.97, 10.43], Wum: [6.38, 10.07], 'Nkambé': [6.6, 10.67], Mbengwi: [6.0, 10.0], Bali: [5.89, 10.0],
  Ebolowa: [2.9, 11.15], Kribi: [2.94, 9.91], 'Sangmélima': [2.93, 11.98], Ambam: [2.39, 11.28], Lolodorf: [3.23, 10.73], Campo: [2.37, 9.82],
  Buea: [4.15, 9.24], Limbe: [4.02, 9.21], Kumba: [4.64, 9.45], Tiko: [4.08, 9.36], Mutengene: [4.09, 9.31], Mamfe: [5.77, 9.29], Idenau: [4.0, 8.99],
  Bafoussam: [5.48, 10.42], Dschang: [5.45, 10.07], Foumban: [5.73, 10.9], Mbouda: [5.63, 10.25], Bafang: [5.16, 10.18], 'Bangangté': [5.15, 10.52], Baham: [5.3, 10.37],
};

export const CITY_ZOOM = 13, REGION_ZOOM = 8;

/** Where to centre the map for a chosen region and city: the town when known, else the region; null when neither is known. */
export function placeView(region?: string, city?: string): { center: LatLng; zoom: number; name: string } | null {
  if (city && CITIES[city]) return { center: CITIES[city], zoom: CITY_ZOOM, name: city };
  if (region && REGIONS[region]) return { center: REGIONS[region], zoom: REGION_ZOOM, name: region };
  return null;
}

/** Great-circle distance in kilometres. */
export function distanceKm(a: LatLng, b: LatLng): number {
  const rad = (d: number) => (d * Math.PI) / 180;
  const h = Math.sin(rad(b[0] - a[0]) / 2) ** 2 + Math.cos(rad(a[0])) * Math.cos(rad(b[0])) * Math.sin(rad(b[1] - a[1]) / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(h));
}
