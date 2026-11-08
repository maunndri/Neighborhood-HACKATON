CREATE DATABASE IF NOT EXISTS barrio_conecta_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE barrio_conecta_db;

-- Spring Data JPA crea/actualiza las tablas al iniciar (ddl-auto=update).
-- Se crea automáticamente valoraciones_servicio para guardar las calificaciones.
-- Cada usuario puede tener una calificación por publicación; puede actualizarla.
-- Tablas nuevas (respuestas_ayuda, eventos_seguimiento, notificaciones, denuncias) y columnas nuevas
-- también las crea Hibernate al iniciar. Los datos de demostración los carga DemoDataLoader.
