// Avisos dentro de la app: consulta periódica, insignia en la campana y aviso del navegador si hay permiso.
document.addEventListener('DOMContentLoaded', () => {
 const nav = document.querySelector('[data-notif-url]');
 const pushBtn = document.getElementById('enable-push');
 if (pushBtn && 'Notification' in window && Notification.permission !== 'granted') {
  pushBtn.classList.remove('d-none');
  pushBtn.addEventListener('click', async () => { const p = await Notification.requestPermission(); window.bcToast(p === 'granted' ? 'Avisos del navegador activados.' : 'No se concedió el permiso.', p === 'granted' ? 'ok' : 'warn'); if (p === 'granted') pushBtn.classList.add('d-none'); });
 }
 if (!nav || nav.dataset.logged !== 'true') return;
 const badge = document.getElementById('bc-notif-badge');
 let ultimo = Number(localStorage.getItem('bc_notif_ultimo') || 0), primera = true;
 async function consultar() {
  if (!navigator.onLine || document.hidden) return;
  try {
   const r = await fetch(nav.dataset.notifUrl, { credentials: 'same-origin', cache: 'no-store' });
   if (!r.ok) return;
   const d = await r.json();
   if (badge) { badge.textContent = d.noLeidas; badge.classList.toggle('d-none', !d.noLeidas); }
   const nuevas = (d.ultimas || []).filter(n => n.id > ultimo).sort((a, b) => a.id - b.id);
   if (nuevas.length) {
    ultimo = Math.max(ultimo, ...nuevas.map(n => n.id)); localStorage.setItem('bc_notif_ultimo', String(ultimo));
    if (!primera) nuevas.forEach(n => {
     window.bcToast('🔔 ' + n.mensaje, 'info');
     if ('Notification' in window && Notification.permission === 'granted') { try { new Notification('Neighborhood', { body: n.mensaje, tag: 'bc-' + n.id }); } catch (e) {} }
    });
   }
   primera = false;
  } catch (e) { /* sin red: se reintenta */ }
 }
 consultar(); setInterval(consultar, 20000);
});
