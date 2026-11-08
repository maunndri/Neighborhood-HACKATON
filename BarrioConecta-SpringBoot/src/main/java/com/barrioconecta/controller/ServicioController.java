package com.barrioconecta.controller;

import com.barrioconecta.model.*;
import com.barrioconecta.repository.*;
import com.barrioconecta.service.*;
import com.barrioconecta.util.Catalogos;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** Servicios de proximidad con Visto Bueno Vecinal. */
@Controller
public class ServicioController {
    private final ServicioRepository servicios;
    private final ValoracionServicioRepository valoraciones;
    private final SesionService sesion;
    private final ReputacionService reputacion;
    private final NotificacionService notificaciones;

    public ServicioController(ServicioRepository servicios, ValoracionServicioRepository valoraciones, SesionService sesion,
                              ReputacionService reputacion, NotificacionService notificaciones) {
        this.servicios = servicios; this.valoraciones = valoraciones; this.sesion = sesion;
        this.reputacion = reputacion; this.notificaciones = notificaciones;
    }

    @GetMapping("/servicios")
    public String lista(@RequestParam(required=false) String tipo, @RequestParam(required=false) String categoria,
                        @RequestParam(required=false) String q, @RequestParam(required=false) String orden,
                        Model model, HttpSession session) {
        Usuario u = sesion.actual(session);
        String t = tipo == null ? "" : tipo.trim();
        String c = categoria == null ? "" : categoria.trim();
        String texto = q == null ? "" : q.trim();
        List<PublicacionServicio> lista = new ArrayList<>(servicios.buscar(t, c, texto));
        reputacion.completar(lista, u);
        if ("recomendados".equals(orden)) {
            lista.sort(Comparator.comparingInt((PublicacionServicio s) -> s.getTotalRecomendaciones()).reversed()
                .thenComparing(Comparator.comparingDouble((PublicacionServicio s) -> s.getPromedioCalificacion()).reversed()));
        }
        model.addAttribute("servicios", lista);
        model.addAttribute("tipo", t);
        model.addAttribute("categoria", c);
        model.addAttribute("q", texto);
        model.addAttribute("orden", orden == null ? "" : orden);
        model.addAttribute("usuarioIdActual", u == null ? null : u.getId());
        return "servicios";
    }

