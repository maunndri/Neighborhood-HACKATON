package com.barrioconecta.controller;

import com.barrioconecta.model.*;
import com.barrioconecta.repository.*;
import com.barrioconecta.service.*;
import com.barrioconecta.util.Catalogos;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/** Inicio, cuentas, perfil, mapa, SOS y directorio de recursos/grupos de apoyo. */
@Controller
public class WebController {
    private final ServicioRepository servicios;
    private final FavorRepository favores;
    private final EmergenciaRepository emergencias;
    private final GrupoApoyoRepository gruposApoyo;
    private final UsuarioRepository usuarios;
    private final RespuestaAyudaRepository respuestas;
    private final UsuarioService usuarioService;
    private final SesionService sesion;
    private final ReputacionService reputacion;
    private final AyudaService ayuda;
    private final NotificacionService notificaciones;

    public WebController(ServicioRepository servicios, FavorRepository favores, EmergenciaRepository emergencias,
                         GrupoApoyoRepository gruposApoyo, UsuarioRepository usuarios, RespuestaAyudaRepository respuestas,
                         UsuarioService usuarioService, SesionService sesion, ReputacionService reputacion,
                         AyudaService ayuda, NotificacionService notificaciones) {
        this.servicios = servicios; this.favores = favores; this.emergencias = emergencias; this.gruposApoyo = gruposApoyo;
        this.usuarios = usuarios; this.respuestas = respuestas; this.usuarioService = usuarioService; this.sesion = sesion;
        this.reputacion = reputacion; this.ayuda = ayuda; this.notificaciones = notificaciones;
    }

    // ---------- Inicio ----------

    @GetMapping("/")
    public String inicio(@RequestParam(required = false) String modo, Model model, HttpSession session) {
        if ("emergencia".equalsIgnoreCase(modo)) {
            session.setAttribute("modoEmergencia", true);
        } else if ("normal".equalsIgnoreCase(modo)) {
            session.setAttribute("modoEmergencia", false);
        }

        Usuario u = sesion.actual(session);
        List<PublicacionServicio> todos = servicios.findByOcultoFalseOrderByCreadoEnDesc();
        reputacion.completar(todos, u);
        // Primero los mejor recomendados por los vecinos; a igualdad, los más recientes.
        List<PublicacionServicio> destacados = todos.stream()
            .sorted(Comparator.comparingInt((PublicacionServicio s) -> s.getTotalRecomendaciones()).reversed()
                .thenComparing(Comparator.comparingDouble((PublicacionServicio s) -> s.getPromedioCalificacion()).reversed()))
            .limit(3).toList();
        List<ReporteEmergencia> activas = emergencias.findByOcultoFalseAndEstadoNotOrderByCreadoEnDesc(Catalogos.ESTADO_RESUELTA);
        ayuda.cargarEmergencias(activas);

        boolean alertaSos = activas.stream().anyMatch(ReporteEmergencia::isSos);
        Boolean modoEmergencia = (Boolean) session.getAttribute("modoEmergencia");
        if (modoEmergencia == null) {
            modoEmergencia = alertaSos;
        }

        model.addAttribute("modoEmergencia", modoEmergencia);
        model.addAttribute("alertaSos", alertaSos);
        model.addAttribute("emergenciasActivas", activas.stream().limit(6).toList());
        model.addAttribute("recursosCriticos", gruposApoyo.findByActivoTrueAndOcultoFalseOrderByVerificadoDescCreadoEnDesc().stream().limit(4).toList());
        model.addAttribute("servicios", destacados);
        model.addAttribute("totalServicios", todos.size());
        model.addAttribute("totalFavores", favores.findByOcultoFalseAndActivoTrueOrderByCreadoEnDesc().size());
        model.addAttribute("totalAlertas", activas.size());
        model.addAttribute("totalResueltos", favores.countByActivoFalseAndOcultoFalse()
            + emergencias.findByOcultoFalseAndEstadoOrderByActualizadoEnDesc(Catalogos.ESTADO_RESUELTA).size());
        return "index";
    }

    @GetMapping("/modo/toggle")
    public String toggleModo(@RequestParam(required = false, defaultValue = "/") String redirect, HttpSession session) {
        Boolean modo = (Boolean) session.getAttribute("modoEmergencia");
        session.setAttribute("modoEmergencia", modo == null ? Boolean.TRUE : !modo);
        return "redirect:" + (redirect.startsWith("/") ? redirect : "/");
    }

    @GetMapping("/offline")
    public String offline() { return "offline"; }

    // ---------- Cuentas ----------

    @GetMapping("/registro")
    public String registro() { return "registro"; }

