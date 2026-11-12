// Semana 14 · SEC-04: movido desde reportar.html para que la CSP no necesite 'unsafe-inline' en scripts.
(function () {
  var mapa = L.map('mapa-reporte').setView([-5.1945, -80.6328], 13);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
  var marca = null;
  var inLat = document.getElementById('lat'), inLng = document.getElementById('lng');
  var estado = document.getElementById('mapa-estado'), quitar = document.getElementById('btn-quitar');
  var textoInicial = estado.textContent;

  function poner(lat, lng) {
    lat = Math.round(lat * 1e6) / 1e6; lng = Math.round(lng * 1e6) / 1e6;
    if (marca) { marca.setLatLng([lat, lng]); } else { marca = L.marker([lat, lng]).addTo(mapa); }
    inLat.value = lat; inLng.value = lng;
    estado.textContent = 'Punto marcado: ' + lat.toFixed(5) + ', ' + lng.toFixed(5);
    quitar.hidden = false;
  }

  mapa.on('click', function (e) { poner(e.latlng.lat, e.latlng.lng); });

  document.getElementById('btn-ubicacion').addEventListener('click', function () {
    if (!navigator.geolocation) { estado.textContent = 'Tu navegador no permite obtener la ubicación. Toca el mapa para marcarla.'; return; }
    estado.textContent = 'Obteniendo ubicación…';
    navigator.geolocation.getCurrentPosition(function (p) {
      poner(p.coords.latitude, p.coords.longitude);
      mapa.setView([p.coords.latitude, p.coords.longitude], 17);
    }, function () { estado.textContent = 'No se pudo obtener tu ubicación. Toca el mapa para marcarla.'; });
  });

  quitar.addEventListener('click', function () {
    if (marca) { mapa.removeLayer(marca); marca = null; }
    inLat.value = ''; inLng.value = '';
    estado.textContent = textoInicial; quitar.hidden = true;
  });
})();
