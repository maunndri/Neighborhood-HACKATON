// Neighborhood - comportamiento común a todas las páginas.
window.BC = window.BC || {};

/* ---------- Avisos breves (toasts) ---------- */
window.bcToast = function (mensaje, tipo) {
 let cont = document.getElementById('bc-toasts');
 if (!cont) { cont = document.createElement('div'); cont.id = 'bc-toasts'; cont.className = 'bc-toasts'; cont.setAttribute('aria-live', 'polite'); document.body.appendChild(cont); }
 const t = document.createElement('div');
 t.className = 'bc-toast ' + (tipo || 'info');
 t.textContent = mensaje;
 cont.appendChild(t);
 setTimeout(() => { t.classList.add('out'); setTimeout(() => t.remove(), 400); }, 5200);
};

/* ---------- Modo Adulto Mayor / Lectura Accesible (A11Y) ---------- */
document.addEventListener('DOMContentLoaded', () => {
 const KEY = 'nb_senior_mode';
 const aplicarSenior = (activo) => {
  document.body.classList.toggle('bc-senior-mode', !!activo);
  document.querySelectorAll('[data-toggle-senior]').forEach(b => b.classList.toggle('active', !!activo));
  document.querySelectorAll('[data-senior-label]').forEach(s => {
   s.textContent = activo ? 'Lectura Fácil: ACTIVA' : 'Modo Adulto Mayor';
  });
 };
 let guardado = false;
 try { guardado = localStorage.getItem(KEY) === '1'; } catch (e) {}
 aplicarSenior(guardado);

 document.querySelectorAll('[data-toggle-senior]').forEach(btn => {
  btn.addEventListener('click', e => {
   e.preventDefault();
   const nuevo = !document.body.classList.contains('bc-senior-mode');
   try { localStorage.setItem(KEY, nuevo ? '1' : '0'); } catch (err) {}
   aplicarSenior(nuevo);
   window.bcToast(
    nuevo
     ? '👓 Modo Adulto Mayor activado: letras, contraste y botones ampliados.'
     : 'Modo de vista estándar restaurado.',
    'info'
   );
  });
 });
});

/* ---------- Compartir en Grupo de WhatsApp Vecinal (1 clic) ---------- */
document.addEventListener('click', e => {
 const btn = e.target.closest && e.target.closest('[data-wa-share]');
 if (!btn) return;
 e.preventDefault();
 const textoBase = btn.getAttribute('data-wa-share') || 'Mira esta publicación en Neighborhood';
 const url = window.location.origin + (btn.getAttribute('data-wa-url') || window.location.pathname);
 const mensaje = textoBase + '\n📍 Enlace vecinal: ' + url;
 window.open('https://wa.me/?text=' + encodeURIComponent(mensaje), '_blank', 'noopener');
});

