# BarrioConecta – mejoras de servicios

## Cambios incluidos
- Tarjetas horizontales: fotografía compacta a la izquierda e información a la derecha.
- Calificación comunitaria de 1 a 5 estrellas, con promedio y número de opiniones.
- Cada usuario autenticado puede calificar una publicación una vez y actualizar su voto.
- No se permite que el dueño califique su propia publicación.
- Distancia aproximada calculada en el navegador con la ubicación actual y las coordenadas guardadas en cada publicación.
- Ordenamiento por cercanía. Las publicaciones sin coordenadas quedan al final.
- La ubicación actual del visitante solo se usa en el navegador y no se almacena.

## Cómo probar
1. Descomprime el ZIP.
2. Abre la carpeta `BarrioConecta-SpringBoot` en IntelliJ IDEA.
3. Configura MySQL en `src/main/resources/application.properties` (usuario y contraseña).
4. Ejecuta el script `database/barrio_conecta_db.sql` en MySQL Workbench si todavía no existe la base de datos.
5. Ejecuta `BarrioConectaApplication`.
6. Registra dos cuentas. Desde una cuenta publica un servicio y marca su ubicación con el mapa.
7. Desde la otra cuenta abre **Servicios y productos**, permite el acceso a la ubicación y califica el servicio.

Hibernate creará automáticamente la tabla `valoraciones_servicio` al iniciar porque el proyecto usa `spring.jpa.hibernate.ddl-auto=update`.

## Notas importantes
- Para calcular distancia, el navegador necesita permiso de ubicación. En localhost funciona; para otros equipos o teléfonos normalmente se requiere HTTPS.
- Las publicaciones antiguas que no tengan latitud y longitud mostrarán “Ubicación no registrada”. Edita o vuelve a publicar marcando la ubicación en el mapa para habilitar la distancia.
- Las calificaciones son comunitarias y quedan asociadas a la cuenta que las envía.
