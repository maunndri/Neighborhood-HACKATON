package com.barrioconecta.service;

import com.barrioconecta.model.PublicacionServicio;
import com.barrioconecta.model.Usuario;
import com.barrioconecta.model.ValoracionServicio;
import com.barrioconecta.repository.ServicioRepository;
import com.barrioconecta.repository.ValoracionServicioRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Visto Bueno Vecinal: calcula, a partir de las calificaciones guardadas, los niveles de confianza
 * y la reputación. Una calificación de 4 o 5 estrellas cuenta como recomendación.
 * Importante: esto mide la opinión de los vecinos; NO verifica títulos ni certificaciones profesionales.
 */
@Service
public class ReputacionService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ValoracionServicioRepository valoraciones;
    private final ServicioRepository servicios;

    public ReputacionService(ValoracionServicioRepository valoraciones, ServicioRepository servicios) {
        this.valoraciones = valoraciones; this.servicios = servicios;
    }

    /** Completa los campos calculados de cada publicación con una sola consulta de calificaciones. */
    public void completar(List<PublicacionServicio> lista, Usuario viewer) {
        if (lista == null || lista.isEmpty()) return;
        List<Long> ids = lista.stream().map(PublicacionServicio::getId).toList();
        Map<Long, List<ValoracionServicio>> porServicio = valoraciones.findByServicioIdIn(ids).stream()
            .collect(Collectors.groupingBy(v -> v.getServicio().getId()));
        for (PublicacionServicio s : lista) {
            List<ValoracionServicio> votos = porServicio.getOrDefault(s.getId(), Collections.emptyList());
            double promedio = votos.stream().mapToInt(ValoracionServicio::getPuntuacion).average().orElse(0);
            List<ValoracionServicio> positivos = votos.stream().filter(v -> v.getPuntuacion() >= 4).toList();
            s.setPromedioCalificacion(Math.round(promedio * 10.0) / 10.0);
            s.setTotalCalificaciones(votos.size());
            s.setTotalRecomendaciones(positivos.size());
            String zonaDueno = s.getUsuario().getZona();
            s.setRecomendacionesZona((int) positivos.stream().filter(v -> mismaZona(zonaDueno, v.getUsuario().getZona())).count());
            positivos.stream().map(ValoracionServicio::getCreadoEn).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).ifPresent(d -> s.setUltimaRecomendacion(d.format(FECHA)));
            s.setNivelVecinal(positivos.size() >= 3 && promedio >= 4.0 ? "MUY" : positivos.isEmpty() ? "NINGUNO" : "RECOMENDADO");
            if (viewer != null) {
                votos.stream().filter(v -> v.getUsuario().getId().equals(viewer.getId())).findFirst()
                    .ifPresent(v -> s.setMiCalificacion(v.getPuntuacion()));
            }
        }
    }

    public List<OpinionVecinal> opiniones(PublicacionServicio s) {
        List<OpinionVecinal> out = new ArrayList<>();
        for (ValoracionServicio v : valoraciones.findByServicioIdOrderByCreadoEnDesc(s.getId())) {
            out.add(new OpinionVecinal(v.getUsuario().getNombreCorto(), v.getUsuario().getZona(), v.getPuntuacion(),
                v.getComentario(), v.getCreadoEn() == null ? "" : v.getCreadoEn().format(FECHA),
                mismaZona(s.getUsuario().getZona(), v.getUsuario().getZona())));
        }
        return out;
    }

    public ReputacionUsuario deUsuario(Usuario u) {
        List<ValoracionServicio> votos = valoraciones.findByServicioUsuarioId(u.getId());
        double promedio = votos.stream().mapToInt(ValoracionServicio::getPuntuacion).average().orElse(0);
        int recomendaciones = (int) votos.stream().filter(v -> v.getPuntuacion() >= 4).count();
        return new ReputacionUsuario(Math.round(promedio * 10.0) / 10.0, votos.size(), recomendaciones,
            (int) servicios.countByUsuarioIdAndOcultoFalse(u.getId()), u.getAyudasCompletadas());
    }

    static boolean mismaZona(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return false;
        return primerTramo(a).equals(primerTramo(b));
    }

    private static String primerTramo(String zona) {
        String t = zona.trim().toLowerCase();
        int coma = t.indexOf(',');
        return (coma > 0 ? t.substring(0, coma) : t).trim();
    }

    /** Opinión mostrada públicamente con nombre abreviado para proteger la privacidad. */
    public static class OpinionVecinal {
        private final String autor, zona, comentario, fecha;
        private final int puntuacion;
        private final boolean mismaZona;
        public OpinionVecinal(String autor, String zona, int puntuacion, String comentario, String fecha, boolean mismaZona) {
            this.autor = autor; this.zona = zona; this.puntuacion = puntuacion; this.comentario = comentario;
            this.fecha = fecha; this.mismaZona = mismaZona;
        }
        public String getAutor() { return autor; }
        public String getZona() { return zona; }
        public int getPuntuacion() { return puntuacion; }
        public String getComentario() { return comentario; }
        public String getFecha() { return fecha; }
        public boolean isMismaZona() { return mismaZona; }
    }

    public static class ReputacionUsuario {
        private final double promedio;
        private final int opiniones, recomendaciones, publicaciones, ayudasCompletadas;
        public ReputacionUsuario(double promedio, int opiniones, int recomendaciones, int publicaciones, int ayudasCompletadas) {
            this.promedio = promedio; this.opiniones = opiniones; this.recomendaciones = recomendaciones;
            this.publicaciones = publicaciones; this.ayudasCompletadas = ayudasCompletadas;
        }
        public double getPromedio() { return promedio; }
        public int getOpiniones() { return opiniones; }
        public int getRecomendaciones() { return recomendaciones; }
        public int getPublicaciones() { return publicaciones; }
        public int getAyudasCompletadas() { return ayudasCompletadas; }
    }
}
