// Modo sin conexión: registro del service worker, aviso de conexión y cola de publicaciones/peticiones pendientes.
(() => {
 window.BC = window.BC || {};
 const CLAVE = 'bc_cola';
 const leer = () => { try { return JSON.parse(localStorage.getItem(CLAVE) || '[]'); } catch (e) { return []; } };
 const guardar = l => { try { localStorage.setItem(CLAVE, JSON.stringify(l)); } catch (e) {} };

 // Guarda una petición POST para enviarla automáticamente cuando vuelva la conexión.
 window.BC.encolar = (url, body, etiqueta) => {
  const l = leer();
  l.push({ url, body, etiqueta: etiqueta || 'tu solicitud', ts: Date.now() });
  guardar(l);
  if ('serviceWorker' in navigator && 'SyncManager' in window) {
   navigator.serviceWorker.ready.then(reg => reg.sync.register('bc-sync-cola')).catch(() => {});
  }
  window.bcToast('📥 Sin conexión: guardamos ' + (etiqueta || 'tu solicitud') + ' en la cola local. Se procesará sola al reconectar.', 'warn');
  actualizarBanner();
 };

 const banner = () => document.getElementById('bc-offline-banner');
 function actualizarBanner() {
  const b = banner(); if (!b) return;
  const cola = leer();
  const pendientes = cola.length;
  const offline = !navigator.onLine;
  b.classList.toggle('d-none', !offline && pendientes === 0);
  if (offline) {
   const resumen = pendientes
    ? ' <strong>(' + pendientes + ' petición' + (pendientes > 1 ? 'es' : '') + ' en cola lista' + (pendientes > 1 ? 's' : '') + ' para enviarse al reconectar)</strong>'
    : '';
   b.innerHTML = '<i class="bi bi-wifi-off me-2"></i>Modo Offline activo (Caché local): puedes consultar datos guardados y números de emergencia (105, 116, 106, 115).' + resumen;
  } else if (pendientes > 0) {
   b.innerHTML = '<i class="bi bi-arrow-repeat me-2"></i>Sincronizando ' + pendientes + ' petición(es) realizada(s) sin conexión…';
  }
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
    const r = await fetch(item.url, {
     method: 'POST',
     headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
     body: item.body,
     credentials: 'same-origin'
    });
    if (r.url && r.url.includes('/login')) {
     window.bcToast('Inicia sesión para terminar de procesar tus solicitudes guardadas en offline.', 'warn');
     break;
    }
    if (!r.ok) throw new Error('http ' + r.status);
    cola.shift(); guardar(cola); enviados++;
   } catch (e) { break; }
  }
  enviando = false; actualizarBanner();
  if (enviados) {
   window.bcToast('✅ Se procesaron y enviaron ' + enviados + ' solicitud(es) que hiciste sin conexión.', 'ok');
   if (navigator.serviceWorker && navigator.serviceWorker.controller) {
    navigator.serviceWorker.controller.postMessage('warmup-pages');
   }
   setTimeout(() => { if (navigator.onLine) window.location.reload(); }, 1400);
  }
 }

 // Serializa un formulario ignorando archivos vacíos para poder encolarlo en texto plano.
 function serializarFormulario(f) {
  const fd = new FormData(f);
  const params = new URLSearchParams();
  for (const [k, v] of fd.entries()) {
   if (!(v instanceof File)) params.append(k, v);
  }
  return params.toString();
 }

 // Los formularios marcados con data-offline se guardan en cola si no hay internet.
 document.addEventListener('submit', e => {
  const f = e.target;
  if (!f.matches || !f.matches('form[data-offline]') || navigator.onLine) return;
  e.preventDefault();
  const fileInput = f.querySelector('input[type="file"]');
  const conArchivo = fileInput && fileInput.files && fileInput.files.length > 0;
  window.BC.encolar(f.action, serializarFormulario(f), f.dataset.offlineLabel || 'tu solicitud');
  if (conArchivo) {
   window.bcToast('Nota: los datos del formulario se guardaron sin la foto adjunta porque estás sin internet.', 'info');
  }
  f.reset();
 });

 window.addEventListener('online', () => {
  window.bcToast('🌐 Volvió la conexión a internet. Sincronizando datos…', 'ok');
  vaciarCola();
 });
 window.addEventListener('offline', () => {
  window.bcToast('📡 Sin conexión a internet. Navegando con datos en caché; tus solicitudes se encolarán.', 'warn');
  actualizarBanner();
 });
 document.addEventListener('DOMContentLoaded', () => { actualizarBanner(); vaciarCola(); });

 // El service worker actúa como proceso en segundo plano (requiere HTTPS o localhost).
 if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
   navigator.serviceWorker.register('/sw.js').then(reg => {
    reg.update().catch(() => {});
    if (navigator.onLine && reg.active) reg.active.postMessage('warmup-pages');
   }).catch(() => {});
  });
  navigator.serviceWorker.addEventListener('message', e => {
   if (e.data === 'flush-offline-queue') vaciarCola();
  });
 }
})();
