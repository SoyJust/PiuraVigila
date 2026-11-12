/* v1.0 · aplica la intensidad (0 a 1) de data-i como variable CSS --i: barras del resumen. Sin estilos en línea (CSP). */
document.querySelectorAll('[data-i]').forEach(function (e) { e.style.setProperty('--i', e.dataset.i); });
