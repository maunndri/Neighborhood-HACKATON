# Neighborhood · Tu Barrio Conectado

<p align="center">
  <img src="src/main/resources/static/images/brand/neighborhood-logo.png" alt="Neighborhood Logo" width="420">
</p>

Plataforma web y PWA de **economía local de hiperproximidad**, **cadena de favores con Banco de Tiempo Vecinal (sin dinero)** y **red de respuesta comunitaria ante emergencias** desarrollada para el reto de **48 horas (Hackathon UTP)**.

---

## Pilares del Proyecto (Alineados al Reto de 48 Horas)

1. **Servicios de Proximidad (Economía Local sin Transporte Mayor):**
   - Directorio de oficios y productos del barrio (*gasfitería, electricidad, carpintería, cerrajería, paseo de mascotas, clases, alimentos y reparaciones*).
   - **Indicador de Hiperproximidad a Pie:** Calcula minutos caminando (`🚶 X min a pie · S/ 0 pasaje`) y filtro rápido `Solo a pie (≤ 15 min)` para eliminar gastos de transporte.
   - **Sistema de Visto Bueno Vecinal:** 3 niveles de confianza (*Recomendado por vecinos*, *Muy recomendado* e *Identidad verificada*), mostrando cuántas recomendaciones provienen de residentes de la misma zona.
   - Fotografías documentales realistas por oficio además de soporte para subida de fotos propias.

2. **Cadena de Favores & Banco de Tiempo Vecinal (Economía Solidaria sin Dinero):**
   - Muro colaborativo bidireccional (*Necesito ayuda* / *Quiero ayudar*) para **préstamo de herramientas, movilidad solidaria, intercambio de habilidades y donación de materiales**.
   - **Banco de Tiempo e Insignias Solidarias:** Cada ayuda completada otorga **+15 Créditos de Gratitud** y hace evolucionar la insignia del vecino (*Vecino Activo* → *Mano Amiga* → *Vecino Solidario* → *Guardián del Barrio*).
   - Flujo completo de coordinación: `Pendiente` → `En coordinación` → `Resuelta` con línea de tiempo auditable.

3. **Evolución Dinámica en Situaciones de Emergencia:**
   - **Conmutador de Modo Emergencia (1 clic):** Transforma la interfaz completa en un tablero de contingencia y ayuda humanitaria cuando ocurre un desastre o alerta SOS.
   - **Botón Flotante SOS Inteligente:** Permite reportar vulnerabilidad (adulto mayor, herido, falta de agua/luz, rescate) geolocalizando al vecino y notificando automáticamente a residentes en un radio de 5 km.
   - **Difusión en WhatsApp Vecinal (1 clic):** Enlace directo para compartir alertas SOS, favores o servicios recomendados en el grupo de WhatsApp de la cuadra o junta vecinal.
   - **Directorio de Recursos Críticos:** Brigadas, puntos de agua, generadores y botiquines comunitarios.

4. **Accesibilidad, Enfoque Móvil y Modo Offline (PWA):**
   - **Modo Adulto Mayor / Lectura Fácil:** Botón en la barra principal que amplía tipografías, contrastes y áreas táctiles para vecinos mayores.
   - **Máximo 3 clics para publicar:** Hoja inferior rápida con geolocalización aproximada (~100 m por privacidad).
   - **Tutorial Interactivo de 6 pasos (`¿Cómo funciona?`):** Disponible al iniciar o desde el menú lateral.
   - **Funcionalidad Online y Offline:** Service Worker (`sw.js`) + `manifest.webmanifest` que guarda páginas visitadas y encola publicaciones o alertas SOS sin internet para enviarlas al reconectar.

---

## Requisitos Técnicos
- **Java JDK 17+**
- **Maven 3.9+**
- **MySQL Server 8.0+**

---

## Instalación y Ejecución Rápida

1. **Crear la base de datos en MySQL:**
   Ejecuta el script `database/neighborhood_db.sql` (crea el esquema `neighborhood_db`; Hibernate genera y actualiza las tablas automáticamente con `ddl-auto=update`).

2. **Configurar credenciales MySQL:**
   Revisa `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/neighborhood_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima
   spring.datasource.username=root
   spring.datasource.password=root
   ```

3. **Ejecutar el servidor Spring Boot:**
   ```bash
   cd Neighborhood-SpringBoot
   mvn spring-boot:run
   ```

4. **Abrir en el navegador:**
   - Aplicación web: [http://localhost:8080](http://localhost:8080)

---

## Cuentas de Demostración Precargadas

| Rol | Correo | Contraseña |
|---|---|---|
| **Vecinos de prueba** | `rosa@demo.pe`, `carlos@demo.pe`, `lucia@demo.pe`, `jorge@demo.pe`, `marta@demo.pe` | `demo1234` |
| **Administrador (Moderación)** | `admin@neighborhood.pe` | `Admin12345` |
