package com.barrioconecta.service;

import com.barrioconecta.model.*;
import com.barrioconecta.repository.*;
import com.barrioconecta.util.Catalogos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coordinación de ayuda entre vecinos: "Puedo ayudar" / "Lo necesito" -> aceptar o rechazar -> resolver.
 * Sirve tanto para la cadena de favores como para las necesidades de emergencia.
 * Devuelve null cuando la operación sale bien o un mensaje de error para mostrar al vecino.
 */
@Service
public class AyudaService {
    public static final String FAVOR = "favor";
    public static final String EMERGENCIA = "emergencia";

    private final FavorRepository favores;
    private final EmergenciaRepository emergencias;
    private final RespuestaAyudaRepository respuestas;
    private final EventoSeguimientoRepository eventos;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;

    public AyudaService(FavorRepository favores, EmergenciaRepository emergencias, RespuestaAyudaRepository respuestas,
                        EventoSeguimientoRepository eventos, UsuarioRepository usuarios, NotificacionService notificaciones) {
        this.favores = favores; this.emergencias = emergencias; this.respuestas = respuestas;
        this.eventos = eventos; this.usuarios = usuarios; this.notificaciones = notificaciones;
    }

    public static String enlaceFavor(Favor f) {
        return "/favores?vista=" + (f.isOferta() ? "ofrezco" : "necesito") + "#favor-" + f.getId();
    }

    public static String enlaceEmergencia(ReporteEmergencia e) { return "/emergencias#emergencia-" + e.getId(); }

    public void registrar(String tipo, Long id, String texto) { eventos.save(new EventoSeguimiento(tipo, id, texto)); }

    // ---------- Carga de respuestas y seguimiento para las vistas ----------

    public void cargarFavores(List<Favor> lista) {
        if (lista.isEmpty()) return;
        List<Long> ids = lista.stream().map(Favor::getId).toList();
        Map<Long, List<RespuestaAyuda>> r = respuestas.findByObjetivoTipoAndObjetivoIdInOrderByCreadoEnAsc(FAVOR, ids).stream()
            .collect(Collectors.groupingBy(RespuestaAyuda::getObjetivoId));
        Map<Long, List<EventoSeguimiento>> ev = eventos.findByObjetivoTipoAndObjetivoIdInOrderByCreadoEnAsc(FAVOR, ids).stream()
            .collect(Collectors.groupingBy(EventoSeguimiento::getObjetivoId));
        for (Favor f : lista) {
            f.setRespuestas(r.getOrDefault(f.getId(), new ArrayList<>()));
            f.setEventos(ev.getOrDefault(f.getId(), new ArrayList<>()));
        }
    }

    public void cargarEmergencias(List<ReporteEmergencia> lista) {
        if (lista.isEmpty()) return;
        List<Long> ids = lista.stream().map(ReporteEmergencia::getId).toList();
        Map<Long, List<RespuestaAyuda>> r = respuestas.findByObjetivoTipoAndObjetivoIdInOrderByCreadoEnAsc(EMERGENCIA, ids).stream()
            .collect(Collectors.groupingBy(RespuestaAyuda::getObjetivoId));
        Map<Long, List<EventoSeguimiento>> ev = eventos.findByObjetivoTipoAndObjetivoIdInOrderByCreadoEnAsc(EMERGENCIA, ids).stream()
            .collect(Collectors.groupingBy(EventoSeguimiento::getObjetivoId));
        for (ReporteEmergencia e : lista) {
            e.setRespuestas(r.getOrDefault(e.getId(), new ArrayList<>()));
            e.setEventos(ev.getOrDefault(e.getId(), new ArrayList<>()));
        }
    }

    // ---------- Acciones ----------

    /** "Puedo ayudar" (necesidad) o "Lo necesito" (oferta). */
    @Transactional
    public String responder(Usuario u, String tipo, Long id, String mensaje) {
        Usuario dueno; String titulo; String enlace; boolean oferta = false;
        if (FAVOR.equals(tipo)) {
            Favor f = favores.findById(id).orElse(null);
            if (f == null || f.isOculto() || !f.isActivo()) return "Esta publicación ya no está disponible.";
            dueno = f.getUsuario(); titulo = f.getTitulo(); oferta = f.isOferta(); enlace = enlaceFavor(f);
        } else if (EMERGENCIA.equals(tipo)) {
            ReporteEmergencia e = emergencias.findById(id).orElse(null);
            if (e == null || e.isOculto() || Catalogos.ESTADO_RESUELTA.equals(e.getEstado())) return "Esta solicitud ya fue atendida o no está disponible.";
            dueno = e.getUsuario(); titulo = e.getTitulo(); enlace = enlaceEmergencia(e);
        } else {
            return "Tipo de publicación no válido.";
        }
        if (dueno.getId().equals(u.getId())) return "No puedes responder a tu propia publicación.";
        if (respuestas.findByObjetivoTipoAndObjetivoIdAndUsuarioId(tipo, id, u.getId()).isPresent())
            return "Ya enviaste tu respuesta. Espera a que el vecino la revise.";
        RespuestaAyuda r = new RespuestaAyuda();
        r.setObjetivoTipo(tipo); r.setObjetivoId(id); r.setUsuario(u);
        r.setMensaje(Catalogos.limpiar(mensaje, 300));
        respuestas.save(r);
        registrar(tipo, id, u.getNombreCorto() + (oferta ? " pidió este recurso" : " se ofreció a ayudar"));
        notificaciones.notificar(dueno, u.getNombreCorto() + (oferta ? " quiere usar lo que ofreces: " : " puede ayudarte con: ") + titulo, enlace);
        return null;
    }

