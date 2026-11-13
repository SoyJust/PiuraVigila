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
