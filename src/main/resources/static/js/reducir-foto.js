/*
 * v1.1 · reduce la foto en el celular antes de enviarla: lado mayor de 1600 px, JPEG al 82 %.
 * Ahorra datos móviles al vecino y espacio al servidor, y evita el error «Foto demasiado pesada» con fotos de 3 a 8 MB.
 * Al dibujarla en un canvas se descartan los metadatos (la ubicación GPS de la casa) y el navegador la endereza.
 * Si el navegador no lo permite, se envía la foto original y el servidor la valida y reduce igual.
 */
(function () {
  // v1.1 · doble clic o «Enter» repetido: el formulario se envía una sola vez (el servidor igual lo controla).
  var formulario = document.querySelector('form[enctype="multipart/form-data"]');
  if (formulario) {
    formulario.addEventListener('submit', function (e) {
      if (formulario.dataset.enviando) { e.preventDefault(); return; }
      formulario.dataset.enviando = '1';
      var boton = formulario.querySelector('button[type=submit]');
      if (boton) { boton.dataset.texto = boton.textContent; boton.disabled = true; boton.textContent = 'Enviando…'; }
    });
    // Si el vecino vuelve con «Atrás», el navegador puede restaurar la página con el botón aún desactivado.
    window.addEventListener('pageshow', function () {
      delete formulario.dataset.enviando;
      var boton = formulario.querySelector('button[type=submit]');
      if (boton && boton.dataset.texto) { boton.disabled = false; boton.textContent = boton.dataset.texto; }
    });
  }
})();
(function () {
  var input = document.getElementById('foto');
  var vista = document.getElementById('vista-foto'), img = document.getElementById('img-vista'), info = document.getElementById('info-foto');
  if (!input || !window.DataTransfer || !window.createImageBitmap || !HTMLCanvasElement.prototype.toBlob) { return; }
  var LADO = 1600, CALIDAD = 0.82, procesando = false, urlVista = null;

  function mb(bytes) { return (bytes / 1048576).toFixed(bytes < 1048576 ? 2 : 1) + ' MB'; }
  function mostrar(archivo, original) {
    if (urlVista) { URL.revokeObjectURL(urlVista); }
    urlVista = URL.createObjectURL(archivo);
    img.src = urlVista;
    info.textContent = original && original.size !== archivo.size
      ? 'Foto lista: ' + mb(archivo.size) + ' (antes ' + mb(original.size) + ')'
      : 'Foto lista: ' + mb(archivo.size);
    vista.hidden = false;
  }

  input.addEventListener('change', function () {
    if (procesando) { return; }
    var original = input.files && input.files[0];
    if (!original || !/^image\/(jpeg|png)$/.test(original.type)) { vista.hidden = true; return; }
    info.textContent = 'Preparando la foto…'; vista.hidden = false;
    createImageBitmap(original, { imageOrientation: 'from-image' }).then(function (bmp) {
      var escala = Math.min(1, LADO / Math.max(bmp.width, bmp.height));
      if (escala === 1 && original.size <= 600 * 1024) { mostrar(original); return null; } // ya es liviana: se envía tal cual
      var canvas = document.createElement('canvas');
      canvas.width = Math.round(bmp.width * escala); canvas.height = Math.round(bmp.height * escala);
      var ctx = canvas.getContext('2d');
      ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(bmp, 0, 0, canvas.width, canvas.height);
      return new Promise(function (ok) { canvas.toBlob(ok, 'image/jpeg', CALIDAD); });
    }).then(function (blob) {
      if (!blob) { return; }
      var nombre = (original.name || 'foto').replace(/\.[^.]*$/, '') + '.jpg';
      var reducida = new File([blob], nombre, { type: 'image/jpeg' });
      var dt = new DataTransfer(); dt.items.add(reducida);
      procesando = true; input.files = dt.files; procesando = false;
      mostrar(reducida, original);
    }).catch(function () { mostrar(original); }); // si falla, va la original y el servidor decide
  });
})();
