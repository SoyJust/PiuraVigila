"""
PiuraVigila · genera las imágenes ilustrativas de los datos de demostración (opcional).

04_datos_demo.sql guarda en cada reporte una ruta como «fotos/demo-e01.jpg», pero esos archivos no existen: este script
crea una imagen simple por reporte, claramente marcada como «Foto de demostración», para que el detalle del caso en el
panel no aparezca vacío durante una demostración. No son fotos reales.

Requisitos: Python 3 con Pillow (pip install pillow).
Uso (desde la carpeta del proyecto):  python database/generar_fotos_demo.py [carpeta_de_fotos]
La carpeta por defecto es «fotos» (la misma de piuravigila.foto.directorio). Está en .gitignore.
"""
import os
import re
import sys
from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SQL = os.path.join(RAIZ, 'database', '04_datos_demo.sql')
DESTINO = sys.argv[1] if len(sys.argv) > 1 else os.path.join(RAIZ, 'fotos')

# prefijo de categoría usado en el SQL → (nombre, color de fondo)
CATEGORIAS = {
    'Bache': ('Bache en la vía', (92, 84, 76)), 'Grieta': ('Grieta o hundimiento', (110, 98, 86)),
    'Vereda': ('Vereda dañada', (132, 126, 118)), 'Luminaria': ('Luminaria dañada', (32, 44, 72)),
    'Sem': ('Semáforo o señalización', (60, 72, 60)), 'Fuga': ('Fuga de agua o desagüe', (40, 88, 118)),
    'Acumulaci': ('Acumulación de basura', (86, 96, 52)), '_rbol': ('Árbol caído o área verde', (46, 96, 58)),
    'Cables': ('Cables expuestos o poste', (70, 70, 84)), 'Otra': ('Otro problema', (96, 80, 104)),
}


def fuente(tam, negrita=False):
    for nombre in (('arialbd.ttf' if negrita else 'arial.ttf'), ('DejaVuSans-Bold.ttf' if negrita else 'DejaVuSans.ttf')):
        try:
            return ImageFont.truetype(nombre, tam)
        except OSError:
            continue
    return ImageFont.load_default()


def imagen(ruta, prefijo, descripcion):
    nombre, color = CATEGORIAS.get(prefijo, ('Reporte', (80, 80, 80)))
    w, h = 1200, 900
    img = Image.new('RGB', (w, h), color)
    d = ImageDraw.Draw(img)
    for y in range(h):  # degradado suave hacia abajo
        f = y / h
        d.line([(0, y), (w, y)], fill=tuple(int(c * (1 - 0.35 * f)) for c in color))
    for i in range(0, w + h, 60):  # textura de líneas
        d.line([(i, 0), (i - h, h)], fill=tuple(min(255, c + 12) for c in color), width=2)
    d.rounded_rectangle([60, 60, w - 60, h - 60], radius=28, outline=(242, 169, 0), width=6)
    d.text((w / 2, 300), nombre, font=fuente(64, True), fill=(255, 255, 255), anchor='mm')
    texto = (descripcion[:58].rstrip() + '…') if len(descripcion) > 58 else descripcion
    d.text((w / 2, 400), texto, font=fuente(30), fill=(230, 236, 240), anchor='mm')
    d.rounded_rectangle([w / 2 - 330, 560, w / 2 + 330, 650], radius=20, fill=(242, 169, 0))
    d.text((w / 2, 605), 'FOTO DE DEMOSTRACIÓN', font=fuente(44, True), fill=(29, 22, 0), anchor='mm')
    d.text((w / 2, 720), 'Imagen ilustrativa · datos ficticios de PiuraVigila', font=fuente(28), fill=(220, 226, 232), anchor='mm')
    img.save(ruta, 'JPEG', quality=80, optimize=True)


def main():
    sql = open(SQL, encoding='utf-8').read()
    os.makedirs(DESTINO, exist_ok=True)
    variables = {}  # cN / eNN := demo_caso(...) → categoría (los vinculados la heredan)
    caso = re.compile(r"(?:(\w+) := |PERFORM )pg_temp\.demo_caso\('([^']+)',\s*'[^']*',\s*'((?:[^']|'')*)'.*?'(fotos/[\w.-]+)'\);", re.S)
    vinc = re.compile(r"PERFORM pg_temp\.demo_vinculado\((\w+),\s*'((?:[^']|'')*)'.*?'(fotos/[\w.-]+)'\);", re.S)
    sentencias = sorted([(m.start(), 'c', m) for m in caso.finditer(sql)] + [(m.start(), 'v', m) for m in vinc.finditer(sql)])
    n = 0
    for _, tipo, m in sentencias:
        if tipo == 'c':
            var, pref, desc, foto = m.group(1), m.group(2), m.group(3).replace("''", "'"), m.group(4)
            if var:
                variables[var] = pref
        else:
            pref, desc, foto = variables.get(m.group(1), 'Otra'), m.group(2).replace("''", "'"), m.group(3)
        imagen(os.path.join(DESTINO, os.path.basename(foto)), pref, desc)
        n += 1
    print(f'{n} imágenes de demostración creadas en {DESTINO}')


if __name__ == '__main__':
    main()
