document.addEventListener('DOMContentLoaded', () => {
  const mapElement = document.getElementById('location-map');
  if (!mapElement || typeof L === 'undefined') return;
  const latInput = document.getElementById('latitud');
  const lngInput = document.getElementById('longitud');
  const status = document.getElementById('location-status');
  const searchInput = document.getElementById('location-search');
  const searchButton = document.getElementById('location-search-btn');
  const currentButton = document.getElementById('use-location');
  const initialLat = latInput?.value ? Number(latInput.value) : -16.4090;
  const initialLng = lngInput?.value ? Number(lngInput.value) : -71.5375;
  const hasInitial = !!(latInput?.value && lngInput?.value);
  const map = L.map(mapElement).setView([initialLat, initialLng], hasInitial ? 16 : 12);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '&copy; OpenStreetMap'
  }).addTo(map);
  let marker = null;
  const setPoint = (lat, lng, message = 'Ubicación seleccionada. Puedes mover el marcador.') => {
    latInput.value = Number(lat).toFixed(7);
    lngInput.value = Number(lng).toFixed(7);
    if (!marker) marker = L.marker([lat, lng], {draggable: true}).addTo(map);
    else marker.setLatLng([lat, lng]);
    marker.off('dragend').on('dragend', e => {
      const p = e.target.getLatLng(); latInput.value = p.lat.toFixed(7); lngInput.value = p.lng.toFixed(7);
      if (status) status.textContent = 'Ubicación ajustada.';
    });
    map.setView([lat, lng], Math.max(map.getZoom(), 15));
    if (status) status.textContent = message;
  };
  if (hasInitial) setPoint(initialLat, initialLng, 'Ubicación guardada. Puedes mover el marcador.');
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
    currentButton.disabled = true; if (status) status.textContent = 'Obteniendo tu ubicación…';
    navigator.geolocation.getCurrentPosition(pos => {
      setPoint(pos.coords.latitude, pos.coords.longitude, 'Tu ubicación actual fue seleccionada.'); currentButton.disabled = false;
    }, err => {
      if (status) status.textContent = err.code === 1 ? 'Permiso denegado. Puedes buscar o marcar el mapa manualmente.' : 'No se pudo obtener tu ubicación. Puedes marcarla en el mapa.';
      currentButton.disabled = false;
    }, {enableHighAccuracy: true, timeout: 15000, maximumAge: 60000});
  });
  setTimeout(() => map.invalidateSize(), 200);
});