    @PostMapping("/registro")
    public String registrar(@RequestParam String nombre, @RequestParam String correo,
                            @RequestParam String password, @RequestParam(required=false) String zona,
                            @RequestParam(required=false) String telefono,
                            @RequestParam(required=false) Double latitud,
                            @RequestParam(required=false) Double longitud,
                            HttpSession session, RedirectAttributes ra) {
        try {
            if (password.length() < 6) throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
            Usuario u = usuarioService.registrar(nombre, correo, password, zona);
            u.setTelefono(telefono); u.setLatitud(latitud); u.setLongitud(longitud); usuarios.save(u);
            sesion.iniciar(session, u);
            ra.addFlashAttribute("mensaje", "¡Bienvenido a Neighborhood! Toca «¿Cómo funciona?» para ver la guía rápida.");
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage()); return "redirect:/registro";
        }
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @PostMapping("/login")
    public String iniciarSesion(@RequestParam String correo, @RequestParam String password,
                                HttpSession session, RedirectAttributes ra) {
        Usuario u = usuarioService.autenticar(correo, password);
        if (u == null) { ra.addFlashAttribute("error", "Correo o contraseña incorrectos, o la cuenta está suspendida."); return "redirect:/login"; }
        sesion.iniciar(session, u);
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String salir(HttpSession session) { session.invalidate(); return "redirect:/"; }

    // ---------- Perfil ----------

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        List<PublicacionServicio> misServicios = servicios.findByUsuarioIdOrderByCreadoEnDesc(u.getId());
        reputacion.completar(misServicios, u);
        List<Favor> misFavores = favores.findByUsuarioIdOrderByCreadoEnDesc(u.getId());
        ayuda.cargarFavores(misFavores);
        List<RespuestaAyuda> misRespuestas = respuestas.findByUsuarioIdOrderByCreadoEnDesc(u.getId());
        model.addAttribute("usuario", u);
        model.addAttribute("reputacion", reputacion.deUsuario(u));
        model.addAttribute("misServicios", misServicios);
        model.addAttribute("misFavores", misFavores);
        model.addAttribute("misRespuestas", misRespuestas);
        return "perfil";
    }

