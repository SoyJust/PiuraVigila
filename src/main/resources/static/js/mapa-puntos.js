// Semana 14 · SEC-04: movido desde mapa.html para que la CSP no necesite 'unsafe-inline' en scripts.
(function () {
  // Intensidad de color de cada zona (antes un atributo style en la plantilla; CSSOM está permitido por la CSP).
  document.querySelectorAll('.mapa .zona[data-i]').forEach(function (z) { z.style.setProperty('--i', z.dataset.i); });

  var mapa = L.map('mapa-puntos').setView([-5.1945, -80.6328], 12);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
  var colores = { 'Recibido': '#2f5d8a', 'En proceso': '#c98a00', 'Atendido': '#1b7a47', 'Descartado': '#7a8794' };
  var limites = [];
  document.querySelectorAll('#datos-puntos li').forEach(function (li) {
    var lat = parseFloat(li.dataset.lat), lng = parseFloat(li.dataset.lng);
    if (isNaN(lat) || isNaN(lng)) { return; }
    var punto = L.circleMarker([lat, lng], { radius: 8, color: '#ffffff', weight: 2, fillColor: colores[li.dataset.estado] || '#2f5d8a', fillOpacity: 0.9 }).addTo(mapa);
    var caja = document.createElement('div');
    var titulo = document.createElement('strong');
    titulo.textContent = li.dataset.codigo || 'Reporte';
    caja.appendChild(titulo);
    caja.appendChild(document.createElement('br'));
    caja.appendChild(document.createTextNode(li.dataset.categoria + ' · ' + li.dataset.zona));
    caja.appendChild(document.createElement('br'));
    caja.appendChild(document.createTextNode('Estado: ' + li.dataset.estado));
    if (li.dataset.codigo) { // v1.1 · abre el detalle del caso (foto e historial)
      caja.appendChild(document.createElement('br'));
      var ver = document.createElement('a');
      ver.href = '/panel/caso?codigo=' + encodeURIComponent(li.dataset.codigo);
      ver.textContent = 'Ver caso';
      caja.appendChild(ver);
    }
    punto.bindPopup(caja);
    limites.push([lat, lng]);
  });
  if (limites.length === 1) { mapa.setView(limites[0], 16); }
  else if (limites.length > 1) { mapa.fitBounds(limites, { padding: [30, 30], maxZoom: 17 }); }
})();
