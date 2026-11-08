document.addEventListener('DOMContentLoaded', () => {
 const el = document.getElementById('map'); if (!el || !window.L) return;
 const cfg = window.BARRIO_MAP_CONFIG || {}, status = document.getElementById('map-status'), radius = document.getElementById('map-radius');
 const needsBox = document.getElementById('map-needs');
 let center = (Number.isFinite(cfg.latitud) && Number.isFinite(cfg.longitud)) ? [cfg.latitud, cfg.longitud] : [-16.458, -71.5885];
 let personal = Number.isFinite(cfg.latitud) && Number.isFinite(cfg.longitud), firstLoad = true, items = [];
 const seenSos = new Set();
 const map = L.map('map').setView(center, personal ? 14 : 12);
 L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(map);
 const markers = L.layerGroup().addTo(map);
 const colors = { servicio: '#147d64', necesito: '#2563eb', ofrezco: '#7c3aed', emergencia: '#dc3545', recurso: '#f59e0b' };
 const safe = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
 const capa = p => p.clase === 'favor' ? (p.tipo === 'ofrezco' ? 'ofrezco' : 'necesito') : p.clase;
 const activas = () => new Set([...document.querySelectorAll('[data-layer]:checked')].map(c => c.dataset.layer));
 const dist = p => (personal && window.BC && window.BC.distanciaKm) ? window.BC.distanciaKm(center[0], center[1], p.latitud, p.longitud) : null;
 const fmtDist = d => d == null ? '' : (d < 1 ? '≈ ' + Math.max(50, Math.round(d * 20) * 50) + ' m' : '≈ ' + d.toFixed(1) + ' km');

 function etiqueta(p) {
  if (p.clase === 'emergencia') return (p.categoria === 'SOS' || (p.titulo && p.titulo.startsWith('ALERTA SOS'))) ? '🚨 ALERTA SOS' : 'Necesidad urgente';
  if (p.clase === 'favor') return p.tipo === 'ofrezco' ? 'Quiere ayudar' : 'Necesita ayuda';
  if (p.clase === 'recurso') return 'Recurso de apoyo';
  return p.tipo === 'producto' ? 'Producto' : 'Servicio';
 }
 function accion(p) {
  const href = safe(p.enlace || '/');
  if (p.clase === 'favor') return `<a class="btn btn-sm btn-primary mt-2" href="${href}">${p.tipo === 'ofrezco' ? 'Lo necesito' : 'Puedo ayudar'}</a>`;
  if (p.clase === 'emergencia') return `<a class="btn btn-sm btn-danger mt-2" href="${href}">Puedo ayudar</a>`;
  if (p.clase === 'recurso') return `<a class="btn btn-sm btn-warning mt-2" href="${href}">Ver recurso</a>`;
  return `<a class="btn btn-sm btn-outline-success mt-2" href="${href}">Ver y dar visto bueno</a>`;
 }
 function popup(p) {
  const sos = p.clase === 'emergencia' && (p.categoria === 'SOS' || (p.titulo && p.titulo.startsWith('ALERTA SOS'))), d = fmtDist(dist(p));
  const estado = (p.clase === 'favor' || p.clase === 'emergencia') ? `<span class="badge text-bg-light ms-1">${safe(p.estado)}</span>` : '';
  const nivel = p.clase === 'servicio' && p.estado === 'MUY' ? '<div class="small text-success">★ Muy recomendado por vecinos</div>' : (p.clase === 'servicio' && p.estado === 'RECOMENDADO' ? '<div class="small text-success">Recomendado por vecinos</div>' : '');
  const ver = p.verificado ? '<div class="small text-primary">✔ Verificado por un administrador</div>' : '';
  return `<div style="max-width:250px"><span class="badge" style="background:${sos ? '#b91c1c' : colors[capa(p)]}">${safe(etiqueta(p))}</span>${estado}<h3 class="h6 fw-bold mt-2">${safe(p.titulo)}</h3><div class="small text-muted mb-1">${safe(p.autor)}${d ? ' · ' + d : ''}</div><p class="small mb-1">${safe(p.descripcion)}</p>${nivel}${ver}${p.categoria && p.clase !== 'emergencia' ? `<div class="small text-muted">${safe(p.categoria)}</div>` : ''}${accion(p)}</div>`;
 }
 function notifySos(p) {
  if ('Notification' in window && Notification.permission === 'granted') { try { new Notification('🚨 ALERTA SOS - Neighborhood', { body: `${p.autor} necesita ayuda. ${p.titulo}`, tag: `sos-${p.id}`, requireInteraction: true }); } catch (e) {} }
  try { const Ctx = window.AudioContext || window.webkitAudioContext; if (Ctx) { const ctx = new Ctx(), osc = ctx.createOscillator(), gain = ctx.createGain(); osc.type = 'sine'; osc.frequency.value = 880; gain.gain.value = .12; osc.connect(gain); gain.connect(ctx.destination); osc.start(); osc.stop(ctx.currentTime + .45); osc.onended = () => ctx.close(); } } catch (e) {}
 }
 function pintar() {
  const on = activas(); markers.clearLayers();
  const visibles = items.filter(p => on.has(capa(p)));
  visibles.forEach(p => {
   const sos = p.clase === 'emergencia' && (p.categoria === 'SOS' || (p.titulo && p.titulo.startsWith('ALERTA SOS')));
   L.circleMarker([p.latitud, p.longitud], { radius: sos ? 13 : 9, color: '#fff', weight: sos ? 3 : 2, fillColor: sos ? '#b91c1c' : colors[capa(p)], fillOpacity: .98 }).addTo(markers).bindPopup(popup(p), { maxWidth: 280 });
  });
  const necesidades = items.filter(p => p.clase === 'emergencia' || (p.clase === 'favor' && p.tipo !== 'ofrezco'))
   .map(p => ({ p, d: dist(p) })).sort((a, b) => (a.d ?? 1e9) - (b.d ?? 1e9)).slice(0, 5);
  if (needsBox) needsBox.innerHTML = necesidades.length ? necesidades.map(({ p, d }) => `<a class="need-link" href="${safe(p.enlace)}"><strong>${safe(p.titulo)}</strong><span>${safe(p.estado)}${d != null ? ' · ' + fmtDist(d) : ''}</span></a>`).join('') : 'No hay necesidades abiertas en este radio.';
  return visibles.length;
 }
 async function load() {
  status.textContent = 'Buscando publicaciones cercanas...';
  const q = new URLSearchParams({ radioKm: radius.value || 5 }); if (personal) { q.set('lat', center[0]); q.set('lng', center[1]); }
  try {
   const res = await fetch(`${cfg.apiUrl}?${q}`, { cache: 'no-store' }); if (!res.ok) throw Error();
   items = await res.json();
   try { localStorage.setItem('bc_mapa_cache', JSON.stringify({ ts: Date.now(), items })); } catch (e) {}
   items.forEach(p => { if (p.clase === 'emergencia' && p.categoria === 'SOS') { const k = String(p.id); if (!firstLoad && !seenSos.has(k)) notifySos(p); seenSos.add(k); } });
   firstLoad = false; const n = pintar();
   status.textContent = `${n} publicación(es) en el radio seleccionado. Se actualiza automáticamente.`;
  } catch (e) {
   let cache = null; try { cache = JSON.parse(localStorage.getItem('bc_mapa_cache') || 'null'); } catch (x) {}
   if (cache) { items = cache.items; pintar(); const min = Math.round((Date.now() - cache.ts) / 60000); status.textContent = `Sin conexión: mostrando datos guardados hace ${min < 1 ? 'instantes' : min + ' min'}.`; }
   else status.textContent = 'No se pudieron cargar publicaciones. Comprueba tu conexión.';
  }
 }
 function locate() {
  if (!navigator.geolocation) { status.textContent = 'Tu navegador no admite geolocalización.'; return; }
  status.textContent = 'Solicitando permiso de ubicación...';
  navigator.geolocation.getCurrentPosition(pos => { center = [pos.coords.latitude, pos.coords.longitude]; personal = true; map.setView(center, 14); L.marker(center).addTo(map).bindPopup('Tu ubicación de referencia').openPopup(); load(); },
   () => status.textContent = 'No se obtuvo tu ubicación. Configúrala desde Mi perfil.', { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 });
 }
 document.getElementById('map-my-location')?.addEventListener('click', locate);
 radius?.addEventListener('change', load);
 document.querySelectorAll('[data-layer]').forEach(c => c.addEventListener('change', pintar));
 document.getElementById('map-enable-notifications')?.addEventListener('click', async () => {
  if (!('Notification' in window)) { status.textContent = 'Este navegador no admite notificaciones.'; return; }
  const permission = await Notification.requestPermission();
  status.textContent = permission === 'granted' ? 'Avisos activados. Mantén esta página abierta para recibir alertas.' : 'No se concedió permiso para notificaciones.';
 });
 document.getElementById('map-enable-notifications')?.classList.remove('d-none');
 if (personal) L.circle(center, { radius: 90, color: '#147d64', fillOpacity: .08 }).addTo(map).bindPopup('Tu zona de referencia');
 load(); setInterval(load, 10000);
});
