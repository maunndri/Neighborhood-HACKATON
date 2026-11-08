# Barrio Conecta – mejoras para el reto de 48 horas

Sigue siendo una **web responsive** (Spring Boot + Thymeleaf + MySQL). Funciona como PWA: se puede "instalar" desde el navegador del teléfono y trabaja sin conexión para lo básico, así que no necesitas hacer una app todavía.

## Cómo probarlo (2 minutos)
1. Ejecuta `database/barrio_conecta_db.sql` en MySQL y revisa usuario/clave en `application.properties`.
2. Ejecuta `BarrioConectaApplication`. Hibernate agrega las columnas y tablas nuevas solo (`ddl-auto=update`).
3. Si la base está vacía se cargan **datos de demostración** (para mostrar todo conectado):
   - Vecinos: `rosa@demo.pe`, `carlos@demo.pe`, `lucia@demo.pe`, `jorge@demo.pe`, `marta@demo.pe` — clave `demo1234`
   - Administrador: `admin@barrioconecta.pe` — clave `Admin12345` (**cámbiala** en `application.properties`)
   - Para empezar sin datos: `barrioconecta.demo-data=false`
4. Abre `http://localhost:8080`. La geolocalización, el modo offline y la instalación requieren `localhost` o HTTPS.

## Guion de demostración sugerido
1. Entra como **Lucía**, abre un servicio (gasfitería de Rosa) → mira *Visto Bueno Vecinal*, quién lo recomendó, fechas y reputación → califica con estrellas.
2. **Cadena de favores**: pestañas *Necesito ayuda / Quiero ayudar*. Como Lucía pulsa **Puedo ayudar** en la escalera de Rosa.
3. Entra como **Rosa**: campana con aviso → acepta la ayuda → estado pasa a **En coordinación**, aparece el responsable y el contacto → **Marcar como resuelta** → Lucía suma una "ayuda completada".
4. **Emergencias**: contadores, números oficiales (105/116/106/115), solicitud con 3 vecinos coordinando y línea de **seguimiento**.
5. **Mapa** con capas, necesidades más cercanas y avisos.
6. Apaga el wifi: banner "Sin conexión", se ven las páginas ya visitadas y lo que publiques se envía al reconectar.
7. Entra como **admin**: *Moderación* → denuncias, ocultar, verificar.

## Qué se implementó (según tu tabla)
| Funcionalidad | Qué hay ahora |
|---|---|
| Visto Bueno Vecinal | 3 niveles: **Recomendado por vecinos** (≥1 recomendación de 4–5★), **Muy recomendado** (≥3 y promedio ≥4), **Identidad verificada** (solo admin). Se muestra quién recomendó (nombre + inicial), cuántas, cuántas de la misma zona, comentarios y fecha de la última. Aviso explícito: *una calificación no es una verificación profesional*. |
| Calificaciones | Se guardan en BD (una por vecino y servicio, editable), con cálculo de promedio, recomendaciones y **reputación del vecino** (opiniones + ayudas completadas) en su perfil. No puedes calificarte a ti mismo. |
| Intercambio solidario | **Se corrigió el bug**: el controlador siempre guardaba `solicito`. Ahora se guarda `solicito`/`ofrezco`. Dos secciones: *Necesito ayuda* y *Quiero ayudar*, con categoría, descripción, ubicación aproximada (se redondea a ~100 m), disponibilidad y estado Pendiente → En coordinación → Resuelta. |
| Coordinación | **Puedo ayudar / Lo necesito** → el dueño **acepta o rechaza** → responsable asignado → contactos (WhatsApp/llamada) solo tras aceptar → resolver. Línea de tiempo de seguimiento por solicitud. |
| Distancia | Km/m en tarjetas de servicios, favores, emergencias y recursos + botón *Más cerca*. La ubicación del visitante no se guarda en el servidor. |
| Emergencias | Botón SOS, "Necesito ayuda", contadores (pendientes/coordinando/atendidas), mapa con capas, recursos con insignia verificado, números oficiales y aviso de que la app no sustituye a las autoridades. SOS avisa por notificación a vecinos con ubicación registrada a <5 km. |
| Tutorial | «¿Cómo funciona?» de 6 pasos, se muestra la primera vez, se puede omitir y reabrir desde el menú, el perfil y el pie de página. |
| Modo offline | Service worker + manifest (PWA): páginas y mapa guardados, página `/offline`, y **cola de publicaciones/SOS** que se envía sola al reconectar. |
| Notificaciones | Campana con contador, página *Mis avisos*, aviso emergente y del navegador (si da permiso) cuando alguien responde, acepta, califica, hay SOS cerca o moderación. |
| Administración | `/admin`: denuncias de vecinos (botón *Reportar* en cada tarjeta), ocultar/mostrar publicaciones, verificar servicios y recursos, suspender cuentas. |
| Máx. 3 clics | Publicar → elegir tipo (hoja inferior en móvil) → enviar. La ubicación se toma del perfil si no marcas otra. |

## Archivos principales nuevos
- `model/`: `RespuestaAyuda`, `EventoSeguimiento`, `Notificacion`, `DenunciaContenido` (+ campos nuevos en `Favor`, `ReporteEmergencia`, `PublicacionServicio`, `Usuario`, `GrupoApoyo`).
- `service/`: `AyudaService` (coordinación), `ReputacionService` (Visto Bueno), `NotificacionService`, `SesionService`.
- `controller/`: `ServicioController`, `FavorController`, `EmergenciaController`, `AyudaController`, `NotificacionController`, `AdminController`, `DenunciaController` (el `WebController` quedó solo con cuentas, perfil, mapa, SOS y recursos → más fácil de escalar).
- `static/`: `sw.js`, `manifest.webmanifest`, `js/offline.js`, `js/tutorial.js`, `js/notificaciones.js`, `js/distancia.js` (reemplaza `service-distance.js`), `js/mapa.js` reescrito.

## Límites honestos (dilos en la presentación como "riesgos y siguientes pasos")
- Es un prototipo: falta HTTPS, CSRF, límites anti-abuso, política de privacidad y verificación real de identidad.
- Notificaciones: se reciben con la web abierta (consulta cada 20 s); las push reales con la app cerrada requerirían Web Push/FCM.
- Offline: se pueden ver las páginas ya visitadas y encolar publicaciones sin fotos; las fotos de servicios necesitan conexión.
- Los números 105/116/106/115 son de Perú: confirma los de tu localidad.
- No pude ejecutar Maven ni la base de datos en mi entorno: revisé la sintaxis Java y el HTML, pero **compila y prueba en tu equipo** y avísame si aparece algún error para corregirlo.
