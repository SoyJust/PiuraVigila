# Plantilla del entorno de despliegue. Copiar a <Base>\config\env.ps1 y completar los valores.
# Ese archivo contiene secretos: queda fuera del repositorio y no se comparte.

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
$env:MAVEN_HOME = "C:\ruta\apache-maven-3.10.0"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

# Base de datos del despliegue (distinta de la de desarrollo y de la de pruebas).
$env:PIURA_DB_URL = "jdbc:postgresql://localhost:5432/piuravigila_prod"
$env:PIURA_DB_USER = "piuravigila_app"
$env:PIURA_DB_PASSWORD = "<clave del rol piuravigila_app>"

# Contraseñas iniciales del personal (solo se usan si el hash sigue pendiente en la BD).
$env:PIURA_ADMIN_PASSWORD = "<clave inicial de admin>"
$env:PIURA_OPERADOR_PASSWORD = "<clave inicial de operador>"

# HTTPS: almacén PKCS12 con alias "piuravigila". El prefijo file: es obligatorio en Windows.
$env:PIURA_KEYSTORE = "file:C:/PiuraVigila-deploy/certs/piuravigila.p12"
$env:PIURA_KEYSTORE_PASSWORD = "<clave del almacén>"
$env:PIURA_HTTPS_PUERTO = "8443"
$env:PIURA_HTTP_PUERTO = "8080"

# Carpetas de datos del despliegue.
$env:PIURA_FOTOS_DIR = "C:\PiuraVigila-deploy\fotos"
$env:PIURA_LOG_DIR = "C:\PiuraVigila-deploy\logs"