    /** El dueño acepta o rechaza una respuesta. Al aceptar, la publicación pasa a "En coordinación". */
    @Transactional
    public String decidir(Usuario dueno, Long respuestaId, boolean aceptar) {
        RespuestaAyuda r = respuestas.findById(respuestaId).orElse(null);
        if (r == null) return "La respuesta ya no existe.";
        Favor f = null; ReporteEmergencia e = null; Usuario propietario; String titulo; String enlace;
        if (FAVOR.equals(r.getObjetivoTipo())) {
            f = favores.findById(r.getObjetivoId()).orElse(null);
            if (f == null) return "La publicación ya no existe.";
            propietario = f.getUsuario(); titulo = f.getTitulo(); enlace = enlaceFavor(f);
        } else {
            e = emergencias.findById(r.getObjetivoId()).orElse(null);
            if (e == null) return "La publicación ya no existe.";
            propietario = e.getUsuario(); titulo = e.getTitulo(); enlace = enlaceEmergencia(e);
        }
        if (!propietario.getId().equals(dueno.getId())) return "No puedes gestionar esta respuesta.";
        if (!r.isOfrecida()) return "Esta respuesta ya fue gestionada.";
        String estadoActual = f != null ? f.getEstado() : e.getEstado();
        if (Catalogos.ESTADO_RESUELTA.equals(estadoActual)) return "La solicitud ya está resuelta.";

        if (aceptar) {
            r.setEstado(RespuestaAyuda.ACEPTADA);
            LocalDateTime ahora = LocalDateTime.now();
            if (f != null) {
                f.setEstado(Catalogos.ESTADO_COORDINANDO);
                if (f.getResponsable() == null) f.setResponsable(r.getUsuario());
                f.setActualizadoEn(ahora); favores.save(f);
            } else {
                e.setEstado(Catalogos.ESTADO_COORDINANDO);
                if (e.getResponsable() == null) e.setResponsable(r.getUsuario());
                e.setActualizadoEn(ahora); emergencias.save(e);
            }
            registrar(r.getObjetivoTipo(), r.getObjetivoId(), dueno.getNombreCorto() + " aceptó la ayuda de " + r.getUsuario().getNombreCorto());
            notificaciones.notificar(r.getUsuario(), dueno.getNombreCorto() + " aceptó tu ofrecimiento en «" + titulo + "». Ya pueden coordinar por WhatsApp o llamada.", enlace);
        } else {
            r.setEstado(RespuestaAyuda.RECHAZADA);
            registrar(r.getObjetivoTipo(), r.getObjetivoId(), dueno.getNombreCorto() + " no pudo aceptar la ayuda de " + r.getUsuario().getNombreCorto());
            notificaciones.notificar(r.getUsuario(), dueno.getNombreCorto() + " no pudo aceptar tu ofrecimiento en «" + titulo + "». ¡Gracias por tu disposición!", enlace);
        }
        respuestas.save(r);
        return null;
    }

    /** El dueño confirma que ya recibió la ayuda. Suma una ayuda completada a quienes fueron aceptados. */
    @Transactional
    public String resolver(Usuario dueno, String tipo, Long id) {
        Usuario propietario; String titulo; String enlace;
        if (FAVOR.equals(tipo)) {
            Favor f = favores.findById(id).orElse(null);
            if (f == null) return "La publicación ya no existe.";
            propietario = f.getUsuario(); titulo = f.getTitulo(); enlace = enlaceFavor(f);
            if (!propietario.getId().equals(dueno.getId())) return "No puedes modificar esta publicación.";
            f.setEstado(Catalogos.ESTADO_RESUELTA); f.setActivo(false); f.setActualizadoEn(LocalDateTime.now()); favores.save(f);
        } else if (EMERGENCIA.equals(tipo)) {
            ReporteEmergencia e = emergencias.findById(id).orElse(null);
            if (e == null) return "La publicación ya no existe.";
            propietario = e.getUsuario(); titulo = e.getTitulo(); enlace = enlaceEmergencia(e);
            if (!propietario.getId().equals(dueno.getId())) return "No puedes modificar esta solicitud.";
            e.setEstado(Catalogos.ESTADO_RESUELTA); e.setActualizadoEn(LocalDateTime.now()); emergencias.save(e);
        } else {
            return "Tipo de publicación no válido.";
        }
        for (RespuestaAyuda r : respuestas.findByObjetivoTipoAndObjetivoId(tipo, id)) {
            Usuario ayudante = r.getUsuario();
            if (r.isAceptada()) {
                ayudante.setAyudasCompletadas(ayudante.getAyudasCompletadas() + 1);
                usuarios.save(ayudante);
                notificaciones.notificar(ayudante, "¡Gracias! " + dueno.getNombreCorto() + " confirmó que «" + titulo + "» quedó resuelto. Sumaste una ayuda completada.", enlace);
            } else if (r.isOfrecida()) {
                notificaciones.notificar(ayudante, "«" + titulo + "» ya fue resuelto. ¡Gracias por tu disposición!", enlace);
            }
        }
        registrar(tipo, id, "Marcado como resuelto por " + dueno.getNombreCorto());
        return null;
    }
}