/* ---------- SOS Inteligente (funciona con modal y también sin conexión) ---------- */
document.addEventListener('DOMContentLoaded', () => {
 const btn = document.getElementById('global-sos-button');
 const confirmBtn = document.getElementById('btn-trigger-sos-confirmed');
 const waBtn = document.getElementById('btn-sos-whatsapp');
 const chipsWrap = document.getElementById('sos-category-chips');
 const detailInput = document.getElementById('sos-detail-input');
 const feedback = document.getElementById('sos-modal-feedback');
 const status = document.getElementById('global-sos-status');
 const setStatus = t => { if (status) status.textContent = t; };
 const setFeedback = (t, cls) => {
  if (!feedback) return;
  feedback.className = 'small text-center mt-2 ' + (cls || 'text-secondary');
  feedback.textContent = t;
 };

 let categoriaActual = 'Adulto mayor o persona vulnerable';
 if (chipsWrap) {
  chipsWrap.querySelectorAll('.sos-chip-btn').forEach(chip => {
   chip.addEventListener('click', () => {
    chipsWrap.querySelectorAll('.sos-chip-btn').forEach(c => c.classList.remove('active'));
    chip.classList.add('active');
    categoriaActual = chip.getAttribute('data-cat') || 'SOS';
   });
  });
 }

 if (waBtn) {
  waBtn.addEventListener('click', () => {
   const det = detailInput && detailInput.value.trim() ? ' (' + detailInput.value.trim() + ')' : '';
   const msg = '🚨 *ALERTA SOS VECINAL - NEIGHBORHOOD*\nSituación: ' + categoriaActual + det +
    '\nNecesitamos apoyo urgente de los vecinos más cercanos.\n📍 Centro de ayuda: ' + window.location.origin + '/emergencias';
   window.open('https://wa.me/?text=' + encodeURIComponent(msg), '_blank', 'noopener');
  });
 }

 if (!btn || !confirmBtn) return;

 const obtenerCoordenadas = (cb) => {
  const fallback = () => {
   try {
    const cache = JSON.parse(localStorage.getItem('bc_pos') || 'null');
    if (cache && Number.isFinite(cache.lat) && Number.isFinite(cache.lon)) {
     cb({ latitude: cache.lat, longitude: cache.lon });
     return;
    }
   } catch (e) {}
   // Coordenada referencial por defecto si el navegador bloquea GPS en escritorio
   cb({ latitude: -16.4090, longitude: -71.5375 });
  };
  if (!navigator.geolocation) { fallback(); return; }
  navigator.geolocation.getCurrentPosition(
   pos => cb(pos.coords),
   () => fallback(),
   { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
  );
 };

 confirmBtn.addEventListener('click', () => {
  if (btn.dataset.logged !== 'true') {
   window.location.href = btn.dataset.loginUrl || '/login';
   return;
  }
  confirmBtn.disabled = true;
  setStatus('Enviando SOS…');
  setFeedback('Obteniendo ubicación y avisando a vecinos cercanos…', 'text-danger fw-semibold');

  obtenerCoordenadas(async coords => {
   const datos = {
    latitud: coords.latitude,
    longitud: coords.longitude,
    categoria: categoriaActual,
    detalle: detailInput ? detailInput.value.trim() : ''
   };
   const body = new URLSearchParams(datos).toString();
   const guardarParaDespues = () => {
    if (window.BC.encolar) window.BC.encolar(btn.dataset.sosUrl, body, 'tu alerta SOS');
    setStatus('SOS guardado');
    setFeedback('Sin conexión: tu SOS quedó guardado y se enviará al reconectar.', 'text-warning fw-semibold');
   };
   try {
    if (!navigator.onLine) { guardarParaDespues(); return; }
    const response = await fetch(btn.dataset.sosUrl, {
     method: 'POST',
     headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
     body,
     credentials: 'same-origin'
    });
    const data = await response.json();
    if (!response.ok || !data.ok) throw new Error(data.mensaje || 'No se pudo enviar el SOS.');
    setStatus('SOS activo');
    setFeedback('✅ ' + data.mensaje, 'text-success fw-bold');
    window.bcToast('🚨 ' + data.mensaje + ' Recuerda llamar también al 105, 116 o 106.', 'danger');
    setTimeout(() => { window.location.href = '/emergencias'; }, 1100);
   } catch (err) {
    if (err instanceof TypeError) guardarParaDespues();
    else {
     setStatus('Error al enviar');
     setFeedback(err.message || 'Ocurrió un error al enviar el SOS.', 'text-danger');
    }
   } finally {
    confirmBtn.disabled = false;
   }
  });
 });
});

/* ---------- Confirmaciones para acciones importantes ---------- */
document.addEventListener('DOMContentLoaded', () => {
 const modalNode = document.createElement('div');
 modalNode.className = 'modal fade'; modalNode.tabIndex = -1; modalNode.setAttribute('aria-hidden', 'true');
 modalNode.innerHTML = `<div class="modal-dialog modal-dialog-centered"><div class="modal-content border-0 bc-modal"><div class="modal-body text-center p-4 p-md-5"><div class="bc-modal-icon" id="bc-confirm-icon"><i class="bi bi-question-lg"></i></div><h2 class="h5 fw-bold mb-2" id="bc-confirm-title">¿Confirmas esta acción?</h2><p class="text-secondary mb-4" id="bc-confirm-message"></p><div class="d-flex justify-content-center gap-2"><button type="button" class="btn btn-light border" data-bs-dismiss="modal">Cancelar</button><button type="button" class="btn btn-danger px-4" id="bc-confirm-accept">Sí, continuar</button></div></div></div></div>`;
 document.body.appendChild(modalNode);
 const modal = window.bootstrap ? new bootstrap.Modal(modalNode) : null;
 let pendingForm = null;
 document.addEventListener('submit', event => {
  const submitter = event.submitter;
  if (!submitter || !submitter.hasAttribute('data-confirm') || !modal) return;
  event.preventDefault(); pendingForm = event.target;
  const msg = submitter.getAttribute('data-confirm') || '¿Deseas continuar?';
  const destructive = /eliminar|borrar|retirar|suspender|estado de esta cuenta/i.test(msg);
  document.getElementById('bc-confirm-title').textContent = destructive ? '¿Estás seguro?' : '¿Confirmas esta acción?';
  document.getElementById('bc-confirm-message').textContent = msg;
  const accept = document.getElementById('bc-confirm-accept');
  accept.className = destructive ? 'btn btn-danger px-4' : 'btn btn-success px-4';
  accept.textContent = destructive ? 'Sí, continuar' : 'Sí, confirmo';
  modal.show();
 }, true);
 document.getElementById('bc-confirm-accept').addEventListener('click', () => {
  if (!pendingForm) return;
  const form = pendingForm; pendingForm = null; modal.hide();
  HTMLFormElement.prototype.submit.call(form);
 });
 modalNode.addEventListener('hidden.bs.modal', () => { pendingForm = null; });
});

/* ---------- Reportar una publicación sospechosa ---------- */
document.addEventListener('DOMContentLoaded', () => {
 const botones = document.querySelectorAll('[data-report-tipo]');
 if (!botones.length || !window.bootstrap) return;
 const nodo = document.createElement('div');
 nodo.className = 'modal fade'; nodo.tabIndex = -1; nodo.setAttribute('aria-hidden', 'true');
 nodo.innerHTML = `<div class="modal-dialog modal-dialog-centered"><div class="modal-content border-0 bc-modal"><form method="post" action="/reportar"><div class="modal-body p-4"><h2 class="h5 fw-bold">Reportar publicación</h2><p class="small text-secondary">Un administrador de Neighborhood la revisará. Úsalo si parece falsa, peligrosa o abusiva.</p><input type="hidden" name="tipo" id="bc-rep-tipo"><input type="hidden" name="id" id="bc-rep-id"><select class="form-select mb-2" id="bc-rep-motivo-sel" aria-label="Motivo"><option>Parece un engaño o estafa</option><option>Información falsa</option><option>Contenido ofensivo</option><option>Pone en riesgo a alguien</option><option>Otro motivo</option></select><textarea class="form-control" id="bc-rep-detalle" rows="2" maxlength="250" placeholder="Detalle (opcional)"></textarea><input type="hidden" name="motivo" id="bc-rep-motivo"><div class="d-flex justify-content-end gap-2 mt-3"><button type="button" class="btn btn-light border" data-bs-dismiss="modal">Cancelar</button><button class="btn btn-danger">Enviar reporte</button></div></div></form></div></div>`;
 document.body.appendChild(nodo);
 const modal = new bootstrap.Modal(nodo);
 botones.forEach(b => b.addEventListener('click', () => {
  document.getElementById('bc-rep-tipo').value = b.dataset.reportTipo;
  document.getElementById('bc-rep-id').value = b.dataset.reportId;
  modal.show();
 }));
 nodo.querySelector('form').addEventListener('submit', () => {
  const sel = document.getElementById('bc-rep-motivo-sel').value, det = document.getElementById('bc-rep-detalle').value.trim();
  document.getElementById('bc-rep-motivo').value = det ? sel + ': ' + det : sel;
 });
});

/* ---------- Al cerrar sesión se limpian las páginas guardadas del dispositivo ---------- */
document.addEventListener('click', e => {
 const a = e.target.closest && e.target.closest('a[href$="/logout"]');
 if (a && navigator.serviceWorker && navigator.serviceWorker.controller) navigator.serviceWorker.controller.postMessage('clear-pages');
});
