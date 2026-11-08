/* Neighborhood - service worker (modo sin conexión real con Cache Storage + Background Sync).
   - Páginas: red primero; precarga en segundo plano de las vistas clave; si no hay red, devuelve la copia en caché o /offline.
   - Archivos estáticos y librerías: copia guardada con actualización en segundo plano (Stale-While-Revalidate).
   - /api/mapa/publicaciones: red primero con copia de respaldo en caché.
   - Las peticiones/solicitudes hechas sin conexión se encolan en el navegador (offline.js) y se envían automáticamente al reconectar. */
const VERSION = 'v6';
const PAGINAS = 'nb-paginas-' + VERSION, ESTATICO = 'nb-estatico-' + VERSION, MAPA = 'nb-mapa-' + VERSION;
const PRECARGA = [
 '/offline',
 '/css/styles.css',
 '/js/app.js',
 '/js/offline.js',
 '/js/tutorial.js',
 '/js/notificaciones.js',
 '/js/distancia.js',
 '/js/mapa.js',
 '/js/location-picker.js',
 '/manifest.webmanifest',
 '/images/icon-192.png',
 '/images/icon-512.png',
 '/images/brand/neighborhood-mark.svg',
 '/images/photos/servicios-oficios.jpg',
 '/images/photos/favores-herramientas.jpg',
 '/images/photos/comunidad-adultos.jpg',
 '/images/photos/emergencia-brigada.jpg'
];
const PAGINAS_CLAVE = [
 '/',
 '/login',
 '/registro',
 '/servicios',
 '/favores',
 '/comunidades',
 '/comunidades/1',
 '/comunidades/2',
 '/comunidades/3',
 '/emergencias',
 '/emergencias/grupos',
 '/mapa',
 '/servicios/nuevo',
 '/favores/nuevo',
 '/emergencias/nuevo',
 '/emergencias/grupos/nuevo'
];
const CDN = [
 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css',
 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js',
 'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css',
 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'
];

async function precargarPaginasClave() {
 const cPaginas = await caches.open(PAGINAS);
 await Promise.all(PAGINAS_CLAVE.map(async u => {
  try {
   const r = await fetch(u, { credentials: 'same-origin', cache: 'no-cache' });
   if (r && r.ok && !r.redirected) await cPaginas.put(u, r.clone());
  } catch (err) {}
 }));
 try {
  const cMapa = await caches.open(MAPA);
  const rMapa = await fetch('/api/mapa/publicaciones?radioKm=5', { credentials: 'same-origin', cache: 'no-cache' });
  if (rMapa && rMapa.ok) {
   await cMapa.put('/api/mapa/publicaciones?radioKm=5', rMapa.clone());
   const items = await rMapa.json();
   const enlaces = [...new Set((items || []).map(i => i.enlace).filter(e => e && e.startsWith('/servicios/')))];
   await Promise.all(enlaces.map(async u => {
    try {
     const r = await fetch(u, { credentials: 'same-origin', cache: 'no-cache' });
     if (r && r.ok && !r.redirected) await cPaginas.put(u, r.clone());
    } catch (e) {}
   }));
  }
 } catch (err) {}
}

self.addEventListener('install', e => {
 e.waitUntil((async () => {
  const c = await caches.open(ESTATICO);
  await Promise.all([...PRECARGA, ...CDN].map(u => c.add(u).catch(() => {})));
  await precargarPaginasClave();
  await self.skipWaiting();
 })());
});

self.addEventListener('activate', e => {
 e.waitUntil((async () => {
  for (const k of await caches.keys()) if (![PAGINAS, ESTATICO, MAPA].includes(k)) await caches.delete(k);
  await self.clients.claim();
 })());
});

self.addEventListener('message', e => {
 if (e.data === 'clear-pages') {
  caches.delete(PAGINAS);
  caches.delete(MAPA);
 } else if (e.data === 'warmup-pages') {
  precargarPaginasClave();
 }
});

self.addEventListener('sync', e => {
 if (e.tag === 'bc-sync-cola') {
  e.waitUntil((async () => {
   const clientes = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
   for (const cliente of clientes) cliente.postMessage('flush-offline-queue');
  })());
 }
});

async function redPrimero(req, nombre, respaldo) {
 const cache = await caches.open(nombre);
 try {
  const res = await fetch(req);
  if (res && res.ok && !res.redirected) cache.put(req, res.clone());
  return res;
 } catch (err) {
  const guardada = await cache.match(req);
  if (guardada) return guardada;
  const sinQuery = await cache.match(req, { ignoreSearch: true });
  if (sinQuery) return sinQuery;
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
