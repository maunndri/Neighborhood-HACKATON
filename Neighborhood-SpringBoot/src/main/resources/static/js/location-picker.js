document.addEventListener('DOMContentLoaded', () => {
  const mapElement = document.getElementById('location-map');
  if (!mapElement || typeof L === 'undefined') return;
  const latInput = document.getElementById('latitud');
  const lngInput = document.getElementById('longitud');
  const status = document.getElementById('location-status');
  const searchInput = document.getElementById('location-search');
  const searchButton = document.getElementById('location-search-btn');
  const currentButton = document.getElementById('use-location');
  const zonaInput = document.querySelector('input[name="zona"]');

  const DISTRITOS_COORDS = {
    'tiabaya': [-16.4580, -71.5885],
    'tunales': [-16.4535, -71.5825],
    'camarones': [-16.4620, -71.5940],
    'alata': [-16.4620, -71.5940],
    'sachaca': [-16.4260, -71.5650],
    'cercado': [-16.4090, -71.5375],
    'yanahuara': [-16.3920, -71.5460],
    'cayma': [-16.3820, -71.5480],
    'cerro colorado': [-16.3740, -71.5640],
    'bustamante': [-16.4290, -71.5190],
    'paucarpata': [-16.4250, -71.5030],
    'miraflores': [-16.3940, -71.5170],
    'selva alegre': [-16.3810, -71.5220],
    'socabaya': [-16.4580, -71.5280],
    'hunter': [-16.4450, -71.5550],
    'mariano melgar': [-16.4040, -71.5030],
    'uchumayo': [-16.4220, -71.6300],
    'characato': [-16.4670, -71.4880],
    'sabandia': [-16.4670, -71.4880]
  };

  const initialLat = latInput?.value ? Number(latInput.value) : -16.4580;
  const initialLng = lngInput?.value ? Number(lngInput.value) : -71.5885;
  const hasInitial = !!(latInput?.value && lngInput?.value);
  const map = L.map(mapElement).setView([initialLat, initialLng], hasInitial ? 16 : 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '&copy; OpenStreetMap'
  }).addTo(map);
  let marker = null;

  const guardarEnCacheLocal = (lat, lng) => {
    try { localStorage.setItem('bc_pos', JSON.stringify({ lat: Number(lat), lon: Number(lng), ts: Date.now() })); } catch (e) {}
  };

  const setPoint = (lat, lng, message = 'Ubicación seleccionada. Puedes mover el marcador.') => {
    latInput.value = Number(lat).toFixed(7);
    lngInput.value = Number(lng).toFixed(7);
    guardarEnCacheLocal(lat, lng);
    if (!marker) marker = L.marker([lat, lng], {draggable: true}).addTo(map);
    else marker.setLatLng([lat, lng]);
    marker.off('dragend').on('dragend', e => {
      const p = e.target.getLatLng();
      latInput.value = p.lat.toFixed(7);
      lngInput.value = p.lng.toFixed(7);
      guardarEnCacheLocal(p.lat, p.lng);
      if (status) status.textContent = 'Ubicación ajustada en el mapa.';
    });
    map.setView([lat, lng], Math.max(map.getZoom(), 15));
    if (status) status.textContent = message;
  };

  if (hasInitial) {
    setPoint(initialLat, initialLng, 'Ubicación guardada. Puedes mover el marcador.');
  } else {
    // Al crear cuenta o abrir formulario sin ubicación previa, fijamos punto inicial y detectamos GPS automáticamente
    setPoint(initialLat, initialLng, 'Obteniendo tu ubicación actual para el mapa…');
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(pos => {
        setPoint(pos.coords.latitude, pos.coords.longitude, '📍 Tu ubicación GPS actual fue detectada automáticamente.');
      }, () => {
        if (status) status.textContent = 'Ubicación referencial lista. Puedes tocar el mapa o elegir tu distrito.';
      }, {enableHighAccuracy: true, timeout: 10000, maximumAge: 60000});
    }
  }

  // Si en registro cambia el campo "zona" (ej. elige Tiabaya, Yanahuara, Cayma), centrar el mapa en ese distrito
  if (zonaInput) {
    zonaInput.addEventListener('change', () => {
      const val = (zonaInput.value || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      for (const [clave, coords] of Object.entries(DISTRITOS_COORDS)) {
        if (val.includes(clave)) {
          setPoint(coords[0], coords[1], '📍 Ubicación centrada en ' + zonaInput.value + '. Puedes ajustar el marcador.');
          break;
        }
      }
    });
  }

  map.on('click', e => setPoint(e.latlng.lat, e.latlng.lng));

  const search = async () => {
    const query = searchInput?.value.trim();
    if (!query) { if (status) status.textContent = 'Escribe una dirección o lugar para buscar.'; return; }
    if (status) status.textContent = 'Buscando ubicación…';
    if (searchButton) searchButton.disabled = true;
    try {
      const url = 'https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(query);
      const response = await fetch(url, {headers: {'Accept-Language': 'es'}});
      if (!response.ok) throw new Error('No se pudo realizar la búsqueda.');
      const results = await response.json();
      if (!results.length) throw new Error('No encontramos ese lugar. Prueba con otra dirección.');
      setPoint(Number(results[0].lat), Number(results[0].lon), 'Lugar encontrado: ' + results[0].display_name);
    } catch (error) { if (status) status.textContent = error.message || 'Error al buscar la ubicación.'; }
    finally { if (searchButton) searchButton.disabled = false; }
  };
  searchButton?.addEventListener('click', search);
  searchInput?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); search(); } });
  currentButton?.addEventListener('click', () => {
    if (!navigator.geolocation) { if (status) status.textContent = 'Tu navegador no permite obtener la ubicación.'; return; }
    currentButton.disabled = true; if (status) status.textContent = 'Obteniendo tu ubicación GPS actual…';
    navigator.geolocation.getCurrentPosition(pos => {
      setPoint(pos.coords.latitude, pos.coords.longitude, '📍 Tu ubicación actual fue seleccionada y sincronizada con el mapa.');
      currentButton.disabled = false;
    }, err => {
      if (status) status.textContent = err.code === 1 ? 'Permiso denegado. Puedes buscar o marcar el mapa manualmente.' : 'No se pudo obtener tu ubicación. Puedes marcarla en el mapa.';
      currentButton.disabled = false;
    }, {enableHighAccuracy: true, timeout: 15000, maximumAge: 60000});
  });
  setTimeout(() => map.invalidateSize(), 200);
});