    @GetMapping("/servicios/{id}")
    public String detalle(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session);
        PublicacionServicio s = servicios.findById(id).orElse(null);
        if (s == null || (s.isOculto() && !sesion.esAdmin(u))) {
            ra.addFlashAttribute("error", "Esta publicación ya no está disponible.");
            return "redirect:/servicios";
        }
        reputacion.completar(List.of(s), u);
        model.addAttribute("s", s);
        model.addAttribute("opiniones", reputacion.opiniones(s));
        model.addAttribute("reputacionDueno", reputacion.deUsuario(s.getUsuario()));
        model.addAttribute("usuarioIdActual", u == null ? null : u.getId());
        ValoracionServicio mio = u == null ? null : valoraciones.findByServicioIdAndUsuarioId(id, u.getId()).orElse(null);
        model.addAttribute("miComentario", mio == null ? "" : mio.getComentario());
        return "servicio-detalle";
    }

    @GetMapping("/servicios/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Usuario u = sesion.actual(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("telefonoActual", u.getTelefono());
        model.addAttribute("latitudActual", u.getLatitud());
        model.addAttribute("longitudActual", u.getLongitud());
        return "publicar-servicio";
    }

    @PostMapping("/servicios/nuevo")
    public String guardar(@RequestParam String titulo, @RequestParam String tipo, @RequestParam String categoria,
            @RequestParam String descripcion, @RequestParam(required=false) Double precio,
            @RequestParam(required=false) String contacto, @RequestParam(required=false) Double latitud,
            @RequestParam(required=false) Double longitud, @RequestParam(required=false) MultipartFile imagen,
            HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        PublicacionServicio p = new PublicacionServicio();
        p.setUsuario(u); p.setTitulo(titulo.trim());
        p.setTipo("producto".equals(tipo) ? "producto" : "servicio");
        p.setCategoria(categoria.trim()); p.setDescripcion(descripcion.trim()); p.setPrecio(precio);
        p.setContacto(Catalogos.limpiar(contacto, 40) != null ? Catalogos.limpiar(contacto, 40) : u.getTelefono());
        p.setLatitud(latitud != null ? latitud : u.getLatitud());
        p.setLongitud(longitud != null ? longitud : u.getLongitud());
        if (imagen != null && !imagen.isEmpty()) {
            if (imagen.getSize() > 5 * 1024 * 1024 || imagen.getContentType() == null
                    || !Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(imagen.getContentType())) {
                ra.addFlashAttribute("error", "La imagen debe ser válida y no superar los 5 MB.");
                return "redirect:/servicios/nuevo";
            }
            try {
                String original = imagen.getOriginalFilename() == null ? "imagen" : imagen.getOriginalFilename();
                String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).replaceAll("[^.a-zA-Z0-9]", "") : ".jpg";
                String nombreArchivo = UUID.randomUUID() + ext;
                Path carpeta = Paths.get("uploads"); Files.createDirectories(carpeta);
                Files.copy(imagen.getInputStream(), carpeta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
                p.setImagenUrl("/uploads/" + nombreArchivo);
            } catch (IOException ex) {
                ra.addFlashAttribute("error", "No se pudo guardar la imagen. Inténtalo nuevamente.");
                return "redirect:/servicios/nuevo";
            }
        }
        servicios.save(p);
        ra.addFlashAttribute("mensaje", "Tu publicación se guardó. Cuando tus vecinos la recomienden, ganará Visto Bueno Vecinal.");
        return "redirect:/servicios/" + p.getId();
    }

    /** Visto Bueno Vecinal: 1 a 5 estrellas y un comentario opcional. Las de 4 o 5 estrellas cuentan como recomendación. */
    @PostMapping("/servicios/{id}/valorar")
    public String valorar(@PathVariable Long id, @RequestParam Integer puntuacion,
            @RequestParam(required=false) String comentario, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session);
        if (u == null) { ra.addFlashAttribute("error", "Inicia sesión para recomendar a un vecino."); return "redirect:/login"; }
        PublicacionServicio servicio = servicios.findById(id).orElse(null);
        if (servicio == null || servicio.isOculto()) { ra.addFlashAttribute("error", "El servicio ya no está disponible."); return "redirect:/servicios"; }
        String destino = "redirect:/servicios/" + id;
        if (servicio.getUsuario().getId().equals(u.getId())) {
            ra.addFlashAttribute("error", "No puedes calificar tu propia publicación."); return destino;
        }
        if (puntuacion < 1 || puntuacion > 5) { ra.addFlashAttribute("error", "La calificación debe ser de 1 a 5 estrellas."); return destino; }
        ValoracionServicio voto = valoraciones.findByServicioIdAndUsuarioId(id, u.getId()).orElseGet(ValoracionServicio::new);
        boolean nuevo = voto.getId() == null;
        voto.setServicio(servicio); voto.setUsuario(u); voto.setPuntuacion(puntuacion);
        voto.setComentario(Catalogos.limpiar(comentario, 500));
        voto.setCreadoEn(LocalDateTime.now());
        valoraciones.save(voto);
        if (nuevo) notificaciones.notificar(servicio.getUsuario(),
            u.getNombreCorto() + " calificó «" + servicio.getTitulo() + "» con " + puntuacion + (puntuacion == 1 ? " estrella." : " estrellas."),
            "/servicios/" + id);
        ra.addFlashAttribute("mensaje", "¡Gracias! Tu opinión ayuda a que otros vecinos elijan con confianza.");
        return destino;
    }

    @PostMapping("/servicios/{id}/eliminar")
    public String eliminar(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario u = sesion.actual(session); if (u == null) return "redirect:/login";
        PublicacionServicio p = servicios.findById(id).orElse(null);
        if (p == null || !(p.getUsuario().getId().equals(u.getId()) || u.isAdmin())) {
            ra.addFlashAttribute("error", "No puedes eliminar esta publicación.");
        } else {
            valoraciones.deleteAll(valoraciones.findByServicioId(id)); servicios.delete(p);
            ra.addFlashAttribute("mensaje", "La publicación fue eliminada.");
        }
        return "redirect:/servicios";
    }
}
