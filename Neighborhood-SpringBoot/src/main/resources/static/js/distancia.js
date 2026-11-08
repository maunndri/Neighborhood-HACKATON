// Neighborhood · Hiperproximidad: distancia aproximada, minutos caminando (sin transporte mayor) y ahorro en pasaje.
// La ubicación del visitante no se envía ni se guarda en el servidor; solo se recuerda en este dispositivo.
(() => {
 const rad = x => x * Math.PI / 180;
 const km = (a, b, c, d) => {
  const h = Math.sin(rad(c - a) / 2) ** 2 + Math.cos(rad(a)) * Math.cos(rad(c)) * Math.sin(rad(d - b) / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.sqrt(h));
 };
 const formato = k => k < 1 ? '≈ ' + Math.max(50, Math.round(k * 20) * 50) + ' m' : '≈ ' + (k < 10 ? k.toFixed(1) : Math.round(k)) + ' km';
 const etiquetaProximidad = d => {
  if (d <= 1.25) {
   const min = Math.max(1, Math.round((d * 1000) / 80));
   return `<i class="bi bi-person-walking"></i> ${min} min a pie (${formato(d)}) · <strong>S/ 0 pasaje</strong>`;
  }
  if (d <= 3.5) {
   const minBici = Math.max(3, Math.round((d * 1000) / 220));
   return `<i class="bi bi-bicycle"></i> ${minBici} min en bici (${formato(d)})`;
  }
  return `<i class="bi bi-geo-alt-fill"></i> ${formato(d)}`;
 };
 window.BC = window.BC || {};
 window.BC.distanciaKm = km; window.BC.formatoDistancia = formato;

 document.addEventListener('DOMContentLoaded', () => {
  const cards = [...document.querySelectorAll('[data-geo-card]')];
  const status = document.getElementById('distance-status');
  const msg = (html) => { if (status) status.innerHTML = html; };
  if (!cards.length) return;
  const tiene = c => c.dataset.lat && c.dataset.lon && Number.isFinite(Number(c.dataset.lat)) && Number.isFinite(Number(c.dataset.lon));
  cards.forEach(c => {
   const l = c.querySelector('[data-distance]'); if (!l) return;
   if (!tiene(c)) { l.textContent = 'Sin ubicación'; l.classList.add('no-location'); c.dataset.km = ''; }
  });
  const aplicar = (lat, lon, origen) => {
   cards.forEach(c => {
    const l = c.querySelector('[data-distance]'); if (!l || !tiene(c)) return;
    const d = km(lat, lon, Number(c.dataset.lat), Number(c.dataset.lon));
    c.dataset.km = d;
    c.dataset.walkable = d <= 1.5 ? 'true' : 'false';
    l.innerHTML = etiquetaProximidad(d);
    l.classList.remove('no-location');
    l.classList.toggle('walkable-badge', d <= 1.25);
   });
   if (origen === 'gps') {
    msg('<i class="bi bi-person-walking text-success me-1"></i><strong>Hiperproximidad activa:</strong> calculando tiempo a pie y ahorro de transporte desde tu ubicación.');
   } else if (origen === 'cache') {
    msg('<i class="bi bi-check-circle-fill text-success me-1"></i>Distancias a pie según tu última ubicación en este dispositivo.');
   } else {
    msg('<i class="bi bi-person-walking text-success me-1"></i>Mostrando tiempos a pie desde el centro referencial del barrio (activa tu GPS para precisión exacta).');
   }
  };
  const cache = () => { try { return JSON.parse(localStorage.getItem('bc_pos') || 'null'); } catch (e) { return null; } };
  const usarRespaldo = () => {
   const p = cache();
   if (p && Number.isFinite(p.lat) && Number.isFinite(p.lon)) aplicar(p.lat, p.lon, 'cache');
   else aplicar(-16.4580, -71.5885, 'referencia');
  };
  const ordenar = () => {
   document.querySelectorAll('[data-geo-list]').forEach(lista => {
    const hijos = [...lista.children];
    hijos.sort((x, y) => {
     const cx = x.hasAttribute('data-geo-card') ? x : x.querySelector('[data-geo-card]');
     const cy = y.hasAttribute('data-geo-card') ? y : y.querySelector('[data-geo-card]');
     const dx = cx && cx.dataset.km ? Number(cx.dataset.km) : Infinity;
     const dy = cy && cy.dataset.km ? Number(cy.dataset.km) : Infinity;
     return dx - dy;
    });
    hijos.forEach(h => lista.appendChild(h));
   });
   msg('<i class="bi bi-sort-down text-success me-1"></i>Ordenado por cercanía a pie (sin transporte mayor).');
  };
  document.querySelectorAll('[data-distance-sort]').forEach(b => b.addEventListener('click', ordenar));

  let soloAPie = false;
  document.querySelectorAll('[data-walkable-filter]').forEach(btn => {
   btn.addEventListener('click', () => {
    soloAPie = !soloAPie;
    document.querySelectorAll('[data-walkable-filter]').forEach(b => b.classList.toggle('active', soloAPie));
    cards.forEach(c => {
     const wrapper = c.parentElement && c.parentElement.hasAttribute('data-geo-list') ? c : (c.closest('[data-geo-list] > *') || c);
     const d = c.dataset.km ? Number(c.dataset.km) : Infinity;
     wrapper.classList.toggle('d-none', soloAPie && d > 1.5);
    });
    msg(soloAPie
     ? '<i class="bi bi-person-walking text-success me-1"></i>Mostrando solo publicaciones a distancia caminable (≤ 15 min a pie · S/ 0 pasaje).'
     : '<i class="bi bi-grid me-1"></i>Mostrando todas las publicaciones del barrio.');
   });
  });

  if (!navigator.geolocation) { usarRespaldo(); return; }
  usarRespaldo();
  navigator.geolocation.getCurrentPosition(pos => {
   try { localStorage.setItem('bc_pos', JSON.stringify({ lat: pos.coords.latitude, lon: pos.coords.longitude, ts: Date.now() })); } catch (e) {}
   aplicar(pos.coords.latitude, pos.coords.longitude, 'gps');
  }, () => {}, { enableHighAccuracy: false, timeout: 8000, maximumAge: 120000 });
 });
})();
