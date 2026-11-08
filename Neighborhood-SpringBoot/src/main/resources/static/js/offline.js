// Modo sin conexión: registro del service worker, aviso de conexión y cola de publicaciones pendientes.
(() => {
 window.BC = window.BC || {};
 const CLAVE = 'bc_cola';
 const leer = () => { try { return JSON.parse(localStorage.getItem(CLAVE) || '[]'); } catch (e) { return []; } };
 const guardar = l => { try { localStorage.setItem(CLAVE, JSON.stringify(l)); } catch (e) {} };

 // Guarda una petición POST para enviarla cuando vuelva la conexión.
 window.BC.encolar = (url, body, etiqueta) => {
  const l = leer(); l.push({ url, body, etiqueta: etiqueta || 'tu publicación', ts: Date.now() }); guardar(l);
  window.bcToast('Sin conexión: guardamos ' + (etiqueta || 'tu publicación') + '. Se enviará sola al reconectar.', 'warn');
  actualizarBanner();
 };

 const banner = () => document.getElementById('bc-offline-banner');
 function actualizarBanner() {
  const b = banner(); if (!b) return;
  const pendientes = leer().length;
  const offline = !navigator.onLine;
  b.classList.toggle('d-none', !offline && pendientes === 0);
  b.innerHTML = offline
   ? '<i class="bi bi-wifi-off me-2"></i>Sin conexión. Puedes ver lo último guardado; lo que publiques se enviará al reconectar.' + (pendientes ? ' <strong>(' + pendientes + ' pendiente' + (pendientes > 1 ? 's' : '') + ')</strong>' : '')
   : '<i class="bi bi-arrow-repeat me-2"></i>Enviando ' + pendientes + ' publicación(es) guardada(s)…';
 }

 let enviando = false;
 async function vaciarCola() {
  if (enviando || !navigator.onLine) return;
  let cola = leer(); if (!cola.length) { actualizarBanner(); return; }
  enviando = true; actualizarBanner();
  let enviados = 0;
  while (cola.length && navigator.onLine) {
   const item = cola[0];
   try {
    const r = await fetch(item.url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: item.body, credentials: 'same-origin' });
    if (r.url && r.url.includes('/login')) { window.bcToast('Inicia sesión para enviar lo que guardaste sin conexión.', 'warn'); break; }
    if (!r.ok) throw new Error('http ' + r.status);
    cola.shift(); guardar(cola); enviados++;
   } catch (e) { break; }
  }
  enviando = false; actualizarBanner();
  if (enviados) window.bcToast('Se enviaron ' + enviados + ' publicación(es) guardada(s) sin conexión.', 'ok');
 }

 // Los formularios marcados con data-offline se guardan si no hay internet.
 document.addEventListener('submit', e => {
  const f = e.target;
  if (!f.matches || !f.matches('form[data-offline]') || navigator.onLine) return;
  e.preventDefault();
  window.BC.encolar(f.action, new URLSearchParams(new FormData(f)).toString(), f.dataset.offlineLabel);
  f.reset();
 });

 window.addEventListener('online', () => { window.bcToast('Volvió la conexión.', 'ok'); vaciarCola(); });
 window.addEventListener('offline', () => { window.bcToast('Te quedaste sin conexión. Seguirás viendo lo último guardado.', 'warn'); actualizarBanner(); });
 document.addEventListener('DOMContentLoaded', () => { actualizarBanner(); vaciarCola(); });

 // El service worker requiere HTTPS o localhost.
 if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js').catch(() => {}));
 }
})();
