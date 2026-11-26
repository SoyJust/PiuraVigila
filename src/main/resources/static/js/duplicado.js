/* Pantalla «¿Es el mismo problema?»: punto del caso existente, punto del vecino y distancia. Sin scripts en línea (CSP). */
(function () {
  var caja = document.getElementById('mapa-duplicado');
  if (!caja || !window.L) { return; }
  var lat = parseFloat(caja.dataset.lat), lng = parseFloat(caja.dataset.lng);
  if (isNaN(lat) || isNaN(lng)) { return; }
  var miLat = parseFloat(caja.dataset.miLat), miLng = parseFloat(caja.dataset.miLng);
  var tengoPunto = !isNaN(miLat) && !isNaN(miLng);

  var mapa = L.map(caja, { scrollWheelZoom: false }).setView([lat, lng], 17);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
  L.circleMarker([lat, lng], { radius: 9, color: '#ffffff', weight: 2, fillColor: '#b3261e', fillOpacity: 0.95 })
    .addTo(mapa).bindTooltip('Caso existente');
  if (!tengoPunto) { return; }

  L.circleMarker([miLat, miLng], { radius: 9, color: '#ffffff', weight: 2, fillColor: '#2f5d8a', fillOpacity: 0.95 })
    .addTo(mapa).bindTooltip('Tu punto');
  mapa.fitBounds([[lat, lng], [miLat, miLng]], { padding: [40, 40], maxZoom: 17 });

  // Distancia en línea recta (fórmula del haversine), redondeada para leerla rápido
  var rad = Math.PI / 180, dLat = (miLat - lat) * rad, dLng = (miLng - lng) * rad;
  var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat * rad) * Math.cos(miLat * rad) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  var metros = 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  var texto = metros < 1000 ? 'unos ' + (Math.round(metros / 10) * 10) + ' m' : 'unos ' + (metros / 1000).toFixed(1).replace('.', ',') + ' km';
  var nota = document.getElementById('distancia');
  nota.textContent = 'Tu punto está a ' + texto + ' del caso existente.' +
    (metros > 300 ? ' Parece estar lejos: probablemente sea otro problema.' : ' Está cerca: puede ser el mismo problema.');
  nota.hidden = false;
})();
