/* Neighborhood - service worker (modo sin conexión).
   - Páginas: red primero; si no hay red, la última copia guardada o /offline.
   - Archivos estáticos y librerías: copia guardada con actualización en segundo plano.
   - /api/mapa/publicaciones: red primero con copia de respaldo.
   Las publicaciones hechas sin conexión las guarda la página (offline.js) y se envían al reconectar. */
const VERSION = 'v2';
const PAGINAS = 'nb-paginas-' + VERSION, ESTATICO = 'nb-estatico-' + VERSION, MAPA = 'nb-mapa-' + VERSION;
const PRECARGA = ['/offline', '/css/styles.css', '/js/app.js', '/js/offline.js', '/js/tutorial.js', '/js/notificaciones.js', '/js/distancia.js', '/js/mapa.js', '/js/location-picker.js', '/manifest.webmanifest', '/images/icon-192.png', '/images/brand/neighborhood-mark.svg'];
const CDN = ['https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css', 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js', 'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css', 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css', 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'];

self.addEventListener('install', e => {
 e.waitUntil((async () => {
  const c = await caches.open(ESTATICO);
  await Promise.all([...PRECARGA, ...CDN].map(u => c.add(u).catch(() => {})));
  await self.skipWaiting();
 })());
});

self.addEventListener('activate', e => {
 e.waitUntil((async () => {
  for (const k of await caches.keys()) if (![PAGINAS, ESTATICO, MAPA].includes(k)) await caches.delete(k);
  await self.clients.claim();
 })());
});

self.addEventListener('message', e => { if (e.data === 'clear-pages') { caches.delete(PAGINAS); caches.delete(MAPA); } });

async function redPrimero(req, nombre, respaldo) {
 const cache = await caches.open(nombre);
 try {
  const res = await fetch(req);
  if (res && res.ok && !res.redirected) cache.put(req, res.clone());
  return res;
 } catch (err) {
  const guardada = await cache.match(req);
  if (guardada) return guardada;
  if (respaldo) { const r = await caches.match(respaldo); if (r) return r; }
  return new Response('Sin conexión', { status: 503, headers: { 'Content-Type': 'text/plain; charset=utf-8' } });
 }
}

async function guardadaYActualiza(req) {
 const cache = await caches.open(ESTATICO);
 const guardada = await cache.match(req);
 const red = fetch(req).then(res => { if (res && (res.ok || res.type === 'opaque')) cache.put(req, res.clone()); return res; }).catch(() => null);
 return guardada || (await red) || new Response('', { status: 504 });
}

self.addEventListener('fetch', e => {
 const req = e.request;
 if (req.method !== 'GET') return;
 const url = new URL(req.url);
 if (req.mode === 'navigate') { e.respondWith(redPrimero(req, PAGINAS, '/offline')); return; }
 if (url.origin === location.origin) {
  if (url.pathname.startsWith('/api/mapa/')) { e.respondWith(redPrimero(req, MAPA)); return; }
  if (url.pathname.startsWith('/api/')) return; // avisos y SOS: siempre en vivo
  if (/^\/(css|js|images|uploads)\//.test(url.pathname) || url.pathname === '/manifest.webmanifest') { e.respondWith(guardadaYActualiza(req)); return; }
  return;
 }
 if (/(jsdelivr|unpkg|tile\.openstreetmap)/.test(url.hostname)) e.respondWith(guardadaYActualiza(req));
});
