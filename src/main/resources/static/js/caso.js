/* v1.1 · detalle del caso: fotos que ya no existen y mapa con el punto. Sin scripts en línea (CSP). */
(function () {
  function sinFoto(img) {
    var enlace = img.closest('a') || img;
    enlace.hidden = true;
    var aviso = enlace.parentElement.querySelector('.sin-foto');
    if (aviso) { aviso.hidden = false; }
  }
  document.querySelectorAll('img.foto-evidencia').forEach(function (img) {
    if (img.complete && img.naturalWidth === 0) { sinFoto(img); }
    img.addEventListener('error', function () { sinFoto(img); });
  });

  // v1.2 · al elegir «Descartado» el comentario pasa a ser obligatorio (motivo) y se muestra el aviso
  var nuevo = document.getElementById('nuevo'), comentario = document.getElementById('comentario');
  if (nuevo && comentario) {
    var aviso = document.getElementById('aviso-descarte'), opcional = document.getElementById('com-opcional');
    var revisar = function () {
      var descarta = nuevo.value === 'Descartado';
      comentario.required = descarta;
      comentario.minLength = descarta ? 10 : 0;
      comentario.placeholder = descarta ? 'Motivo: por ejemplo, la foto no muestra ningún problema en la vía pública.' : 'Ejemplo: Se programó la cuadrilla para el lunes.';
      if (aviso) { aviso.hidden = !descarta; }
      if (opcional) { opcional.textContent = descarta ? '(obligatorio: motivo del descarte)' : '(opcional)'; }
    };
    nuevo.addEventListener('change', revisar);
    revisar();
  }

  var caja = document.getElementById('mapa-caso');
  if (caja && window.L) {
    var lat = parseFloat(caja.dataset.lat), lng = parseFloat(caja.dataset.lng);
    if (!isNaN(lat) && !isNaN(lng)) {
      var mapa = L.map(caja, { scrollWheelZoom: false }).setView([lat, lng], 17);
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
      L.circleMarker([lat, lng], { radius: 9, color: '#ffffff', weight: 2, fillColor: '#b3261e', fillOpacity: 0.95 }).addTo(mapa);
    }
  }
})();