    @PostMapping("/perfil/ubicacion")
    public String actualizarUbicacion(@RequestParam Double latitud, @RequestParam Double longitud, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            ra.addFlashAttribute("error", "Las coordenadas no son válidas."); return "redirect:/perfil";
        }
        u.setLatitud(latitud); u.setLongitud(longitud); usuarios.save(u);
        ra.addFlashAttribute("mensaje", "Tu ubicación fue actualizada."); return "redirect:/perfil";
    }

    @PostMapping("/perfil/contacto")
    public String actualizarContacto(@RequestParam(required=false) String telefono, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        u.setTelefono(telefono == null ? null : telefono.trim()); usuarios.save(u);
        ra.addFlashAttribute("mensaje", "Tu número de contacto fue actualizado."); return "redirect:/perfil";
    }

    // ---------- Recursos y grupos de apoyo ----------

    @GetMapping("/emergencias/grupos")
    public String gruposApoyo(@RequestParam(required=false) String categoria, Model model, HttpSession session) {
        Usuario u = sesion.actual(session);
        model.addAttribute("grupos", categoria == null || categoria.isBlank()
            ? gruposApoyo.findByActivoTrueAndOcultoFalseOrderByVerificadoDescCreadoEnDesc()
            : gruposApoyo.findByActivoTrueAndOcultoFalseAndCategoriaIgnoreCaseOrderByVerificadoDescCreadoEnDesc(categoria));
        model.addAttribute("categoriaSeleccionada", categoria);
        model.addAttribute("usuarioIdActual", u == null ? null : u.getId());
        return "grupos-apoyo";
    }

    @GetMapping("/emergencias/grupos/nuevo")
    public String nuevoGrupoApoyo(HttpSession session) {
        return sesion.actual(session) == null ? "redirect:/login" : "publicar-grupo-apoyo";
    }

    @PostMapping("/emergencias/grupos/nuevo")
    public String guardarGrupoApoyo(@RequestParam String nombre, @RequestParam String tipo,
            @RequestParam String categoria, @RequestParam String descripcion,
            @RequestParam(required=false) String zonaCobertura, @RequestParam(required=false) String telefono,
            @RequestParam(required=false) String disponibilidad, @RequestParam(required=false) Double latitud,
            @RequestParam(required=false) Double longitud, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        if ((latitud == null) != (longitud == null)) {
            ra.addFlashAttribute("error", "Completa ambas coordenadas o déjalas vacías.");
            return "redirect:/emergencias/grupos/nuevo";
        }
        GrupoApoyo g = new GrupoApoyo(); g.setUsuario(u); g.setNombre(nombre.trim());
        g.setTipo(tipo); g.setCategoria(categoria); g.setDescripcion(descripcion.trim());
        g.setZonaCobertura(zonaCobertura); g.setTelefono(telefono);
        g.setDisponibilidad(disponibilidad); g.setLatitud(latitud); g.setLongitud(longitud);
        gruposApoyo.save(g);
        notificaciones.notificarAdmins("Nuevo recurso de apoyo por revisar: " + g.getNombre(), "/admin");
        ra.addFlashAttribute("mensaje", "El recurso de apoyo se publicó. Aparecerá como no verificado hasta que un administrador lo revise.");
        return "redirect:/emergencias/grupos";
    }

    @GetMapping("/emergencias/grupos/{id}/editar")
    public String editarGrupoApoyoForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        GrupoApoyo g = gruposApoyo.findById(id).orElse(null);
        if (g == null || !g.getUsuario().getId().equals(u.getId())) {
            ra.addFlashAttribute("error", "No puedes editar este recurso."); return "redirect:/emergencias/grupos";
        }
        model.addAttribute("grupo", g); return "editar-grupo-apoyo";
    }

    @PostMapping("/emergencias/grupos/{id}/editar")
    public String actualizarGrupoApoyo(@PathVariable Long id, @RequestParam String nombre,
            @RequestParam String tipo, @RequestParam String categoria, @RequestParam String descripcion,
            @RequestParam(required=false) String zonaCobertura, @RequestParam(required=false) String telefono,
            @RequestParam(required=false) String disponibilidad, @RequestParam(required=false) Double latitud,
            @RequestParam(required=false) Double longitud, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        GrupoApoyo g = gruposApoyo.findById(id).orElse(null);
        if (g == null || !g.getUsuario().getId().equals(u.getId())) {
            ra.addFlashAttribute("error", "No puedes editar este recurso."); return "redirect:/emergencias/grupos";
        }
        if ((latitud == null) != (longitud == null)) { ra.addFlashAttribute("error", "Selecciona una ubicación válida."); return "redirect:/emergencias/grupos/" + id + "/editar"; }
        g.setNombre(nombre.trim()); g.setTipo(tipo); g.setCategoria(categoria); g.setDescripcion(descripcion.trim());
        g.setZonaCobertura(zonaCobertura); g.setTelefono(telefono); g.setDisponibilidad(disponibilidad);
        g.setLatitud(latitud); g.setLongitud(longitud);
        g.setVerificado(false); // si cambia el contenido, un administrador debe volver a revisarlo
        gruposApoyo.save(g);
        ra.addFlashAttribute("mensaje", "Tu recurso vecinal fue actualizado."); return "redirect:/emergencias/grupos";
    }

    @PostMapping("/emergencias/grupos/{id}/eliminar")
    public String eliminarGrupoApoyo(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        GrupoApoyo g = gruposApoyo.findById(id).orElse(null);
        if (g == null || !(g.getUsuario().getId().equals(u.getId()) || u.isAdmin())) ra.addFlashAttribute("error", "No puedes eliminar este recurso.");
        else { gruposApoyo.delete(g); ra.addFlashAttribute("mensaje", "El recurso vecinal fue eliminado."); }
        return "redirect:/emergencias/grupos";
    }

    // ---------- SOS ----------

    @PostMapping("/api/sos")
    @ResponseBody
    public Map<String, Object> enviarSos(
            @RequestParam Double latitud, 
            @RequestParam Double longitud,
            @RequestParam(required = false, defaultValue = "SOS") String categoria,
            @RequestParam(required = false) String detalle,
            HttpSession session) {
        Usuario u = sesion.actual(session);
        if (u == null) return Map.of("ok", false, "mensaje", "Inicia sesión para enviar una alerta SOS.");
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180)
            return Map.of("ok", false, "mensaje", "La ubicación no es válida.");
        ReporteEmergencia e = new ReporteEmergencia(); 
        e.setUsuario(u);
        String cat = (categoria != null && !categoria.isBlank()) ? categoria.trim() : "SOS";
        e.setCategoria(cat);
        e.setTitulo("ALERTA SOS: " + cat);
        e.setDescripcion(detalle != null && !detalle.isBlank() ? detalle.trim() : "Solicitud urgente de ayuda. Vecinos coordinando asistencia inmediata.");
        e.setLatitud(latitud); 
        e.setLongitud(longitud); 
        e.setEstado(Catalogos.ESTADO_PENDIENTE); 
        emergencias.save(e);
        session.setAttribute("modoEmergencia", true);
        ayuda.registrar(AyudaService.EMERGENCIA, e.getId(), "Alerta SOS (" + cat + ") enviada por " + u.getNombreCorto());
        // Avisa a vecinos que registraron su ubicación a menos de 5 km (máximo 50 avisos).
        int avisados = 0;
        for (Usuario v : usuarios.findAll()) {
            if (avisados >= 50) break;
            if (v.getId().equals(u.getId()) || v.getLatitud() == null || v.getLongitud() == null) continue;
            if (dentroRadio(latitud, longitud, v.getLatitud(), v.getLongitud(), 5)) {
                notificaciones.notificar(v, "🚨 " + u.getNombreCorto() + " activó SOS por " + cat + " cerca de ti. Ingresa para coordinar soporte.", AyudaService.enlaceEmergencia(e));
                avisados++;
            }
        }
        return Map.of("ok", true, "id", e.getId(), "mensaje", "Alerta SOS activada con éxito en el mapa vecinal.");
    }

    // ---------- Mapa ----------

    @GetMapping("/mapa")
    public String mapa(HttpSession session, Model model) {
        Usuario u = sesion.actual(session);
        model.addAttribute("latitudUsuario", u == null ? null : u.getLatitud());
        model.addAttribute("longitudUsuario", u == null ? null : u.getLongitud());
        return "mapa";
    }

    @GetMapping("/api/mapa/publicaciones")
    @ResponseBody
    public List<MapaPublicacionDto> publicacionesMapa(@RequestParam(required=false) Double lat,
            @RequestParam(required=false) Double lng, @RequestParam(defaultValue="5") double radioKm) {
        double radio = Math.max(1, Math.min(radioKm, 30));
        List<MapaPublicacionDto> out = new ArrayList<>();
        List<PublicacionServicio> lista = servicios.findByOcultoFalseOrderByCreadoEnDesc();
        reputacion.completar(lista, null);
        for (PublicacionServicio p : lista)
            if (p.getLatitud() != null && p.getLongitud() != null && dentroRadio(lat, lng, p.getLatitud(), p.getLongitud(), radio))
                out.add(new MapaPublicacionDto("servicio", p.getId(), p.getTitulo(), p.getDescripcion(), p.getCategoria(), p.getTipo(),
                    p.getUsuario().getNombreCorto(), p.getLatitud(), p.getLongitud(), p.getContacto(), p.getNivelVecinal(),
                    p.isVerificado(), p.getPromedioCalificacion(), "/servicios/" + p.getId()));
        for (Favor f : favores.findByOcultoFalseAndActivoTrueOrderByCreadoEnDesc())
            if (f.getLatitud() != null && f.getLongitud() != null && dentroRadio(lat, lng, f.getLatitud(), f.getLongitud(), radio))
                out.add(new MapaPublicacionDto("favor", f.getId(), f.getTitulo(), f.getDescripcion(), f.getCategoria(), f.getTipo(),
                    f.getUsuario().getNombreCorto(), f.getLatitud(), f.getLongitud(), null, f.getEstado(), false, null, AyudaService.enlaceFavor(f)));
        for (ReporteEmergencia e : emergencias.findByOcultoFalseAndEstadoNotOrderByCreadoEnDesc(Catalogos.ESTADO_RESUELTA))
            if (e.getLatitud() != null && e.getLongitud() != null && dentroRadio(lat, lng, e.getLatitud(), e.getLongitud(), radio))
                out.add(new MapaPublicacionDto("emergencia", e.getId(), e.getTitulo(), e.getDescripcion(), e.getCategoria(), e.getEstado(),
                    e.getUsuario().getNombreCorto(), e.getLatitud(), e.getLongitud(), null, e.getEstado(), false, null, AyudaService.enlaceEmergencia(e)));
        for (GrupoApoyo g : gruposApoyo.findByActivoTrueAndOcultoFalseOrderByVerificadoDescCreadoEnDesc())
            if (g.getLatitud() != null && g.getLongitud() != null && dentroRadio(lat, lng, g.getLatitud(), g.getLongitud(), radio))
                out.add(new MapaPublicacionDto("recurso", g.getId(), g.getNombre(), g.getDescripcion(), g.getCategoria(), g.getTipo(),
                    g.getUsuario().getNombreCorto(), g.getLatitud(), g.getLongitud(), g.getTelefono(), g.isVerificado() ? "Verificado" : "No verificado",
                    g.isVerificado(), null, "/emergencias/grupos"));
        return out;
    }

    static boolean dentroRadio(Double lat1, Double lon1, double lat2, double lon2, double radioKm) {
        if (lat1 == null || lon1 == null) return true;
        double r = 6371, dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) <= radioKm;
    }
}
